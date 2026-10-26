package com.geekhelper.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : Activity() {

    private var webServer: WebServer? = null
    private lateinit var tvAppTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvServerUrl: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAppTitle = findViewById(R.id.tvAppTitle)
        tvStatus = findViewById(R.id.tvStatus)
        tvServerUrl = findViewById(R.id.tvServerUrl)

        // 1. 根據系統環境動態設定名稱（支援中英文自動切換）
        val appName = if (LocaleHelper.isChineseEnvironment(this)) "极客助手 (TV)" else "GeekHelper TV"
        tvAppTitle.text = appName

        // 2. 啟動內嵌 Web 伺服器 (監聽 8080 埠號)
        try {
            webServer = WebServer(8080, cacheDir) { apkFile ->
                // 當網頁端上傳完成 APK 時，回調此處自動調起安裝
                runOnUiThread {
                    installApk(apkFile)
                }
            }
            webServer?.start()
            
            val ip = getLocalIpAddress()
            val url = "http://$ip:8080"
            tvStatus.text = if (LocaleHelper.isChineseEnvironment(this)) 
                "请用手机或电脑连接同一 Wi-Fi，在浏览器中输入：" 
                else "Connect phone/PC to same Wi-Fi, open in browser:"
            tvServerUrl.text = url

        } catch (e: Exception) {
            tvStatus.text = "伺服器啟動失敗: ${e.message}"
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress ?: continue
                        // 過濾出 IPv4 地址
                        if (sAddr.indexOf(':') < 0) {
                            return sAddr
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        return "127.0.0.1"
    }

    private fun installApk(apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", apkFile),
                "application/vnd.android.package-archive"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer?.stop()
    }
}
