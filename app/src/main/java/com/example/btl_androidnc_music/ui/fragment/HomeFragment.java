package com.example.btl_androidnc_music.ui.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.model.CategoryRow;
import com.example.btl_androidnc_music.data.model.Track;
import com.example.btl_androidnc_music.databinding.FragmentHomeBinding;
import com.example.btl_androidnc_music.service.MusicPlayerService;
import com.example.btl_androidnc_music.ui.activity.PlayerActivity;
import com.example.btl_androidnc_music.ui.adapter.CategorySongAdapter;
import com.example.btl_androidnc_music.ui.adapter.HomeCategoryAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment implements CategorySongAdapter.OnSongClickListener {

    private FragmentHomeBinding binding;
    private AppDatabase db;
    private HomeCategoryAdapter mainAdapter; // Adapter DỌC mới
    private List<CategoryRow> categoryRowList = new ArrayList<>(); // Danh sách các hàng
    private List<Track> allTracksCache = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        db = Room.databaseBuilder(requireContext(),
                        AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration()
                .build();

        setupMainRecyclerView();
        loadAllCategories();

        return binding.getRoot();
    }

    // Hàm cài đặt cho RecyclerView DỌC
    private void setupMainRecyclerView() {
        mainAdapter = new HomeCategoryAdapter(categoryRowList, this);
        binding.rvMainCategories.setAdapter(mainAdapter);
    }

    private void loadAllCategories() {
        Executors.newSingleThreadExecutor().execute(() -> {

            // 1. Tải TẤT CẢ bài hát vào bộ nhớ cache
            allTracksCache.clear();
            allTracksCache.addAll(db.trackDao().getAllTracks());

            // 2. Lấy danh sách tên thể loại (giữ nguyên)
            List<String> genres = db.trackDao().getAllUniqueGenres();
            List<CategoryRow> newRows = new ArrayList<>();

            // 3. Với mỗi tên thể loại, lấy danh sách bài hát (giữ nguyên)
            for (String genre : genres) {
                // (Đảm bảo TrackDao đã có hàm getTracksByGenre)
                List<Track> tracksForThisGenre = db.trackDao().getTracksByGenre(genre);

                if (tracksForThisGenre != null && !tracksForThisGenre.isEmpty()) {
                    newRows.add(new CategoryRow(genre, tracksForThisGenre));
                }
            }

            // 4. Cập nhật UI (giữ nguyên)
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    categoryRowList.clear();
                    categoryRowList.addAll(newRows);
                    mainAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    @Override
    public void onSongClick(ArrayList<Track> genreTrackList, int position) {
        // genreTrackList: là danh sách các bài CÙNG THỂ LOẠI (ví dụ: "Pop")
        // position: là vị trí bài hát được click TRONG danh sách đó

        // 1. Lấy bài hát được click
        Track clickedTrack = genreTrackList.get(position);
        String clickedGenre = clickedTrack.genre;

        // 2. Tạo danh sách phát mới
        ArrayList<Track> finalPlaylist = new ArrayList<>();

        // 3. [Phần 1] Thêm bài hát được click vào đầu tiên
        finalPlaylist.add(clickedTrack);

        // 4. [Phần 2] Thêm CÁC BÀI CÙNG THỂ LOẠI (trừ bài đã click)
        for (Track track : genreTrackList) {
            if (track.id != clickedTrack.id) {
                finalPlaylist.add(track);
            }
        }

        // 5. [Phần 3] Thêm TẤT CẢ CÁC BÀI CÒN LẠI (khác thể loại)
        // (Dùng cache mà chúng ta đã tải ở loadAllCategories)
        if (allTracksCache != null) {
            for (Track track : allTracksCache) {
                // Nếu thể loại của bài hát này KHÁC với thể loại đã click
                if (!TextUtils.equals(track.genre, clickedGenre)) {
                    finalPlaylist.add(track);
                }
            }
        }

        // 6. KHỞI ĐỘNG SERVICE
        Intent serviceIntent = new Intent(getActivity(), MusicPlayerService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("TRACK_LIST", finalPlaylist);
        bundle.putInt("TRACK_POSITION", 0); // Vị trí luôn là 0
        serviceIntent.putExtras(bundle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().startForegroundService(serviceIntent);
        } else {
            getActivity().startService(serviceIntent);
        }

        // 7. Mở PlayerActivity (vẫn gửi list để Activity xử lý)
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra("TRACK_LIST", finalPlaylist);
        intent.putExtra("TRACK_POSITION", 0);
        startActivity(intent);
    }
}