package com.geekhelper.tv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteOrder
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var webServer: WebServer? = null
    private lateinit var tvStatus: TextView
    private var isChineseEnv: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-detect Chinese vs English system locale
        isChineseEnv = isChineseLocale()

        // Clean Dark TV UI
        tvStatus = TextView(this).apply {
            textSize = 22f
            setPadding(60, 60, 60, 60)
            setTextColor(0xFFF8FAFC.toInt())
            text = getString(R.string.server_starting)
        }
        setContentView(tvStatus)

        checkPermissions()
        startServer()
    }

    private fun isChineseLocale(): Boolean {
        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
        return currentLocale.language.equals("zh", ignoreCase = true)
    }

    private fun startServer() {
        val ip = getWifiIpAddress()
        val port = 8080
        val saveDir = getExternalFilesDir(null) ?: filesDir

        webServer = WebServer(port, saveDir, isChineseEnv) { uploadedFile ->
            runOnUiThread {
                Toast.makeText(
                    this,
                    getString(R.string.file_received, uploadedFile.name),
                    Toast.LENGTH_SHORT
                ).show()
                installApkCompat(uploadedFile)
            }
        }

        try {
            webServer?.start()
            val title = getString(R.string.server_ready_title)
            val desc = getString(R.string.server_instruction)
            
            tvStatus.text = "$title\n\n$desc\n\nhttp://$ip:$port"
        } catch (e: Exception) {
            tvStatus.text = "Server Error: ${e.message}"
        }
    }

    private fun installApkCompat(apkFile: File) {
        if (!apkFile.exists()) return

        // 1. Android 8.0+ Unknown App Installation Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                Toast.makeText(this, getString(R.string.permission_install_required), Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                return
            }
        }

        // 2. Build Intent with FileProvider support
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri: Uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "$packageName.fileprovider",
                    apkFile
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(apkUri, "application/vnd.android.package-archive")
            } else {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }
        }
        startActivity(intent)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
        }
    }

    private fun getWifiIpAddress(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var ipAddress = wifiManager.connectionInfo.ipAddress
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress)
        }
        val ipByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()
        return try {
            InetAddress.getByAddress(ipByteArray).hostAddress ?: "127.0.0.1"
        } catch (ex: Exception) {
            "127.0.0.1"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer?.stop()
    }
}