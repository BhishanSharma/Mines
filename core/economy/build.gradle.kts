plugins {
    alias(libs.plugins.android.library)
}

// Pure, Android-free economy rules shared by the guest (DataStore) and
// signed-in (Firestore) code paths. Everything here must stay
// unit-testable on the plain JVM.
android {
    namespace = "com.genoma.mines.core.economy"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    testImplementation(libs.junit)
    // Real org.json on the JVM; android.jar only ships stubs for unit tests.
    testImplementation(libs.org.json)
}
