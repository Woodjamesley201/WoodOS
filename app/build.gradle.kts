plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.woodos"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.woodos"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    // Release signing is driven by environment variables so that
    // credentials never live in the repository. CI (GitHub Actions)
    // injects them from GitHub Secrets.
    signingConfigs {
        create("release") {
            val ksPath = System.getenv("WOODOS_KEYSTORE")
            if (!ksPath.isNullOrBlank() && rootProject.file(ksPath).exists()) {
                storeFile = rootProject.file(ksPath)
                storePassword = System.getenv("WOODOS_STORE_PASSWORD")
                keyAlias = System.getenv("WOODOS_KEY_ALIAS")
                keyPassword = System.getenv("WOODOS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (System.getenv("WOODOS_KEYSTORE")?.isNotBlank() == true) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    applicationVariants.all {
        outputs.all {
            (this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)
                ?.outputFileName = "WoodOS-${name}.apk"
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
    // Intentionally no runtime dependencies: WoodOS is built entirely on
    // the Android framework to minimize APK size, RAM use and build time.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303") // real org.json for local unit tests
}
