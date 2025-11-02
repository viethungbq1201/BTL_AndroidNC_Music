package com.example.btl_androidnc_music.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.btl_androidnc_music.auth.AuthManager;
import com.example.btl_androidnc_music.databinding.FragmentManageBinding; // Import binding
import com.example.btl_androidnc_music.ui.activity.FavoritesActivity;
import com.example.btl_androidnc_music.ui.activity.LoginActivity;
import com.example.btl_androidnc_music.ui.activity.UploadActivity;
import com.example.btl_androidnc_music.ui.activity.UploadedMusicActivity;

public class ManageFragment extends Fragment {

    private FragmentManageBinding binding;
    private AuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentManageBinding.inflate(inflater, container, false);
        authManager = new AuthManager(requireContext());

        // Xử lý Đăng xuất (giống hệt code cũ)
        binding.btnLogout.setOnClickListener(v -> {
            // 1. Gọi hàm logout mới và truyền Context
            authManager.logout(requireContext());

            // 2. Quay về màn hình Login
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            // Cờ này sẽ xóa tất cả các Activity cũ (Home, Player)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            getActivity().finish(); // Đóng HomeActivity
        });

        // Xử lý khi nhấn vào "Tải nhạc lên"
        binding.tvUploadMusic.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UploadActivity.class);
            startActivity(intent);
        });

        // Xử lý khi nhấn vào "Nhạc đã tải lên"
        binding.tvUploadedMusic.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UploadedMusicActivity.class); // <-- SỬA Ở ĐÂY
            startActivity(intent);
        });

        binding.tvFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FavoritesActivity.class);
            startActivity(intent);
        });

        return binding.getRoot();
    }
}