package com.example.btl_androidnc_music.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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

public class UploadedMusicActivity extends AppCompatActivity implements TrackAdapter.OnTrackOptionsClickListener {

    private RecyclerView rvTracks;
    private TrackAdapter adapter;
    private List<Track> trackList = new ArrayList<>();
    private AppDatabase db;
    private ActivityResultLauncher<Intent> editTrackLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploaded_music);

        rvTracks = findViewById(R.id.rvTracks);
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration() // <-- THÊM DÒNG NÀY
                .build();

        setupRecyclerView();
        loadTracksFromDb();

        editTrackLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Kiểm tra xem có phải kết quả OK từ UploadActivity không
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Nếu OK, tải lại toàn bộ danh sách
                        loadTracksFromDb();
                    }
                }
        );
    }

    private void setupRecyclerView() {
        adapter = new TrackAdapter(trackList, position -> {
            // *** XỬ LÝ KHI CLICK VÀO BÀI HÁT ***
            // 1. Mở PlayerActivity
            Intent intent = new Intent(UploadedMusicActivity.this, PlayerActivity.class);

            // 2. Gửi CẢ DANH SÁCH nhạc và VỊ TRÍ bài hát được click
            intent.putExtra("TRACK_LIST", (Serializable) trackList);
            intent.putExtra("TRACK_POSITION", position);
            startActivity(intent);
        }, this, true);
        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(adapter);
    }

    // --- THÊM MỚI 2 HÀM NÀY ---
    @Override
    public void onEditClick(Track track) {
        // Mở UploadActivity ở chế độ Sửa
        Intent intent = new Intent(this, UploadActivity.class);
        intent.putExtra(UploadActivity.EXTRA_TRACK_TO_EDIT, track);

        // <-- SỬA LẠI: Dùng launcher để mở Activity -->
        editTrackLauncher.launch(intent);
    }

    @Override
    public void onDeleteClick(Track track, int position) {
        // Hiển thị dialog xác nhận
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa bài hát '" + track.title + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Xóa khỏi DB trên luồng nền
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.trackDao().deleteTrack(track);
                        // Cập nhật lại UI trên luồng chính
                        runOnUiThread(() -> {
                            trackList.remove(position);
                            adapter.notifyItemRemoved(position);
                            Toast.makeText(UploadedMusicActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadTracksFromDb() {
        // Lấy dữ liệu từ Room trên luồng nền
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Track> tracks = db.trackDao().getAllTracks();
            runOnUiThread(() -> {
                trackList.clear();
                trackList.addAll(tracks);
                adapter.notifyDataSetChanged();
            });
        });
    }
}