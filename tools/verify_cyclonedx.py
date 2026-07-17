#!/usr/bin/env python3
"""Validate the generated CycloneDX JSON and XML SBOM pair."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def child_text(parent: ET.Element | None, name: str) -> str | None:
    if parent is None:
        return None
    for child in parent:
        if local_name(child.tag) == name:
            return child.text
    return None


def first_child(parent: ET.Element | None, name: str) -> ET.Element | None:
    if parent is None:
        return None
    for child in parent:
        if local_name(child.tag) == name:
            return child
    return None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", dest="json_path", type=Path, required=True)
    parser.add_argument("--xml", dest="xml_path", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    violations: list[str] = []

    try:
        json_doc = json.loads(args.json_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        violations.append(f"JSON SBOM cannot be parsed: {exc}")
        json_doc = {}

    try:
        xml_root = ET.parse(args.xml_path).getroot()
    except (OSError, ET.ParseError) as exc:
        violations.append(f"XML SBOM cannot be parsed: {exc}")
        xml_root = ET.Element("invalid")

    if json_doc.get("bomFormat") != "CycloneDX":
        violations.append("JSON bomFormat must be CycloneDX")
    if json_doc.get("specVersion") != "1.6":
        violations.append("JSON specVersion must be 1.6")
    if "serialNumber" in json_doc:
        violations.append("JSON serialNumber must be omitted for the reproducible baseline")

    metadata = json_doc.get("metadata") or {}
    json_component = metadata.get("component") or {}
    json_name = json_component.get("name")
    json_version = json_component.get("version")
    if not json_name:
        violations.append("JSON metadata.component.name is missing")
    if not json_version:
        violations.append("JSON metadata.component.version is missing")

    components = json_doc.get("components") or []
    if not components:
        violations.append("JSON SBOM contains no dependency components")

    if local_name(xml_root.tag) != "bom":
        violations.append("XML root element must be bom")
    if xml_root.get("serialNumber") is not None:
        violations.append("XML serialNumber must be omitted for the reproducible baseline")

    xml_metadata = first_child(xml_root, "metadata")
    xml_component = first_child(xml_metadata, "component")
    xml_name = child_text(xml_component, "name")
    xml_version = child_text(xml_component, "version")
    if not xml_name:
        violations.append("XML metadata/component/name is missing")
    if not xml_version:
        violations.append("XML metadata/component/version is missing")

    if json_name and xml_name and json_name != xml_name:
        violations.append(
            f"JSON/XML component names disagree: {json_name!r} != {xml_name!r}"
        )
    if json_version and xml_version and json_version != xml_version:
        violations.append(
            f"JSON/XML component versions disagree: {json_version!r} != {xml_version!r}"
        )

    report = {
        "schema": "range-sense.sbom-validation.v1",
        "status": "pass" if not violations else "fail",
        "json_sha256": sha256(args.json_path) if args.json_path.exists() else None,
        "xml_sha256": sha256(args.xml_path) if args.xml_path.exists() else None,
        "component_name": json_name,
        "component_version": json_version,
        "dependency_component_count": len(components),
        "violations": violations,
        "limitations": [
            "Schema markers and structural invariants are checked",
            "This validator does not query vulnerability databases",
            "A valid SBOM does not prove dependency safety",
        ],
    }

    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )

    if violations:
        for violation in violations:
            print(f"SBOM FAILURE: {violation}", file=sys.stderr)
        return 1

    print(f"CycloneDX SBOM validation passed; report: {args.report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
