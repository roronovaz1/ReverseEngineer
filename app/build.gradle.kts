plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.souvik.rea"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.souvik.rea"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/INDEX.LIST", "META-INF/AL2.0", "META-INF/LGPL2.1")
            pickFirsts += setOf("META-INF/MANIFEST.MF")
        }
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("io.github.skylot:jadx-core:1.5.0")
    implementation("io.github.skylot:jadx-dex-input:1.5.0")
    implementation("com.android.tools.smali:smali-baksmali:3.0.8")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
