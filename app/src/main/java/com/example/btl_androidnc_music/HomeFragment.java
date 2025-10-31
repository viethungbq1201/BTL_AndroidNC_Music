package com.example.btl_androidnc_music;

import android.content.Intent;
import android.os.Bundle;
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

            // 1. Lấy danh sách tên các thể loại duy nhất (ví dụ: "Pop", "Ballad"...)
            List<String> genres = db.trackDao().getAllUniqueGenres();

            // 2. Tạo một danh sách mới để chứa các hàng
            List<CategoryRow> newRows = new ArrayList<>();

            // 3. Với mỗi tên thể loại, lấy danh sách bài hát tương ứng
            for (String genre : genres) {
                // (Hàm getTracksByGenre bạn đã có từ trước)
                List<Track> tracksForThisGenre = db.trackDao().getTracksByGenre(genre);

                // Nếu thể loại này có bài hát thì mới thêm vào danh sách
                if (tracksForThisGenre != null && !tracksForThisGenre.isEmpty()) {
                    // Thêm một "Hàng" mới (gồm Tiêu đề và List nhạc)
                    newRows.add(new CategoryRow(genre, tracksForThisGenre));
                }
            }

            // 4. Cập nhật UI trên luồng chính
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
    public void onSongClick(ArrayList<Track> trackList, int position) {
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra("TRACK_LIST", trackList);
        intent.putExtra("TRACK_POSITION", position);
        startActivity(intent);
    }
}