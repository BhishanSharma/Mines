plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.genoma.mines.session"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(platform(libs.firebase.bom))
    api(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
}
