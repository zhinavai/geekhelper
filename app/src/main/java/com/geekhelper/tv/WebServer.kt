package com.geekhelper.tv

import fi.iki.elonen.NanoHTTPD
import java.io.File

/**
 * Embedded NanoHTTPD server delivering a web upload portal for TV APK sideloading.
 */
class WebServer(
    port: Int,
    private val saveDir: File,
    private val isChinese: Boolean,
    private val onFileUploaded: (File) -> Unit
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return if (Method.POST == session.method) {
            val files = HashMap<String, String>()
            try {
                session.parseBody(files)
                val tempFilePath = files["file"] ?: return newFixedLengthResponse("Upload Failed")
                val filename = session.parms["file"] ?: "app.apk"

                val destFile = File(saveDir, filename)
                File(tempFilePath).copyTo(destFile, overwrite = true)

                // Trigger callback on main thread
                onFileUploaded(destFile)

                val successMsg = if (isChinese) {
                    "<h3 style='color: #10b981;'>✅ APK 上传成功！电视端正在调起安装...</h3>"
                } else {
                    "<h3 style='color: #10b981;'>✅ APK Uploaded Successfully! Triggering installer on TV...</h3>"
                }

                newFixedLengthResponse(Response.Status.OK, "text/html", successMsg)
            } catch (e: Exception) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: ${e.message}")
            }
        } else {
            val title = if (isChinese) "🚀 极客助手 (GeekHelper) 无线闪传" else "🚀 GeekHelper TV Wireless Upload"
            val label = if (isChinese) "选择或拖拽 APK 文件" else "Select or Drag & Drop APK File"

            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>$title</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; text-align: center; padding: 40px 20px; background: #0f172a; color: #f8fafc; }
                        .card { background: #1e293b; border: 1px solid #334155; padding: 40px 24px; border-radius: 16px; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.5); max-width: 420px; margin: auto; }
                        input[type="file"] { display: none; }
                        .btn { background: linear-gradient(135deg, #06b6d4, #2563eb); color: white; padding: 14px 28px; border-radius: 10px; cursor: pointer; display: inline-block; font-size: 16px; font-weight: 600; margin-top: 20px; transition: transform 0.2s; }
                        .btn:hover { transform: scale(1.02); }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>$title</h2>
                        <form method="post" enctype="multipart/form-data">
                            <label class="btn" for="fileInput">$label</label>
                            <input id="fileInput" type="file" name="file" accept=".apk" onchange="form.submit()"/>
                        </form>
                    </div>
                </body>
                </html>
            """.trimIndent()
            newFixedLengthResponse(Response.Status.OK, "text/html", html)
        }
    }
}