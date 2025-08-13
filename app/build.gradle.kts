plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.20"
}

android {
    namespace = "com.example.cyclink"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cyclink"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        debug {
            val apiKey = project.findProperty("AI_STUDIO_API_KEY")?.toString() ?: ""
            buildConfigField("String", "AI_STUDIO_API_KEY", "\"$apiKey\"")
            buildConfigField("String", "WEB_CLIENT_ID", "\"${project.findProperty("WEB_CLIENT_ID") ?: ""}\"")
        }
        release {
            val apiKey = project.findProperty("AI_STUDIO_API_KEY")?.toString() ?: ""
            buildConfigField("String", "AI_STUDIO_API_KEY", "\"$apiKey\"")
            buildConfigField("String", "WEB_CLIENT_ID", "\"${project.findProperty("WEB_CLIENT_ID") ?: ""}\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.compose.animation:animation:1.5.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    implementation(libs.material)

    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation(platform("androidx.compose:compose-bom:2025.07.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    implementation(libs.androidx.runner)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}