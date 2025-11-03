package com.example.btl_androidnc_music.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.btl_androidnc_music.R;
import com.example.btl_androidnc_music.auth.AuthManager;
import com.example.btl_androidnc_music.databinding.FragmentManageBinding;
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
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authManager = new AuthManager(requireContext());

        // Tải thông tin người dùng
        loadUserProfile();

        // Cài đặt sự kiện click cho các menu

        // Nút Yêu thích
        binding.tvFavorites.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), FavoritesActivity.class));
        });

        // Nút Nhạc đã tải lên (Dùng ID từ layout cũ của bạn)
        binding.tvUploadedMusic.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), UploadedMusicActivity.class));
        });

        // Nút Tải nhạc lên (Dùng ID từ layout cũ của bạn)
        binding.tvUploadMusic.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), UploadActivity.class));
        });

        // Nút Đăng xuất
        binding.btnLogout.setOnClickListener(v -> {
            authManager.logout(requireContext());
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        // (Tùy chọn: Thêm sự kiện click cho nút Cài đặt)
        binding.btnManageSettings.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chức năng Cài đặt chưa được phát triển", Toast.LENGTH_SHORT).show();
        });
    }

    // Tải thông tin user
    private void loadUserProfile() {
        if (authManager == null || binding == null) return;

        String email = authManager.getLoggedInUsername();

        if (email != null && !email.isEmpty()) {
            // Tách email để lấy tên (ví dụ: "abc@gmail.com" -> "abc")
            String displayName = email.split("@")[0];

            // Cập nhật giao diện
            binding.tvManageName.setText(displayName);
            binding.tvManageEmail.setText(email);
        } else {
            // Trường hợp lỗi
            binding.tvManageName.setText("Khách");
            binding.tvManageEmail.setText("Không có thông tin");
        }
    }

    // Cập nhật thông tin khi quay lại tab này
    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}