plugins {
    id("com.android.test")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.irah.galleria.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.junit)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
}
