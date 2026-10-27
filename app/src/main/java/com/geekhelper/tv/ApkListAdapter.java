package com.geekhelper.tv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ApkListAdapter extends RecyclerView.Adapter<ApkListAdapter.VH> {

    public interface OnItemClick {
        void onClick(File file);
    }

    private final List<File> files;
    private final OnItemClick listener;

    public ApkListAdapter(List<File> files, OnItemClick listener) {
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_apk, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final File f = files.get(position);
        holder.name.setText(f.getName());
        holder.size.setText(formatSize(f.length()));
        holder.itemView.setOnClickListener(v -> listener.onClick(f));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    private static String formatSize(long len) {
        if (len < 1024) return len + " B";
        if (len < 1024 * 1024) return String.format(Locale.US, "%.1f KB", len / 1024.0);
        return String.format(Locale.US, "%.1f MB", len / 1024.0 / 1024.0);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        TextView size;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.apk_name);
            size = v.findViewById(R.id.apk_size);
        }
    }
}
