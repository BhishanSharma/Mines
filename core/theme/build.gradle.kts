plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.genoma.mines.core.theme"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
}

dependencies {
    // Exposed as `api`, not `implementation`: every feature module depends on
    // core:theme, and every feature module's Compose screens need these same
    // baseline artifacts (the Compose BOM to keep versions aligned, plus the
    // extended icon set and @Preview support that almost every screen uses).
    // Declaring them once here avoids each feature module having to
    // separately remember to add them.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material.icons.extended)
}
