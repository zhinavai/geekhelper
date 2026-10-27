package com.geekhelper.tv;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class UploadServer extends NanoHTTPD {

    private static final String TAG = "UploadServer";
    public static final int PORT = 8899;
    private final File uploadDir;

    public UploadServer(Context context) {
        super(PORT);
        File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();
        uploadDir = new File(base, "received");
        if (!uploadDir.exists()) uploadDir.mkdirs();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if ("/".equals(uri) || "/index.html".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", UPLOAD_HTML);
        }
        if ("/upload".equals(uri) && Method.POST.equals(session.getMethod())) {
            try {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);

                String tmpPath = null;
                for (Map.Entry<String, String> e : files.entrySet()) {
                    if (!"postData".equals(e.getKey())) {
                        tmpPath = e.getValue();
                        break;
                    }
                }
                if (tmpPath == null) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                            "text/plain; charset=utf-8", "NO_FILE");
                }
                File tmp = new File(tmpPath);
                String name = "apk_" + System.currentTimeMillis() + ".apk";
                File dest = new File(uploadDir, name);
                copyFile(tmp, dest);
                tmp.delete();
                return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8",
                        "OK:" + dest.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "upload failed", e);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                        "text/plain; charset=utf-8", "FAIL:" + e.getMessage());
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain; charset=utf-8", "404");
    }

    private void copyFile(File src, File dst) throws Exception {
        try (InputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private static final String UPLOAD_HTML =
            "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>GeekHelper - PushTV Style</title><style>" +
            "body{font-family:system-ui,-apple-system,sans-serif;background:#0F172A;color:#E2E8F0;" +
            "display:flex;flex-direction:column;align-items:center;padding:40px 20px;margin:0}" +
            "h1{font-size:24px;color:#38BDF8;margin-bottom:8px}p{color:#94A3B8;font-size:14px}" +
            "form{margin-top:30px;background:#1E293B;padding:30px;border-radius:12px;border:1px solid #334155;display:flex;flex-direction:column;align-items:center}" +
            "input[type=file]{color:#94A3B8;margin-bottom:20px}" +
            "button{padding:12px 36px;font-size:16px;font-weight:bold;border:none;border-radius:8px;background:#38BDF8;color:#0F172A;cursor:pointer}" +
            "button:hover{background:#0EA5E9}</style></head><body>" +
            "<h1>PushTV · 極速文件閃傳</h1><p>選取 APK 檔案並上傳至電視端</p>" +
            "<form method='post' action='/upload' enctype='multipart/form-data'>" +
            "<input type='file' name='file' accept='.apk' required>" +
            "<button type='submit'>一鍵上傳並安裝</button></form>" +
            "<p style='margin-top:30px'>上傳成功後，請在電視端直接點擊安裝</p>" +
            "</body></html>";
}
