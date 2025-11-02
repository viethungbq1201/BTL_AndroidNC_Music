package com.example.btl_androidnc_music.ui.fragment; // <-- THAY PACKAGE CỦA BẠN

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
import androidx.media3.session.MediaController; // <-- SỬA: Import MediaController
import androidx.room.Room;

import com.example.btl_androidnc_music.R;
import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.model.Track;
import com.example.btl_androidnc_music.databinding.FragmentPlayerControlsBinding;
import com.example.btl_androidnc_music.ui.activity.PlayerActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PlayerControlsFragment extends Fragment {

    private FragmentPlayerControlsBinding binding;
    private MediaController mediaController; // <-- SỬA: Dùng MediaController
    private AppDatabase db;
    private Track mCurrentTrack;

    private Handler handler;
    private Runnable updateProgressRunnable;
    private Player.Listener playerListener; // Biến listener

    // Các biến trạng thái cho Loop
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo trình xin quyền
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (trackToDownload != null) {
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

        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        // Lấy MediaController từ PlayerActivity (cha của nó)
        if (getActivity() != null) {
            // SỬA: Gọi đúng tên hàm mới
            mediaController = ((PlayerActivity) getActivity()).getServiceMediaController();
        }

        if (mediaController == null) {
            Toast.makeText(requireContext(), "Lỗi trình phát nhạc", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().finish();
            }
            return view;
        }

        handler = new Handler(Looper.getMainLooper());

        initializeUpdateRunnable();
        setupClickListeners();
        setupPlayerListener();
        updateUiForNewTrack();

        return view;
    }

    // SỬA HÀM NÀY (Vì giờ ta lấy Track từ DB)
    private void updateUiForNewTrack() {
        if (mediaController == null || binding == null || !isAdded()) return;

        MediaItem currentItem = mediaController.getCurrentMediaItem();
        if (currentItem == null || currentItem.mediaId == null) return;

        // Lấy track ID từ MediaItem (chúng ta đã gán nó ở PlayerActivity)
        int trackId;
        try {
            trackId = Integer.parseInt(currentItem.mediaId);
        } catch (NumberFormatException e) {
            return; // ID không hợp lệ
        }

        // Lấy thông tin Track đầy đủ từ DB (chạy nền)
        Executors.newSingleThreadExecutor().execute(() -> {
            mCurrentTrack = db.trackDao().getTrackById(trackId); // <-- **CẦN THÊM HÀM NÀY VÀO DAO**

            if (mCurrentTrack != null && getActivity() != null) {
                // Cập nhật UI trên luồng chính
                getActivity().runOnUiThread(() -> {
                    if(binding == null || !isAdded()) return; // Kiểm tra lại

                    binding.tvSongTitle.setText(mCurrentTrack.title);
                    binding.tvSongArtist.setText(mCurrentTrack.artist);

                    if (mCurrentTrack.imagePath != null && !mCurrentTrack.imagePath.isEmpty()) {
                        binding.ivCoverArt.setImageURI(Uri.fromFile(new File(mCurrentTrack.imagePath)));
                    } else {
                        binding.ivCoverArt.setImageResource(R.drawable.ic_music_note);
                    }
                    setFavoriteButtonState(mCurrentTrack.isFavorite);
                });
            }
        });
    }

    // SỬA HÀM NÀY (Đổi exoPlayer -> mediaController)
    private void setupPlayerListener() {
        if (mediaController == null) return;

        if (playerListener == null) {
            playerListener = new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (binding == null || !isAdded()) return;

                    if (playbackState == Player.STATE_READY) {
                        long totalDuration = mediaController.getDuration();
                        binding.seekBar.setMax((int) totalDuration);
                        binding.tvTotalTime.setText(formatTime(totalDuration));
                        handler.post(updateProgressRunnable);
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        if (currentLoopMode == LOOP_MODE_ONE_SHOT && !hasLoopedOnce) {
                            hasLoopedOnce = true;
                            mediaController.seekTo(0);
                            mediaController.play();
                        }
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (binding == null || !isAdded()) return;

                    if (isPlaying) {
                        handler.post(updateProgressRunnable);
                        binding.btnPlayPause.setImageResource(R.drawable.ic_pause);
                    } else {
                        handler.removeCallbacks(updateProgressRunnable);
                        binding.btnPlayPause.setImageResource(R.drawable.ic_play);
                    }
                }

                @Override
                public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                    if (binding == null || !isAdded()) return;

                    updateUiForNewTrack(); // Tải thông tin bài mới

                    if (currentLoopMode == LOOP_MODE_ONE_SHOT) {
                        setLoopMode(LOOP_MODE_OFF);
                    }
                    hasLoopedOnce = false;
                }
            };
        }

        mediaController.addListener(playerListener);

        if (binding != null) {
            binding.btnPlayPause.setImageResource(mediaController.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    // SỬA HÀM NÀY (Đổi exoPlayer -> mediaController)
    private void setupClickListeners() {
        binding.btnPlayPause.setOnClickListener(v -> {
            if (mediaController.isPlaying()) {
                mediaController.pause();
            } else {
                mediaController.play();
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (mediaController.hasNextMediaItem()) {
                mediaController.seekToNextMediaItem();
            } else if (currentLoopMode != LOOP_MODE_OFF) {
                mediaController.seekTo(0, 0); // Quay về bài đầu
            } else {
                Toast.makeText(requireContext(), "Đã hết danh sách phát", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnPrevious.setOnClickListener(v -> mediaController.seekToPreviousMediaItem());

        binding.btnLoop.setOnClickListener(v -> {
            if (currentLoopMode == LOOP_MODE_OFF) {
                setLoopMode(LOOP_MODE_ONE_SHOT);
            } else if (currentLoopMode == LOOP_MODE_ONE_SHOT) {
                setLoopMode(LOOP_MODE_INFINITE);
            } else {
                setLoopMode(LOOP_MODE_OFF);
            }
        });

        binding.btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            mediaController.setShuffleModeEnabled(isShuffle); // Sửa
            if (isShuffle) {
                binding.btnShuffle.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green));
                Toast.makeText(requireContext(), "Shuffle On", Toast.LENGTH_SHORT).show();
            } else {
                binding.btnShuffle.clearColorFilter();
                Toast.makeText(requireContext(), "Shuffle Off", Toast.LENGTH_SHORT).show();
            }
        });

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
                mediaController.seekTo(seekBar.getProgress()); // Sửa
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
        if (mediaController == null) return;

        if (mode == LOOP_MODE_OFF) {
            mediaController.setRepeatMode(Player.REPEAT_MODE_OFF);
            binding.btnLoop.clearColorFilter();
            binding.btnLoop.setImageResource(R.drawable.ic_loop);
            Toast.makeText(requireContext(), "Loop Off", Toast.LENGTH_SHORT).show();
        } else if (mode == LOOP_MODE_ONE_SHOT) {
            mediaController.setRepeatMode(Player.REPEAT_MODE_OFF);
            binding.btnLoop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green));
            binding.btnLoop.setImageResource(R.drawable.ic_repeat_one);
            Toast.makeText(requireContext(), "Loop One Shot", Toast.LENGTH_SHORT).show();
        } else if (mode == LOOP_MODE_INFINITE) {
            mediaController.setRepeatMode(Player.REPEAT_MODE_ONE);
            binding.btnLoop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green));
            binding.btnLoop.setImageResource(R.drawable.ic_loop);
            Toast.makeText(requireContext(), "Loop Infinite", Toast.LENGTH_SHORT).show();
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
                if (mediaController != null && mediaController.isPlaying() && binding != null && isAdded()) {
                    long currentPosition = mediaController.getCurrentPosition();
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

        if (handler != null) {
            handler.removeCallbacks(updateProgressRunnable);
        }
        // Gỡ listener
        if (mediaController != null && playerListener != null) {
            mediaController.removeListener(playerListener);
        }
        binding = null;
    }
}