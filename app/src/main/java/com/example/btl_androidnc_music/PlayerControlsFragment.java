package com.example.btl_androidnc_music;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.room.Room;

import com.example.btl_androidnc_music.databinding.FragmentPlayerControlsBinding; // <-- Đổi tên file binding nếu cần

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Fragment này chứa giao diện điều khiển trình phát nhạc chính (play, pause, seekbar, v.v.)
 */
public class PlayerControlsFragment extends Fragment {

    private FragmentPlayerControlsBinding binding;
    private ExoPlayer exoPlayer;
    private AppDatabase db;
    private Track mCurrentTrack; // Bài hát đang được phát

    private Handler handler;
    private Runnable updateProgressRunnable;

    // Các biến trạng thái cho nút Loop (theo logic tùy chỉnh của bạn)
    private static final int LOOP_MODE_OFF = 0;
    private static final int LOOP_MODE_ONE_SHOT = 1;
    private static final int LOOP_MODE_INFINITE = 2;
    private int currentLoopMode = LOOP_MODE_OFF;
    private boolean hasLoopedOnce = false;

    // Biến trạng thái cho Shuffle
    private boolean isShuffle = false;

    // Launcher để xin quyền Tải xuống
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Track trackToDownload = null;
    private Player.Listener playerListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo trình xin quyền (phải làm trong onCreate hoặc onAttach)
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (trackToDownload != null) {
                            // Người dùng vừa cấp quyền -> tiến hành lưu file
                            saveTrackForLegacy(trackToDownload);
                        }
                    } else {
                        Toast.makeText(requireContext(), "Cần cấp quyền để tải nhạc", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerControlsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Lấy DB
        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        // Lấy ExoPlayer từ Singleton
        exoPlayer = MusicPlayerManager.getInstance(requireContext()).getExoPlayer();

        handler = new Handler(Looper.getMainLooper());

        initializeUpdateRunnable();
        setupClickListeners();
        setupPlayerListener();
        updateUiForNewTrack(); // Cập nhật UI ngay khi Fragment được tạo

        return view;
    }

    // Hàm này lấy bài hát hiện tại từ Manager
    private void updateUiForNewTrack() {
        mCurrentTrack = MusicPlayerManager.getInstance(requireContext()).getCurrentTrack();
        if (mCurrentTrack == null || binding == null) return;

        binding.tvSongTitle.setText(mCurrentTrack.title);
        binding.tvSongArtist.setText(mCurrentTrack.artist);

        // Cập nhật ảnh bìa
        if (mCurrentTrack.imagePath != null && !mCurrentTrack.imagePath.isEmpty()) {
            binding.ivCoverArt.setImageURI(Uri.fromFile(new File(mCurrentTrack.imagePath)));
        } else {
            binding.ivCoverArt.setImageResource(R.drawable.ic_music_note);
        }

        // Cập nhật nút yêu thích
        setFavoriteButtonState(mCurrentTrack.isFavorite);
    }

    // Cài đặt listener để lắng nghe sự kiện từ ExoPlayer
    // Cài đặt listener để lắng nghe sự kiện từ ExoPlayer
    private void setupPlayerListener() {
        if (exoPlayer == null) return;

        // --- SỬA LỖI Ở ĐÂY ---
        // Chúng ta phải khởi tạo listener TRƯỚC KHI sử dụng
        if (playerListener == null) {
            playerListener = new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    // Thêm kiểm tra binding null để tránh crash khi chuyển bài
                    if (binding == null) return;

                    if (playbackState == Player.STATE_READY) {
                        // Nhạc đã sẵn sàng, lấy tổng thời gian
                        long totalDuration = exoPlayer.getDuration();
                        binding.seekBar.setMax((int) totalDuration);
                        binding.tvTotalTime.setText(formatTime(totalDuration));
                        handler.post(updateProgressRunnable);
                    }

                    // Khi bài hát KẾT THÚC (xử lý logic Loop One)
                    if (playbackState == Player.STATE_ENDED) {
                        if (currentLoopMode == LOOP_MODE_ONE_SHOT && !hasLoopedOnce) {
                            hasLoopedOnce = true;
                            exoPlayer.seekTo(0);
                            exoPlayer.play();
                        }
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (binding == null) return; // Kiểm tra null

                    if (isPlaying) {
                        handler.post(updateProgressRunnable);
                        binding.btnPlayPause.setImageResource(R.drawable.ic_pause);
                    } else {
                        handler.removeCallbacks(updateProgressRunnable);
                        binding.btnPlayPause.setImageResource(R.drawable.ic_play);
                    }
                }

                // Khi bài hát tự động chuyển hoặc do nhấn Next/Prev
                @Override
                public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                    if (binding == null) return; // Kiểm tra null

                    updateUiForNewTrack();

                    // Tự động tắt LOOP ONE khi chuyển bài
                    if (currentLoopMode == LOOP_MODE_ONE_SHOT) {
                        setLoopMode(LOOP_MODE_OFF);
                    }
                    hasLoopedOnce = false; // Reset cờ lặp
                }
            };
        }
        // --- KẾT THÚC SỬA ---

        // Bây giờ listener chắc chắn không null, ta thêm nó vào
        exoPlayer.addListener(playerListener);

        // Cập nhật trạng thái nút Play/Pause ban đầu
        if (binding != null) { // Thêm kiểm tra ở đây cho chắc
            binding.btnPlayPause.setImageResource(exoPlayer.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    // Gán sự kiện click cho tất cả các nút
    private void setupClickListeners() {
        // Play/Pause
        binding.btnPlayPause.setOnClickListener(v -> {
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
            } else {
                exoPlayer.play();
            }
        });

        // Next
        binding.btnNext.setOnClickListener(v -> exoPlayer.seekToNextMediaItem());

        // Previous
        binding.btnPrevious.setOnClickListener(v -> exoPlayer.seekToPreviousMediaItem());

        // Loop (Logic tùy chỉnh)
        binding.btnLoop.setOnClickListener(v -> {
            if (currentLoopMode == LOOP_MODE_OFF) {
                setLoopMode(LOOP_MODE_ONE_SHOT);
            } else if (currentLoopMode == LOOP_MODE_ONE_SHOT) {
                setLoopMode(LOOP_MODE_INFINITE);
            } else {
                setLoopMode(LOOP_MODE_OFF);
            }
        });

        // Shuffle
        binding.btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            exoPlayer.setShuffleModeEnabled(isShuffle);
            if (isShuffle) {
                binding.btnShuffle.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green)); // Đổi màu
                Toast.makeText(requireContext(), "Shuffle On", Toast.LENGTH_SHORT).show();
            } else {
                binding.btnShuffle.clearColorFilter();
                Toast.makeText(requireContext(), "Shuffle Off", Toast.LENGTH_SHORT).show();
            }
        });

        // SeekBar
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

        // Nút Yêu thích
        binding.btnFavorite.setOnClickListener(v -> {
            if (mCurrentTrack == null) return;

            mCurrentTrack.isFavorite = !mCurrentTrack.isFavorite;
            setFavoriteButtonState(mCurrentTrack.isFavorite);
            updateTrackInDatabase(mCurrentTrack); // Lưu vào DB

            if (mCurrentTrack.isFavorite) {
                Toast.makeText(requireContext(), "Đã thêm vào Yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Đã xóa khỏi Yêu thích", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút Tải xuống
        binding.btnDownload.setOnClickListener(v -> {
            if (mCurrentTrack == null) return;
            trackToDownload = mCurrentTrack; // Lưu bài hát cần tải
            checkAndRequestPermission(); // Bắt đầu quy trình xin quyền/tải
        });
    }

    // --- CÁC HÀM HELPER (Hỗ trợ) ---

    // Hàm helper cho Nút Loop
    private void setLoopMode(int mode) {
        currentLoopMode = mode;
        hasLoopedOnce = false;

        if (mode == LOOP_MODE_OFF) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF); // Tắt lặp
            binding.btnLoop.clearColorFilter();
            binding.btnLoop.setImageResource(R.drawable.ic_loop);
            Toast.makeText(requireContext(), "Loop Off", Toast.LENGTH_SHORT).show();
        } else if (mode == LOOP_MODE_ONE_SHOT) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF); // Vẫn tắt lặp (vì ta tự xử lý)
            binding.btnLoop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green)); // Đổi màu
            binding.btnLoop.setImageResource(R.drawable.ic_repeat_one); // Đổi icon
            Toast.makeText(requireContext(), "Loop One Shot", Toast.LENGTH_SHORT).show();
        } else if (mode == LOOP_MODE_INFINITE) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE); // Bật lặp 1 BÀI VÔ HẠN
            binding.btnLoop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green));
            binding.btnLoop.setImageResource(R.drawable.ic_loop); // Dùng icon lặp thường
            Toast.makeText(requireContext(), "Loop All (Infinite)", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm helper cho Nút Yêu thích
    private void setFavoriteButtonState(boolean isFavorite) {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    // Hàm helper để Cập nhật DB
    private void updateTrackInDatabase(Track track) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.trackDao().updateTrack(track);
        });
    }

    // Hàm helper cho SeekBar (Format thời gian)
    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d", minutes, seconds);
    }

    // Hàm helper cho SeekBar (Runnable)
    private void initializeUpdateRunnable() {
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && exoPlayer.isPlaying() && binding != null) {
                    long currentPosition = exoPlayer.getCurrentPosition();
                    binding.seekBar.setProgress((int) currentPosition);
                    binding.tvCurrentTime.setText(formatTime(currentPosition));
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }

    // --- LOGIC TẢI XUỐNG (Copy từ code cũ) ---

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                saveTrackForLegacy(trackToDownload);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } else {
            saveTrackWithMediaStore(trackToDownload);
        }
    }

    private void saveTrackWithMediaStore(Track track) {
        Toast.makeText(requireContext(), "Đang tải xuống...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            ContentResolver resolver = requireActivity().getContentResolver();
            ContentValues contentValues = new ContentValues();
            String fileName = new File(track.filePath).getName();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + File.separator + "BTL_AndroidNC_Music");
            Uri uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);

            if (uri != null) {
                try (OutputStream outputStream = resolver.openOutputStream(uri);
                     InputStream inputStream = new FileInputStream(track.filePath)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Đã tải về thư mục Music/BTL_AndroidNC_Music", Toast.LENGTH_LONG).show());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Tải xuống thất bại", Toast.LENGTH_SHORT).show());
                    }
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Tải xuống thất bại", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void saveTrackForLegacy(Track track) {
        Toast.makeText(requireContext(), "Đang tải xuống...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            File sourceFile = new File(track.filePath);
            String fileName = sourceFile.getName();
            File destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            File appMusicDir = new File(destinationDir, "BTL_AndroidNC_Music");
            if (!appMusicDir.exists()) {
                appMusicDir.mkdirs();
            }
            File destinationFile = new File(appMusicDir, fileName);
            boolean success = copyFile(sourceFile, destinationFile);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(requireContext(), "Đã tải về thư mục Music/BTL_AndroidNC_Music", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Tải xuống thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

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
            e.printStackTrace();
            return false;
        }
    }

    // Dọn dẹp listener khi Fragment bị hủy View
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Gỡ runnable của seekbar
        if (handler != null) {
            handler.removeCallbacks(updateProgressRunnable);
        }

        // --- SỬA LỖI Ở ĐÂY ---
        // Gỡ listener ra khỏi player để tránh crash
        if (exoPlayer != null && playerListener != null) {
            exoPlayer.removeListener(playerListener);
        }
        // --- KẾT THÚC SỬA ---

        binding = null; // Quan trọng: Tránh memory leak
    }
}