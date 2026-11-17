plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dyfl.labcalculator"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.dyfl.labcalculator"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.2-preview.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
        create("performance") {
            initWith(getByName("release"))
            applicationIdSuffix = ".performance"
            versionNameSuffix = "-performance"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            optimization {
                enable = true
            }
            matchingFallbacks += listOf("release")
        }
        // Same app ID: install only over an app signed with this local debug certificate.
        create("optimizedLocal") {
            initWith(getByName("release"))
            versionNameSuffix = "-optimized-local"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            optimization {
                enable = true
            }
            matchingFallbacks += listOf("release")
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
