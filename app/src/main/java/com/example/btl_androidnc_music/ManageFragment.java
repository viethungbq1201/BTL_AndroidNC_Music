package com.example.btl_androidnc_music;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.btl_androidnc_music.databinding.FragmentManageBinding; // Import binding

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
            authManager.logout();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
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