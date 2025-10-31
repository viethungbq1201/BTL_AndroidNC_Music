package com.example.btl_androidnc_music;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Adapter này dùng cho các RecyclerView NẰM NGANG
public class CategorySongAdapter extends RecyclerView.Adapter<CategorySongAdapter.SongViewHolder> {
    private List<Track> trackList;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(ArrayList<Track> trackList, int position);
    }

    // <-- SỬA LẠI: Thêm listener vào constructor -->
    public CategorySongAdapter(List<Track> trackList, OnSongClickListener listener) {
        this.trackList = trackList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Track track = trackList.get(position);
        holder.tvSongTitle.setText(track.title);

        if (track.imagePath != null && !track.imagePath.isEmpty()) {
            // Nếu có, hiển thị ảnh đó
            File imageFile = new File(track.imagePath);
            holder.ivCoverArt.setImageURI(Uri.fromFile(imageFile));
        } else {
            // Nếu không có, hiển thị ảnh mặc định
            holder.ivCoverArt.setImageResource(R.drawable.ic_music_note);
        }

        // Xử lý click (ví dụ: mở PlayerActivity)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Gửi cả danh sách và vị trí bài hát được click
                listener.onSongClick(new ArrayList<>(trackList), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trackList.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCoverArt;
        TextView tvSongTitle;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ (tìm) các View từ ID trong file XML
            ivCoverArt = itemView.findViewById(R.id.ivCoverArt);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
        }
    }
}