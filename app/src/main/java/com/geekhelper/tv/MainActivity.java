package com.geekhelper.tv;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView tvAddress;
    private ImageView ivQr;
    private Button btnClear;
    private final List<File> apkFiles = new ArrayList<>();
    private ApkListAdapter adapter;

    private int lastCount = -1;
    private long lastModified = -1;

    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            reloadApks();
            handler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        tvAddress = findViewById(R.id.tv_address);
        ivQr = findViewById(R.id.iv_qr);
        btnClear = findViewById(R.id.btn_clear);
        RecyclerView recyclerView = findViewById(R.id.recycler);

        adapter = new ApkListAdapter(apkFiles, file -> {
            if (ensureInstallPermission()) {
                ApkInstaller.install(MainActivity.this, file);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setFocusable(true);
        recyclerView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        btnClear.setOnClickListener(v -> clearReceivedApks());

        startUploadService();
    }

    private void clearReceivedApks() {
        File dir = new File(getExternalFilesDir(null), "received");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
        reloadApks();
        Toast.makeText(this, R.string.cleared_success, Toast.LENGTH_SHORT).show();
    }

    private boolean ensureInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                Toast.makeText(this, R.string.need_install_permission, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return false;
            }
        }
        return true;
    }

    private void startUploadService() {
        Intent intent = new Intent(this, UploadService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent);
        } else {
            startService(intent);
        }
        handler.post(refreshTask);
        showAddress();
    }

    private void showAddress() {
        String ip = getLocalIp();
        if (ip == null) {
            tvAddress.setText(R.string.no_network);
            ivQr.setImageBitmap(null);
            return;
        }
        String url = "http://" + ip + ":" + UploadServer.PORT;
        tvAddress.setText(url);
        ivQr.setImageBitmap(generateQr(url, 220));
    }

    private void reloadApks() {
        File dir = new File(getExternalFilesDir(null), "received");
        File[] list = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".apk"));
        List<File> sorted = new ArrayList<>();
        if (list != null) {
            sorted.addAll(Arrays.asList(list));
            Collections.sort(sorted, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        }
        int count = sorted.size();
        long mod = count > 0 ? sorted.get(0).lastModified() : 0;
        if (count == lastCount && mod == lastModified) {
            return;
        }
        lastCount = count;
        lastModified = mod;
        apkFiles.clear();
        apkFiles.addAll(sorted);
        adapter.notifyDataSetChanged();
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                if (nif.isLoopback() || !nif.isUp()) continue;
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            android.net.wifi.WifiManager wm =
                    (android.net.wifi.WifiManager) getApplicationContext()
                            .getSystemService(WIFI_SERVICE);
            if (wm != null) {
                int ip = wm.getConnectionInfo().getIpAddress();
                if (ip != 0) return Formatter.formatIpAddress(ip);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Bitmap generateQr(String content, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            int w = matrix.getWidth(), h = matrix.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    pixels[y * w + x] = matrix.get(x, y) ? 0xFF0F172A : 0xFFFFFFFF;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bmp.setPixels(pixels, 0, w, 0, 0, w, h);
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(refreshTask);
        super.onDestroy();
    }
}
