plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cloudplay.runtimebridge"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        ndk {
            abiFilters += listOf("arm64-v8a")
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
    // Match Winlator's archive format: Zstandard-compressed TAR (.tzst).
    implementation("com.github.luben:zstd-jni:1.5.2-3")
    implementation("org.apache.commons:commons-compress:1.20")
}
