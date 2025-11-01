package com.example.btl_androidnc_music; // <-- THAY PACKAGE CỦA BẠN

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.btl_androidnc_music.databinding.ActivityPlayerBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private ListenableFuture<MediaController> controllerFuture;

    // SỬA: Chuyển từ "public static" thành "private"
    private MediaController mediaController;

    private ArrayList<Track> trackList;
    private int startPosition;

    // <-- THÊM MỚI: Hàm Getter -->
    public MediaController getServiceMediaController() {
        return mediaController;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Lấy danh sách nhạc và vị trí từ Intent
        trackList = (ArrayList<Track>) getIntent().getSerializableExtra("TRACK_LIST");
        startPosition = getIntent().getIntExtra("TRACK_POSITION", 0);

        // --- SỬA: XÓA PHẦN SETUP VIEWPAGER2 KHỎI ĐÂY ---
        // (Chúng ta sẽ gọi nó sau khi kết nối service thành công)
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Kết nối tới MusicPlayerService
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicPlayerService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();

        // Thêm một listener để xử lý khi kết nối thành công
        controllerFuture.addListener(() -> {
            try {
                // SỬA: Gán vào biến private
                mediaController = controllerFuture.get();

                // --- SỬA: CHỈ KHI KẾT NỐI THÀNH CÔNG, MỚI HIỂN THỊ FRAGMENT ---
                setupViewPager(); // <--- Gọi hàm setup ViewPager2

                // Khi đã kết nối, ra lệnh cho Service phát nhạc
                prepareAndPlayPlaylist();

            } catch (ExecutionException | InterruptedException e) {
                // Xử lý lỗi kết nối
                Toast.makeText(this, "Lỗi kết nối tới dịch vụ nhạc", Toast.LENGTH_SHORT).show();
                finish();
            }
        }, MoreExecutors.directExecutor());
    }

    // <-- THÊM MỚI: Hàm setup ViewPager2 -->
    private void setupViewPager() {
        PlayerViewPagerAdapter adapter = new PlayerViewPagerAdapter(this);
        binding.viewPagerPlayer.setAdapter(adapter);
        binding.viewPagerPlayer.setCurrentItem(1, false); // Mặc định là tab Player (vị trí 1)
    }

    private void prepareAndPlayPlaylist() {
        if (mediaController == null || trackList == null || trackList.isEmpty()) {
            return;
        }

        // Tạo danh sách MediaItem
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Track track : trackList) {
            mediaItems.add(new MediaItem.Builder()
                    .setMediaId(String.valueOf(track.id)) // Quan trọng
                    .setUri(Uri.fromFile(new File(track.filePath)))
                    .build());
        }

        mediaController.setMediaItems(mediaItems);
        mediaController.seekTo(startPosition, 0);
        mediaController.prepare();
        mediaController.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Giải phóng controller khi Activity dừng
        if (mediaController != null) {
            MediaController.releaseFuture(controllerFuture);
            mediaController = null; // SỬA: Gán private member về null
        }
    }

    // (Lớp ViewPagerAdapter bên trong giữ nguyên, không cần sửa)
    private class PlayerViewPagerAdapter extends FragmentStateAdapter {
        public PlayerViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new CommentsFragment(); // Màn hình Bình luận
                case 1:
                    return new PlayerControlsFragment(); // Màn hình Phát nhạc
                default:
                    return new PlayerControlsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Chỉ có 2 tab
        }
    }
}