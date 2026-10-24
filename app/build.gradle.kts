plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ivangames.fogofwar"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ivangames.fogofwar"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootDir}/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "fogofwar123"
            keyAlias = System.getenv("KEY_ALIAS") ?: "fogofwar"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "fogofwar123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
