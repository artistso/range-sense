plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.cyclonedx.bom)
}

group = "com.soquarky"
version = "0.2.0"

tasks.cyclonedxBom {
    projectType = "application"
    componentName = "range-sense"
    componentVersion = project.version.toString()
    includeBomSerialNumber = false
    includeLicenseText = false
    includeBuildSystem = true
}
