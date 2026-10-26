android {
    namespace = "com.geekhelper.tv"
    compileSdk = 34

    // 🌟 新增簽署配置
    signingConfigs {
        create("release") {
            val keystoreBase64 = System.getenv("SIGNING_KEY_STORE_BASE64")
            if (!keystoreBase64.isNullOrEmpty()) {
                val jksFile = File.createTempFile("temp_store", ".jks")
                jksFile.writeBytes(java.util.Base64.getDecoder().decode(keystoreBase64))
                storeFile = jksFile
                storePassword = System.getenv("KEY_STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                // 本地若沒有設定環境變數，可以暫時指向 debug 簽署或印出提示，避免本地 Build 報錯
                // storeFile = file("debug.keystore") 
            }
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
