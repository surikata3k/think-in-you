plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.thinkinyou.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.thinkinyou.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Firma fija: permite instalar actualizaciones encima sin perder el progreso
    signingConfigs {
        create("fija") {
            storeFile = file("release.jks")
            storePassword = "thinkinyou"
            keyAlias = "thinkinyou"
            keyPassword = "thinkinyou"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fija")
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

// Sin dependencias externas: solo el framework de Android.
