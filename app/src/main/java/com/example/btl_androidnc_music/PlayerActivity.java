package com.example.btl_androidnc_music;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.btl_androidnc_music.databinding.ActivityPlayerBinding;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private MusicPlayerManager playerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Lấy danh sách nhạc và vị trí từ Intent
        ArrayList<Track> trackList = (ArrayList<Track>) getIntent().getSerializableExtra("TRACK_LIST");
        int position = getIntent().getIntExtra("TRACK_POSITION", 0);

        // Lấy trình quản lý nhạc
        playerManager = MusicPlayerManager.getInstance(this);

        // Bắt đầu phát nhạc (nếu cần)
        // (Chúng ta có thể kiểm tra xem bài hát đang phát có giống bài được click không)
        playerManager.play(trackList, position);

        // Cài đặt ViewPager2
        PlayerViewPagerAdapter adapter = new PlayerViewPagerAdapter(this);
        binding.viewPagerPlayer.setAdapter(adapter);

        // Quan trọng: Đặt màn hình mặc định là màn hình Player (ở giữa, vị trí 1)
        binding.viewPagerPlayer.setCurrentItem(1, false);
    }

    // Adapter cho ViewPager2
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
            return 2; // Chúng ta có 3 tab
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Bạn có thể quyết định release player ở đây
        // HOẶC để nó tiếp tục chạy (nếu bạn dùng Service sau này)
        // playerManager.release(); // Tạm thời chưa release
    }
}