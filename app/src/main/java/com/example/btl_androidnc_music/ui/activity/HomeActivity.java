package com.example.btl_androidnc_music.ui.activity; // <-- THAY PACKAGE CỦA BẠN

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.room.Room;

import com.example.btl_androidnc_music.R;
import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.model.Track;
import com.example.btl_androidnc_music.databinding.ActivityHomeBinding;
import com.example.btl_androidnc_music.service.MusicPlayerService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    // --- THÊM CÁC BIẾN MỚI ---
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;
    private Player.Listener playerListener;
    private AppDatabase db;
    private Track mCurrentTrack; // Lưu lại track hiện tại

    // Các View của Mini-player
    private View miniPlayerContainer;
    private ImageButton btnMiniPlayPause, btnMiniFavorite, btnMiniNext;
    private TextView tvMiniTitle, tvMiniArtist;
    // --- KẾT THÚC THÊM BIẾN ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Bottom Navigation (Giữ nguyên)
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        // --- THÊM MỚI: Khởi tạo DB và View ---
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        // Ánh xạ các View từ mini-player (layout đã được include)
        miniPlayerContainer = binding.miniPlayerContainer;
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniFavorite = findViewById(R.id.btnMiniFavorite);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);

        // Cài đặt sự kiện click cho mini-player
        setupMiniPlayerClickListeners();
    }

    // --- SỬA HÀM onStart ---
    @Override
    protected void onStart() {
        super.onStart();
        // Kết nối với Service khi Activity hiển thị
        connectToService();
    }

    // --- SỬA HÀM onStop ---
    @Override
    protected void onStop() {
        super.onStop();
        // Ngắt kết nối khỏi Service khi Activity bị ẩn
        disconnectFromService();
    }

    // --- THÊM HÀM MỚI: Kết nối tới Service ---
    private void connectToService() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicPlayerService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                // Kết nối thành công, bắt đầu lắng nghe
                setupMediaControllerListener();
                // Cập nhật UI ngay lập tức
                updateMiniPlayerUi();
            } catch (ExecutionException | InterruptedException e) {
                // Lỗi (có thể service chưa chạy)
            }
        }, MoreExecutors.directExecutor());
    }

    // --- THÊM HÀM MỚI: Ngắt kết nối ---
    private void disconnectFromService() {
        if (mediaController != null && playerListener != null) {
            mediaController.removeListener(playerListener);
        }
        MediaController.releaseFuture(controllerFuture);
        mediaController = null;
    }

    // --- THÊM HÀM MỚI: Lắng nghe Service ---
    private void setupMediaControllerListener() {
        playerListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                // HIỂN THỊ hoặc ẨN mini-player
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    miniPlayerContainer.setVisibility(View.GONE);
                } else {
                    miniPlayerContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                // Cập nhật nút Play/Pause
                btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                // Khi chuyển bài, cập nhật lại toàn bộ UI
                updateMiniPlayerUi();
            }
        };
        // Gắn listener vào controller
        mediaController.addListener(playerListener);
    }

    // --- THÊM HÀM MỚI: Cập nhật UI (Tên bài, tác giả, nút like) ---
    private void updateMiniPlayerUi() {
        if (mediaController == null || mediaController.getCurrentMediaItem() == null) {
            miniPlayerContainer.setVisibility(View.GONE);
            return;
        }

        MediaItem currentItem = mediaController.getCurrentMediaItem();
        String mediaId = currentItem.mediaId;
        if (mediaId == null) return;

        // Lấy thông tin Track từ DB (chạy nền)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int trackId = Integer.parseInt(mediaId);
                mCurrentTrack = db.trackDao().getTrackById(trackId); // Dùng hàm đã tạo

                if (mCurrentTrack != null) {
                    runOnUiThread(() -> {
                        // Cập nhật tên bài, tác giả
                        tvMiniTitle.setText(mCurrentTrack.title);
                        tvMiniArtist.setText(mCurrentTrack.artist);
                        // Cập nhật nút Yêu thích
                        btnMiniFavorite.setImageResource(mCurrentTrack.isFavorite ?
                                R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
                        // Cập nhật nút Play/Pause
                        btnMiniPlayPause.setImageResource(mediaController.isPlaying() ?
                                R.drawable.ic_pause : R.drawable.ic_play);
                        // Hiển thị Mini-player
                        miniPlayerContainer.setVisibility(View.VISIBLE);
                    });
                }
            } catch (NumberFormatException e) {
                // ID không hợp lệ
            }
        });
    }

    // --- THÊM HÀM MỚI: Gán sự kiện Click cho các nút ---
    private void setupMiniPlayerClickListeners() {
        // 1. Click vào cả thanh player -> Mở PlayerActivity
        miniPlayerContainer.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            // KHÔNG cần gửi list nhạc, vì Service đã có sẵn
            startActivity(intent);
        });

        // 2. Click nút Play/Pause
        btnMiniPlayPause.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });

        // 3. Click nút Next
        btnMiniNext.setOnClickListener(v -> {
            if (mediaController != null && mediaController.hasNextMediaItem()) {
                mediaController.seekToNextMediaItem();
            } else if (mediaController != null) {
                // Nếu hết bài, quay về bài đầu
                mediaController.seekTo(0, 0);
            }
        });

        // 4. Click nút Favorite
        btnMiniFavorite.setOnClickListener(v -> {
            if (mCurrentTrack != null) {
                mCurrentTrack.isFavorite = !mCurrentTrack.isFavorite;
                // Cập nhật icon ngay lập tức
                btnMiniFavorite.setImageResource(mCurrentTrack.isFavorite ?
                        R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
                // Cập nhật vào DB (chạy nền)
                Executors.newSingleThreadExecutor().execute(() -> {
                    db.trackDao().updateTrack(mCurrentTrack);
                });
            }
        });
    }
}