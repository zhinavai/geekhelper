package com.geekhelper.tv;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class ApkInstaller {

    public static void install(Context context, File apk) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    apk);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(apk);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.install_failed, Toast.LENGTH_LONG).show();
        }
    }
}
