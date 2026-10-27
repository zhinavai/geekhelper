plugins {
    id("com.android.application")
}

android {
    namespace = "com.geekhelper.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.geekhelper.tv"
        minSdk = 16
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        resourceConfigurations.addAll(listOf("en", "zh"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.google.zxing:core:3.4.1")
}
