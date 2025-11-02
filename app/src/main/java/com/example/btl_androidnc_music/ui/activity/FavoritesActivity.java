package com.example.btl_androidnc_music.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.btl_androidnc_music.R;
import com.example.btl_androidnc_music.ui.adapter.TrackAdapter;
import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.model.Track;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView rvTracks;
    private TrackAdapter adapter;
    private List<Track> trackList = new ArrayList<>();
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        rvTracks = findViewById(R.id.rvTracks);
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration() // <-- THÊM DÒNG NÀY
                .build();

        setupRecyclerView();
        loadTracksFromDb();

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(rvTracks);
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            Track track = trackList.get(position);

            // Đánh dấu là không còn yêu thích
            track.isFavorite = false;

            // Cập nhật lại trong DB (chạy nền)
            Executors.newSingleThreadExecutor().execute(() -> {
                db.trackDao().updateTrack(track);
            });

            // Xóa khỏi danh sách trên UI
            trackList.remove(position);
            adapter.notifyItemRemoved(position);
            Toast.makeText(FavoritesActivity.this, "Đã xóa khỏi Yêu thích", Toast.LENGTH_SHORT).show();
        }
    };
    private void setupRecyclerView() {
        adapter = new TrackAdapter(trackList, position -> {
            // *** XỬ LÝ KHI CLICK VÀO BÀI HÁT ***
            // 1. Mở PlayerActivity
            Intent intent = new Intent(FavoritesActivity.this, PlayerActivity.class);

            // 2. Gửi CẢ DANH SÁCH nhạc và VỊ TRÍ bài hát được click
            intent.putExtra("TRACK_LIST", (Serializable) trackList);
            intent.putExtra("TRACK_POSITION", position);
            startActivity(intent);
        }, null, false);
        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(adapter);
    }

    private void loadTracksFromDb() {
        // Lấy dữ liệu từ Room trên luồng nền
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Track> tracks = db.trackDao().getFavoriteTracks();
            runOnUiThread(() -> {
                trackList.clear();
                trackList.addAll(tracks);
                adapter.notifyDataSetChanged();
            });
        });
    }
}