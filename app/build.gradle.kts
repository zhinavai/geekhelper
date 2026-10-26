android {
    namespace = "com.geekhelper.tv"
    compileSdk = 34

    // 🌟 新增簽署配置
    signingConfigs {
        create("release") {
            // 從系統環境變數讀取密鑰（GitHub Actions 會自動傳入）
            val keystoreBase64 = System.getenv("SIGNING_KEY_STORE_BASE64")
            if (!keystoreBase64.isNullOrEmpty()) {
                val jksFile = File.createTempFile("temp_store", ".jks")
                jksFile.writeBytes(java.util.Base64.getDecoder().decode(keystoreBase64))
                storeFile = jksFile
            }
            storePassword = System.getenv("KEY_STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.geekhelper.tv"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 🌟 綁定 release 簽署
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
