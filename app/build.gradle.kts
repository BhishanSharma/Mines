plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.genoma.mines"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.genoma.mines"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Core (shared, no feature ever lives here) ---
    implementation(project(":core:theme"))
    implementation(project(":core:ui"))
    implementation(project(":core:session"))

    // --- Features (the app module only wires these together) ---
    implementation(project(":feature:auth"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:store"))
    implementation(project(":feature:wallet"))
    implementation(project(":feature:tournament"))
    implementation(project(":feature:moregames"))
    implementation(project(":feature:userfeedback"))
    implementation(project(":feature:game"))
    implementation(project(":feature:achievements"))
    implementation(project(":feature:celebration"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:home"))

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // --- Firebase (app needs FirebaseAuth directly for account deletion flow) ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
