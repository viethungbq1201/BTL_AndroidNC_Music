package com.example.btl_androidnc_music;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.room.Room;
import com.example.btl_androidnc_music.databinding.FragmentHomeBinding; // Sẽ khác với binding cũ

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

// HomeFragment vẫn implement listener CỦA ADAPTER BÊN TRONG (CategorySongAdapter)
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

    // VIẾT LẠI HOÀN TOÀN HÀM NÀY
    private void loadAllCategories() {
        Executors.newSingleThreadExecutor().execute(() -> {

            // <-- THÊM MỚI: Tải tất cả bài hát vào bộ nhớ cache -->
            // (Đảm bảo bạn đã có hàm getAllTracks() trong TrackDao)
            allTracksCache.clear();
            allTracksCache.addAll(db.trackDao().getAllTracks());
            // --- KẾT THÚC THÊM MỚI ---

            // 1. Lấy danh sách tên các thể loại duy nhất (Giữ nguyên)
            List<String> genres = db.trackDao().getAllUniqueGenres();

            List<CategoryRow> newRows = new ArrayList<>();

            // 2. Với mỗi tên thể loại, lấy danh sách bài hát (Giữ nguyên)
            for (String genre : genres) {
                // (Code này sẽ chạy chậm nếu có nhiều thể loại,
                //  nhưng chúng ta sẽ tối ưu sau nếu cần)
                List<Track> tracksForThisGenre = db.trackDao().getTracksByGenre(genre);

                if (tracksForThisGenre != null && !tracksForThisGenre.isEmpty()) {
                    newRows.add(new CategoryRow(genre, tracksForThisGenre));
                }
            }

            // 3. Cập nhật UI (Giữ nguyên)
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    categoryRowList.clear();
                    categoryRowList.addAll(newRows);
                    mainAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    // Hàm click này giữ nguyên, nó sẽ được gọi từ adapter BÊN TRONG
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
            // So sánh bằng ID để đảm bảo không thêm lại bài đã click
            if (track.id != clickedTrack.id) {
                finalPlaylist.add(track);
            }
        }

        // 5. [Phần 3] Thêm TẤT CẢ CÁC BÀI CÒN LẠI (khác thể loại)
        if (allTracksCache != null) {
            for (Track track : allTracksCache) {
                // Dùng TextUtils.equals để so sánh, an toàn với null
                // Nếu thể loại của bài hát này KHÁC với thể loại đã click
                if (!TextUtils.equals(track.genre, clickedGenre)) {
                    finalPlaylist.add(track);
                }
            }
        }

        // 6. Mở PlayerActivity với danh sách mới đã sắp xếp
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra("TRACK_LIST", finalPlaylist);

        // Vị trí luôn là 0, vì bài hát được click đã được đưa lên đầu
        intent.putExtra("TRACK_POSITION", 0);

        startActivity(intent);
    }
}