package com.example.btl_androidnc_music;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.Nullable; // <-- THÊM MỚI
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.room.Room;

import com.example.btl_androidnc_music.R; // <-- THÊM MỚI
import com.example.btl_androidnc_music.databinding.ActivityPlayerBinding; // <-- Thay package của bạn

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.activity.result.ActivityResultLauncher; // Thêm import
import androidx.activity.result.contract.ActivityResultContracts; // Thêm import

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private ExoPlayer exoPlayer;
    private List<Track> trackList = new ArrayList<>();

    // -- Quản lý trạng thái tùy chỉnh --
    private boolean isShuffle = false;

    // 0 = Tắt, 1 = Loop One (Lặp 1 lần), 2 = Loop All (Lặp vô hạn)
    private static final int LOOP_MODE_OFF = 0;
    private static final int LOOP_MODE_ONE_SHOT = 1;
    private static final int LOOP_MODE_INFINITE = 2;
    private int currentLoopMode = LOOP_MODE_OFF;

    // Cờ (flag) để theo dõi logic "Loop One Shot"
    private boolean hasLoopedOnce = false;
    // -- ----------------------------- --

    private Handler handler;
    private Runnable updateProgressRunnable;
    private AppDatabase db;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Track trackToDownload = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Lấy danh sách nhạc (đã sửa lỗi unchecked)
        @SuppressWarnings("unchecked")
        List<Track> receivedList = (List<Track>) getIntent().getSerializableExtra("TRACK_LIST");
        if (receivedList != null) {
            trackList.addAll(receivedList);
        }
        int startPosition = getIntent().getIntExtra("TRACK_POSITION", 0);

        handler = new Handler(Looper.getMainLooper());

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration() // Nhớ thêm dòng này
                .build();

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Nếu được cấp quyền, bắt đầu tải
                        if (trackToDownload != null) {
                            saveTrackWithMediaStore(trackToDownload);
                        }
                    } else {
                        Toast.makeText(this, "Cần cấp quyền để tải nhạc", Toast.LENGTH_SHORT).show();
                    }
                });

        initializePlayer();
        setupClickListeners();
        preparePlaylist(startPosition); // Chuẩn bị toàn bộ danh sách nhạc
    }

    /**
     * Khởi tạo ExoPlayer và các Listener (Trái tim của logic)
     */
    private void initializePlayer() {
        exoPlayer = new ExoPlayer.Builder(this).build();
        initializeUpdateRunnable(); // Khởi tạo Runnable cho SeekBar

        // Thêm Listener để xử lý các sự kiện của trình phát
        exoPlayer.addListener(new Player.Listener() {

            // Xử lý khi trạng thái phát thay đổi (Play, Pause, Sẵn sàng, KẾT THÚC)
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                // Khi nhạc sẵn sàng
                if (playbackState == Player.STATE_READY) {
                    long totalDuration = exoPlayer.getDuration();
                    binding.seekBar.setMax((int) totalDuration);
                    binding.tvTotalTime.setText(formatTime(totalDuration));
                    handler.post(updateProgressRunnable); // Bắt đầu cập nhật SeekBar
                }

                // Khi bài hát KẾT THÚC
                if (playbackState == Player.STATE_ENDED) {
                    // Xử lý logic LOOP ONE (Lặp 1 lần)
                    if (currentLoopMode == LOOP_MODE_ONE_SHOT && !hasLoopedOnce) {
                        hasLoopedOnce = true; // Đánh dấu là đã lặp 1 lần
                        exoPlayer.seekTo(0); // Tua về đầu
                        exoPlayer.play(); // Phát lại
                        return; // Đã xử lý, không làm gì thêm
                    }

                    // Nếu là LOOP INFINITE (Loop All), ExoPlayer sẽ tự lặp lại
                    // Nếu là LOOP OFF, ExoPlayer sẽ tự chuyển bài tiếp theo
                }
            }

            // Xử lý khi đang phát hay đang dừng
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    handler.post(updateProgressRunnable);
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause);
                } else {
                    handler.removeCallbacks(updateProgressRunnable);
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play);
                }
            }

            // Xử lý khi bài hát bị chuyển (do Next, Previous, hoặc tự động)
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                updateUiForNewTrack(); // Cập nhật tên bài hát, tác giả

                // Logic chính cho LOOP ONE: Tự động tắt khi chuyển bài
                if (currentLoopMode == LOOP_MODE_ONE_SHOT) {
                    // Nếu bài hát tự động chuyển (REASON_AUTO)
                    // hoặc người dùng nhấn Next/Prev (REASON_SEEK)
                    // thì tắt chế độ Loop One.
                    setLoopMode(LOOP_MODE_OFF);
                }

                // Khi chuyển bài, luôn reset cờ "đã lặp"
                hasLoopedOnce = false;
            }
        });
    }

    /**
     * Chuẩn bị toàn bộ danh sách nhạc cho ExoPlayer
     */
    private void preparePlaylist(int startPosition) {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Track track : trackList) {
            Uri trackUri = Uri.fromFile(new File(track.filePath));
            mediaItems.add(MediaItem.fromUri(trackUri));
        }

        exoPlayer.setMediaItems(mediaItems); // Đưa cả danh sách vào
        exoPlayer.seekTo(startPosition, 0); // Tua đến bài hát được chọn
        exoPlayer.prepare();
        exoPlayer.play();
        updateUiForNewTrack(); // Cập nhật UI cho bài hát đầu tiên
    }

    // Hàm helper để cập nhật icon trái tim
    private void setFavoriteButtonState(boolean isFavorite) {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_filled); // <-- Cần icon tim đầy
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border); // <-- Cần icon tim rỗng
        }
    }

    // Hàm helper để lưu vào DB trên luồng nền
    private void updateTrackInDatabase(Track track) {
        // Chạy trên luồng nền để không làm treo UI
        Executors.newSingleThreadExecutor().execute(() -> {
            db.trackDao().updateTrack(track);
        });
    }

    /**
     * Cập nhật UI (Tên bài, Tác giả) khi chuyển bài
     */
    private void updateUiForNewTrack() {
        if (exoPlayer.getCurrentMediaItem() == null) return;
        int currentTrackIndex = exoPlayer.getCurrentMediaItemIndex();
        Track currentTrack = trackList.get(currentTrackIndex);
        binding.tvSongTitle.setText(currentTrack.title);
        binding.tvSongArtist.setText(currentTrack.artist);
        setFavoriteButtonState(currentTrack.isFavorite);
    }

    /**
     * Gán sự kiện click cho tất cả các nút
     */
    private void setupClickListeners() {
        // --- CÁC NÚT CHUẨN ---
        binding.btnPlayPause.setOnClickListener(v -> {
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
            } else {
                exoPlayer.play();
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            exoPlayer.seekToNextMediaItem();
        });

        binding.btnPrevious.setOnClickListener(v -> {
            exoPlayer.seekToPreviousMediaItem();
        });

        // --- NÚT SHUFFLE ---
        binding.btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle; // Đảo trạng thái
            exoPlayer.setShuffleModeEnabled(isShuffle); // Bật/tắt shuffle

            if (isShuffle) {
                binding.btnShuffle.setColorFilter(ContextCompat.getColor(this, R.color.green));
                Toast.makeText(this, "Shuffle On", Toast.LENGTH_SHORT).show();
            } else {
                binding.btnShuffle.clearColorFilter();
                Toast.makeText(this, "Shuffle Off", Toast.LENGTH_SHORT).show();
            }
        });

        // --- NÚT LOOP (LOGIC TÙY CHỈNH) ---
        binding.btnLoop.setOnClickListener(v -> {
            if (currentLoopMode == LOOP_MODE_OFF) {
                setLoopMode(LOOP_MODE_ONE_SHOT); // Chuyển sang Lặp 1 lần
            } else if (currentLoopMode == LOOP_MODE_ONE_SHOT) {
                setLoopMode(LOOP_MODE_INFINITE); // Chuyển sang Lặp vô hạn
            } else {
                setLoopMode(LOOP_MODE_OFF); // Tắt Loop
            }
        });

        // --- SEEKBAR (Giữ nguyên) ---
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateProgressRunnable);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                exoPlayer.seekTo(seekBar.getProgress());
                handler.post(updateProgressRunnable);
            }
        });

        binding.btnFavorite.setOnClickListener(v -> {
            // Lấy bài hát hiện tại
            int currentTrackIndex = exoPlayer.getCurrentMediaItemIndex();
            Track currentTrack = trackList.get(currentTrackIndex);

            // Đảo ngược trạng thái
            currentTrack.isFavorite = !currentTrack.isFavorite;

            // Cập nhật UI (icon)
            setFavoriteButtonState(currentTrack.isFavorite);

            // Lưu vào Database (chạy nền)
            updateTrackInDatabase(currentTrack);

            if (currentTrack.isFavorite) {
                Toast.makeText(this, "Đã thêm vào Yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đã xóa khỏi Yêu thích", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnDownload.setOnClickListener(v -> {
            int currentTrackIndex = exoPlayer.getCurrentMediaItemIndex();
            trackToDownload = trackList.get(currentTrackIndex);

            // Kiểm tra quyền trước khi tải
            checkAndRequestPermission();
        });
    }

    private void checkAndRequestPermission() {
        // Chỉ cần yêu cầu quyền cho Android 9 (P) trở xuống
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) {
                // Đã có quyền, tiến hành tải (cách cũ)
                saveTrackForLegacy(trackToDownload);
            } else {
                // Chưa có quyền, yêu cầu quyền
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } else {
            // Android 10+ không cần quyền, sử dụng MediaStore
            saveTrackWithMediaStore(trackToDownload);
        }
    }

    /**
     * Cách 1: Lưu file cho Android 10 (Q) trở lên (Dùng MediaStore)
     */
    private void saveTrackWithMediaStore(Track track) {
        Toast.makeText(this, "Đang tải xuống...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();

            // Lấy tên file gốc (ví dụ: track_12345.mp3)
            String fileName = new File(track.filePath).getName();

            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
            // Đặt đường dẫn lưu là thư mục Music
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + File.separator + "BTL_AndroidNC_Music");

            // Chèn một mục mới vào MediaStore
            Uri uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);

            if (uri != null) {
                try (OutputStream outputStream = resolver.openOutputStream(uri);
                     InputStream inputStream = new FileInputStream(track.filePath)) {

                    // Sao chép file từ bộ nhớ riêng ra bộ nhớ chung
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }

                    // Thành công
                    runOnUiThread(() -> Toast.makeText(this, "Đã tải về thư mục Music/BTL_AndroidNC_Music", Toast.LENGTH_LONG).show());

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Tải xuống thất bại", Toast.LENGTH_SHORT).show());
                }
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Tải xuống thất bại", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Cách 2: Lưu file cho Android 9 (P) trở xuống (Cách cũ)
     */
    private void saveTrackForLegacy(Track track) {
        Toast.makeText(this, "Đang tải xuống ...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            File sourceFile = new File(track.filePath);
            String fileName = sourceFile.getName();

            File destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            File appMusicDir = new File(destinationDir, "BTL_AndroidNC_Music"); // Tên app của bạn
            if (!appMusicDir.exists()) {
                appMusicDir.mkdirs();
            }

            File destinationFile = new File(appMusicDir, fileName);

            boolean success = copyFile(sourceFile, destinationFile); // Dùng lại hàm copyFile cũ

            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Đã tải về thư mục Music/BTL_AndroidNC_Music", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Tải xuống thất bại", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Hàm tiện ích để sao chép (Giữ nguyên như code của bạn)
     */
    private boolean copyFile(File source, File destination) {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace(); // Lỗi sẽ được in ra ở đây
            return false;
        }
    }

    /**
     * Hàm helper trung tâm để quản lý trạng thái LOOP
     */
    private void setLoopMode(int mode) {
        currentLoopMode = mode;
        hasLoopedOnce = false; // Luôn reset cờ khi đổi chế độ

        if (mode == LOOP_MODE_OFF) {
            // TẮT: Tắt lặp của ExoPlayer
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            binding.btnLoop.clearColorFilter();
            binding.btnLoop.setImageResource(R.drawable.ic_loop);
            Toast.makeText(this, "Loop Off", Toast.LENGTH_SHORT).show();

        } else if (mode == LOOP_MODE_ONE_SHOT) {
            // LẶP 1 LẦN: Vẫn TẮT lặp của ExoPlayer (vì chúng ta tự xử lý)
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            binding.btnLoop.setColorFilter(ContextCompat.getColor(this, R.color.green));
            binding.btnLoop.setImageResource(R.drawable.ic_repeat_one); // Dùng icon lặp 1
            Toast.makeText(this, "Loop One Shot", Toast.LENGTH_SHORT).show();

        } else if (mode == LOOP_MODE_INFINITE) {
            // LẶP VÔ HẠN (Loop All): BẬT chế độ lặp 1 BÀI của ExoPlayer
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            binding.btnLoop.setColorFilter(ContextCompat.getColor(this, R.color.green));
            binding.btnLoop.setImageResource(R.drawable.ic_loop); // Dùng icon lặp thường
            Toast.makeText(this, "Loop All (Infinite)", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Các hàm tiện ích (Giữ nguyên) ---

    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d", minutes, seconds);
    }

    private void initializeUpdateRunnable() {
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && exoPlayer.isPlaying()) {
                    long currentPosition = exoPlayer.getCurrentPosition();
                    binding.seekBar.setProgress((int) currentPosition);
                    binding.tvCurrentTime.setText(formatTime(currentPosition));
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateProgressRunnable);
        exoPlayer.release(); // Rất quan trọng!
    }
}