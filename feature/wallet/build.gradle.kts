plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.genoma.mines.wallet"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:session"))

    api(libs.androidx.datastore.preferences)
    api(platform(libs.firebase.bom))
    api(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
}
