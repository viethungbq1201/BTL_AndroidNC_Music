package com.example.btl_androidnc_music.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_androidnc_music.R;
import com.example.btl_androidnc_music.data.model.Track;

import java.io.File;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<Track> trackList;
    private OnTrackClickListener listener;
    private OnTrackOptionsClickListener optionsClickListener;
    private boolean showOptionsButton;

    // Interface để xử lý sự kiện click
    public interface OnTrackClickListener {
        void onTrackClick(int position);
    }
    public interface OnTrackOptionsClickListener {
        void onEditClick(Track track);
        void onDeleteClick(Track track, int position);
    }

    public TrackAdapter(List<Track> trackList, OnTrackClickListener listener) {
        this.trackList = trackList;
        this.listener = listener;
    }
    public TrackAdapter(List<Track> trackList, OnTrackClickListener listener, OnTrackOptionsClickListener optionsClickListener, boolean showOptionsButton) {
        this.trackList = trackList;
        this.listener = listener;
        this.optionsClickListener = optionsClickListener;
        this.showOptionsButton = showOptionsButton;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = trackList.get(position);
        holder.tvTitle.setText(track.title);
        holder.tvArtist.setText(track.artist);
        holder.tvDuration.setText(track.duration);
        if (showOptionsButton) {
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.btnMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(holder.itemView.getContext(), holder.btnMore);
                popup.inflate(R.menu.track_options_menu);
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_edit) {
                        optionsClickListener.onEditClick(track);
                        return true;
                    } else if (itemId == R.id.menu_delete) {
                        optionsClickListener.onDeleteClick(track, holder.getAdapterPosition());
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        } else {
            // Ẩn nút "..." đi
            holder.btnMore.setVisibility(View.GONE);
        }
        // <-- THÊM MỚI: Hiển thị ảnh -->
        if (track.imagePath != null && !track.imagePath.isEmpty()) {
            holder.ivImage.setImageURI(Uri.fromFile(new File(track.imagePath)));
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_music_note);
        }

        // Gán sự kiện click
        holder.itemView.setOnClickListener(v -> {
            listener.onTrackClick(position);
        });

        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(holder.itemView.getContext(), holder.btnMore);
            popup.inflate(R.menu.track_options_menu); // <-- Cần tạo file menu này
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_edit) {
                    optionsClickListener.onEditClick(track);
                    return true;
                } else if (itemId == R.id.menu_delete) {
                    optionsClickListener.onDeleteClick(track, position);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return trackList.size();
    }

    // ViewHolder
    class TrackViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage; // <-- Khai báo biến
        TextView tvTitle, tvArtist, tvDuration; // <-- Khai báo biến
        ImageButton btnMore; // <-- Khai báo biến

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ (tìm) các View từ ID trong file XML
            ivImage = itemView.findViewById(R.id.ivTrackImage);
            tvTitle = itemView.findViewById(R.id.tvTrackTitle);
            tvArtist = itemView.findViewById(R.id.tvTrackArtist);
            tvDuration = itemView.findViewById(R.id.tvTrackDuration);
            btnMore = itemView.findViewById(R.id.btnMoreOptions); // <-- Đây là nút đang bị lỗi
        }
    }
}
