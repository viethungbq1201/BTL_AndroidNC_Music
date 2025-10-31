package com.example.btl_androidnc_music;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import com.example.btl_androidnc_music.databinding.FragmentSearchBinding; // Thay package của bạn

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private AppDatabase db;
    private SearchHistoryManager historyManager;

    // Adapters
    private TrackAdapter searchResultAdapter;
    private RecentSearchAdapter recentSearchAdapter;
    private CategoryGridAdapter categoryGridAdapter;

    // Lists
    private List<Track> searchResultsList = new ArrayList<>();
    private List<String> recentSearchesList = new ArrayList<>();
    private List<String> categoriesList = new ArrayList<>();

    private boolean isProgrammaticTextChange = false; // Flag để tránh TextWatcher

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);

        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();
        historyManager = new SearchHistoryManager(requireContext());

        setupRecyclerViews();
        loadDefaultData();
        setupSearchBox();

        return binding.getRoot();
    }

    private void setupRecyclerViews() {
        // 1. Kết quả tìm kiếm (dùng TrackAdapter)
        searchResultAdapter = new TrackAdapter(searchResultsList,

                // Click vào bài hát
                position -> {
                    // SỬA: Chỉ lưu lịch sử khi người dùng CHỌN 1 BÀI HÁT
                    String query = binding.etSearch.getText().toString().trim();
                    if (!query.isEmpty()) {
                        historyManager.saveSearchQuery(query);
                    }

                    // Mở PlayerActivity
                    Intent intent = new Intent(getActivity(), PlayerActivity.class);
                    intent.putExtra("TRACK_LIST", (ArrayList<Track>) searchResultsList);
                    intent.putExtra("TRACK_POSITION", position);
                    startActivity(intent);
                },

                // Nút "..." (Sửa/Xóa)
                new TrackAdapter.OnTrackOptionsClickListener() {
                    @Override
                    public void onEditClick(Track track) { /* Để trống */ }
                    @Override
                    public void onDeleteClick(Track track, int position) { /* Để trống */ }
                },

                false // Ẩn nút "..."
        );
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchResults.setAdapter(searchResultAdapter);

        // 2. Tìm kiếm gần đây (SỬA LẠI: Thêm listener cho Long Click)
        recentSearchAdapter = new RecentSearchAdapter(recentSearchesList,
                // Click: Điền vào ô tìm kiếm
                query -> {
                    binding.etSearch.setText(query);
                    binding.etSearch.setSelection(query.length()); // Di chuyển con trỏ về cuối
                },
                // Long Click (Nhấn giữ): Xóa
                (query, position) -> {
                    showDeleteHistoryDialog(query, position);
                }
        );
        binding.rvRecentSearches.setLayoutManager(new LinearLayoutManager(requireContext())); // Sửa lại layout
        binding.rvRecentSearches.setAdapter(recentSearchAdapter);

        // 3. Thể loại (Giữ nguyên logic cũ)
        categoryGridAdapter = new CategoryGridAdapter(categoriesList, category -> {
            isProgrammaticTextChange = true;
            binding.etSearch.setText(category);
            isProgrammaticTextChange = false;

            toggleLayout(true);
            performGenreSearch(category);
        });
        binding.rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvCategories.setAdapter(categoryGridAdapter);
    }

    // Tải dữ liệu cho "Gần đây" và "Thể loại"
    private void loadDefaultData() {
        // Tải lịch sử
        recentSearchesList.clear();
        recentSearchesList.addAll(historyManager.getSearchHistory());
        recentSearchAdapter.notifyDataSetChanged();

        // Tải thể loại (chạy nền)
        Executors.newSingleThreadExecutor().execute(() -> {
            List<String> genres = db.trackDao().getAllUniqueGenres();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    categoriesList.clear();
                    categoriesList.addAll(genres);
                    categoryGridAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    // Cài đặt listener cho ô tìm kiếm
    private void setupSearchBox() {
        // SỬA 1: Xử lý khi nhấn "Enter/Search" trên bàn phím
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    // 1. Lưu lịch sử
                    historyManager.saveSearchQuery(query);
                    // 2. Ẩn bàn phím
                    hideKeyboard();
                    // 3. (Hàm performNameSearch đã được gọi bởi TextWatcher rồi)
                }
                return true;
            }
            return false;
        });

        // SỬA 2: Cập nhật TextWatcher
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isProgrammaticTextChange) {
                    return;
                }

                String query = s.toString().trim();
                if (query.isEmpty()) {
                    // Nếu không nhập gì
                    toggleLayout(false);
                    // SỬA 3: Tải lại "Gần đây" để cập nhật list
                    loadDefaultData();
                } else {
                    // Nếu có nhập
                    toggleLayout(true);
                    performNameSearch(query); // Chỉ tìm, KHÔNG lưu lịch sử ở đây
                }
            }
        });
    }

    // Hàm chuyển đổi 2 layout
    private void toggleLayout(boolean showResults) {
        if (showResults) {
            binding.rvSearchResults.setVisibility(View.VISIBLE);
            binding.defaultSearchContainer.setVisibility(View.GONE);
        } else {
            binding.rvSearchResults.setVisibility(View.GONE);
            binding.defaultSearchContainer.setVisibility(View.VISIBLE);
        }
    }

    // SỬA: Hàm này giờ CHỈ tìm kiếm, không lưu lịch sử
    private void performNameSearch(String query) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Track> results = db.trackDao().searchTracks(query);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    searchResultsList.clear();
                    searchResultsList.addAll(results);
                    searchResultAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    // Hàm tìm theo thể loại (Giữ nguyên)
    private void performGenreSearch(String genre) {
        // Chạy trên luồng nền
        Executors.newSingleThreadExecutor().execute(() -> {

            // Gọi đúng hàm DAO: getTracksByGenre
            // Hàm này sẽ tìm chính xác thể loại (ví dụ: "123")
            List<Track> results = db.trackDao().getTracksByGenre(genre);

            if (getActivity() != null) {
                // Cập nhật UI trên luồng chính
                getActivity().runOnUiThread(() -> {
                    // Đổ dữ liệu vào cùng 1 danh sách kết quả
                    searchResultsList.clear();
                    searchResultsList.addAll(results);
                    searchResultAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    // --- THÊM CÁC HÀM HELPER MỚI ---

    // THÊM: Hiển thị dialog xác nhận xóa
    private void showDeleteHistoryDialog(String query, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa tìm kiếm")
                .setMessage("Bạn có muốn xóa '" + query + "' khỏi lịch sử?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    historyManager.deleteSearchQuery(query);
                    recentSearchesList.remove(position);
                    recentSearchAdapter.notifyItemRemoved(position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // THÊM: Ẩn bàn phím
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}