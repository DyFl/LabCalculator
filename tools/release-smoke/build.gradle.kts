plugins { alias(libs.plugins.android.application) }

android {
    namespace = "com.dyfl.labcalculator.binarycheck"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.dyfl.labcalculator.binarycheck"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation("androidx.test:runner:1.7.0")
}
