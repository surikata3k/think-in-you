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
        versionCode = 2
        versionName = "1.1"
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

// Las frases viven en docs/frases.txt (lo publica GitHub Pages y lo lee la app).
// Se copia una versión dentro del APK como respaldo sin conexión.
val copyFrases = tasks.register<Copy>("copyFrases") {
    from(rootProject.file("docs/frases.txt"))
    into(layout.buildDirectory.dir("generated/frases"))
}
android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/frases"))
tasks.named("preBuild") { dependsOn(copyFrases) }
