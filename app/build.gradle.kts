plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ir.mehdisekoba.sample"
    compileSdk = 37


    defaultConfig {
        applicationId = "ir.mehdisekoba.sample"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.compose.ui)
    implementation(libs.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(project(":metaball"))
    implementation(libs.media3.exoplayer)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}