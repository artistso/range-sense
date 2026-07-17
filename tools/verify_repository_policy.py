#!/usr/bin/env python3
"""Fail-closed repository and Android manifest policy checks.

This is a deterministic, dependency-free guardrail for the current public,
offline Range Sense application. It is not a substitute for independent
security review, historical secret scanning, or platform certification.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, asdict
from pathlib import Path

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
APP_NAMESPACE = "com.soquarky.rangesense"
ALLOWED_EXPORTED_COMPONENTS = {f"{APP_NAMESPACE}.MainActivity"}
ALLOWED_PERMISSIONS: set[str] = set()

TEXT_SUFFIXES = {
    ".gradle",
    ".java",
    ".json",
    ".kt",
    ".kts",
    ".md",
    ".properties",
    ".toml",
    ".xml",
    ".yaml",
    ".yml",
}
SKIP_DIRECTORY_NAMES = {".git", ".gradle", ".idea", "build"}

SECRET_PATTERNS = {
    "github_legacy_token": re.compile(r"\bgh[pousr]_[A-Za-z0-9]{20,255}\b"),
    "github_fine_grained_token": re.compile(r"\bgithub_pat_[A-Za-z0-9_]{20,255}\b"),
    "aws_access_key": re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    "google_api_key": re.compile(r"\bAIza[0-9A-Za-z_-]{35}\b"),
    "slack_token": re.compile(r"\bxox[baprs]-[0-9A-Za-z-]{10,}\b"),
    "private_key": re.compile(
        r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"
    ),
}

FORBIDDEN_APP_PATTERNS = {
    "android_webview": re.compile(r"\bandroid\.webkit\b|\bWebView\s*\(|<\s*WebView\b"),
    "network_client": re.compile(
        r"\b(?:okhttp3|retrofit2|io\.ktor\.client|java\.net\.)\b"
    ),
}


@dataclass(frozen=True)
class Violation:
    check: str
    path: str
    detail: str
    line: int | None = None


def android_attr(element: ET.Element, name: str) -> str | None:
    return element.get(f"{ANDROID_NS}{name}")


def resolve_component_name(raw_name: str) -> str:
    if raw_name.startswith("."):
        return f"{APP_NAMESPACE}{raw_name}"
    if "." not in raw_name:
        return f"{APP_NAMESPACE}.{raw_name}"
    return raw_name


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def iter_text_files(repo_root: Path):
    verifier = Path(__file__).resolve()
    for path in sorted(repo_root.rglob("*")):
        if not path.is_file():
            continue
        if path.resolve() == verifier:
            continue
        if any(part in SKIP_DIRECTORY_NAMES for part in path.parts):
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        yield path


def verify_manifest(manifest_path: Path, violations: list[Violation]) -> dict[str, object]:
    try:
        root = ET.parse(manifest_path).getroot()
    except (ET.ParseError, OSError) as exc:
        violations.append(
            Violation("manifest_parse", str(manifest_path), f"Cannot parse manifest: {exc}")
        )
        return {}

    if android_attr(root, "sharedUserId") is not None:
        violations.append(
            Violation(
                "manifest_shared_user",
                str(manifest_path),
                "android:sharedUserId is prohibited",
            )
        )

    permissions = sorted(
        {
            android_attr(node, "name")
            for tag in ("uses-permission", "uses-permission-sdk-23")
            for node in root.findall(tag)
            if android_attr(node, "name")
        }
    )
    unexpected_permissions = sorted(set(permissions) - ALLOWED_PERMISSIONS)
    for permission in unexpected_permissions:
        violations.append(
            Violation(
                "manifest_permission",
                str(manifest_path),
                f"Permission is not allowlisted: {permission}",
            )
        )

    if root.find("queries") is not None:
        violations.append(
            Violation(
                "manifest_package_visibility",
                str(manifest_path),
                "Package-visibility queries are prohibited for the offline baseline",
            )
        )

    application = root.find("application")
    if application is None:
        violations.append(
            Violation("manifest_application", str(manifest_path), "Missing <application>")
        )
        return {"permissions": permissions}

    required_attributes = {
        "allowBackup": "false",
        "usesCleartextTraffic": "false",
    }
    for attribute, expected in required_attributes.items():
        actual = android_attr(application, attribute)
        if actual != expected:
            violations.append(
                Violation(
                    "manifest_application_policy",
                    str(manifest_path),
                    f"android:{attribute} must be {expected!r}; found {actual!r}",
                )
            )

    if android_attr(application, "debuggable") == "true":
        violations.append(
            Violation(
                "manifest_debuggable",
                str(manifest_path),
                "Main manifest must not force android:debuggable=true",
            )
        )
    if android_attr(application, "testOnly") == "true":
        violations.append(
            Violation(
                "manifest_test_only",
                str(manifest_path),
                "Main manifest must not force android:testOnly=true",
            )
        )

    exported_components: list[str] = []
    component_tags = ("activity", "activity-alias", "service", "receiver", "provider")
    for tag in component_tags:
        for component in application.findall(tag):
            raw_name = android_attr(component, "name")
            name = resolve_component_name(raw_name) if raw_name else f"<unnamed-{tag}>"
            exported = android_attr(component, "exported")
            has_intent_filter = component.find("intent-filter") is not None
            if has_intent_filter and exported is None:
                violations.append(
                    Violation(
                        "manifest_exported_explicit",
                        str(manifest_path),
                        f"{tag} {name} has an intent filter without explicit android:exported",
                    )
                )
            if exported == "true":
                exported_components.append(name)
                if name not in ALLOWED_EXPORTED_COMPONENTS:
                    violations.append(
                        Violation(
                            "manifest_exported_allowlist",
                            str(manifest_path),
                            f"Exported component is not allowlisted: {name}",
                        )
                    )

    missing_required_export = sorted(
        ALLOWED_EXPORTED_COMPONENTS - set(exported_components)
    )
    for component in missing_required_export:
        violations.append(
            Violation(
                "manifest_launcher_export",
                str(manifest_path),
                f"Required launcher component is not explicitly exported: {component}",
            )
        )

    return {
        "permissions": permissions,
        "exported_components": sorted(exported_components),
        "allow_backup": android_attr(application, "allowBackup"),
        "uses_cleartext_traffic": android_attr(application, "usesCleartextTraffic"),
    }


def verify_sources(repo_root: Path, violations: list[Violation]) -> dict[str, int]:
    scanned_files = 0
    secret_findings = 0
    app_policy_findings = 0
    app_root = (repo_root / "app" / "src" / "main").resolve()

    for path in iter_text_files(repo_root):
        scanned_files += 1
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue

        relative = path.relative_to(repo_root).as_posix()
        for name, pattern in SECRET_PATTERNS.items():
            for match in pattern.finditer(text):
                secret_findings += 1
                violations.append(
                    Violation(
                        "secret_scan",
                        relative,
                        f"Potential {name} material",
                        line_number(text, match.start()),
                    )
                )

        try:
            is_app_source = path.resolve().is_relative_to(app_root)
        except AttributeError:  # pragma: no cover - Python <3.9 fallback
            is_app_source = str(path.resolve()).startswith(str(app_root))

        if is_app_source:
            for name, pattern in FORBIDDEN_APP_PATTERNS.items():
                for match in pattern.finditer(text):
                    app_policy_findings += 1
                    violations.append(
                        Violation(
                            "offline_app_policy",
                            relative,
                            f"Forbidden {name} reference",
                            line_number(text, match.start()),
                        )
                    )

    return {
        "scanned_files": scanned_files,
        "secret_findings": secret_findings,
        "offline_app_findings": app_policy_findings,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    parser.add_argument(
        "--manifest",
        type=Path,
        default=Path("app/src/main/AndroidManifest.xml"),
    )
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    repo_root = args.repo_root.resolve()
    manifest_path = args.manifest
    if not manifest_path.is_absolute():
        manifest_path = repo_root / manifest_path

    violations: list[Violation] = []
    manifest_summary = verify_manifest(manifest_path, violations)
    source_summary = verify_sources(repo_root, violations)

    report = {
        "schema": "range-sense.repository-policy.v1",
        "status": "pass" if not violations else "fail",
        "manifest": manifest_summary,
        "source_scan": source_summary,
        "violations": [asdict(item) for item in violations],
        "limitations": [
            "Current-worktree scan only; Git history is not inspected",
            "Pattern checks do not replace independent security review",
            "Source-manifest checks do not replace installed-artifact inspection",
        ],
    }

    report_path = args.report
    if not report_path.is_absolute():
        report_path = repo_root / report_path
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    if violations:
        for violation in violations:
            location = violation.path
            if violation.line is not None:
                location = f"{location}:{violation.line}"
            print(f"POLICY FAILURE [{violation.check}] {location}: {violation.detail}", file=sys.stderr)
        return 1

    print(f"Repository policy checks passed; report: {report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
