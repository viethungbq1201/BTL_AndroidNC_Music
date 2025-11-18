package com.example.btl_androidnc_music.ui.fragment;

import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ManageFragment extends Fragment {

    private FragmentManageBinding binding;
    private AuthManager authManager;
    private FirebaseAuth mAuth;

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
        mAuth = FirebaseAuth.getInstance();

        // Tải thông tin người dùng
        loadUserProfile();

        // Cài đặt sự kiện click cho các menu
        binding.tvFavorites.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), FavoritesActivity.class));
        });

        binding.tvUploadedMusic.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), UploadedMusicActivity.class));
        });

        binding.tvUploadMusic.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), UploadActivity.class));
        });

        // Nút Đăng xuất
        binding.btnLogout.setOnClickListener(v -> {
            authManager.logout(requireContext()); // Hàm này giờ sẽ đăng xuất Firebase
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        binding.btnManageSettings.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chức năng Cài đặt chưa được phát triển", Toast.LENGTH_SHORT).show();
        });
    }

    // Tải thông tin user từ Firebase
    private void loadUserProfile() {
        if (mAuth == null || binding == null) return;

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String email = user.getEmail();
            String displayName = user.getDisplayName(); // Lấy tên từ Google
            Uri photoUrl = user.getPhotoUrl(); // Lấy ảnh avatar từ Google

            if (displayName != null && !displayName.isEmpty()) {
                binding.tvManageName.setText(displayName);
            } else if (email != null && !email.isEmpty()) {
                // Nếu không có tên, dùng tạm email
                binding.tvManageName.setText(email.split("@")[0]);
            } else {
                binding.tvManageName.setText("Người dùng");
            }

            binding.tvManageEmail.setText(email);
            binding.ivManageAvatar.setImageResource(R.drawable.ic_default_avatar);

        } else {
            // Trường hợp lỗi (không có user)
            binding.tvManageName.setText("Khách");
            binding.tvManageEmail.setText("Chưa đăng nhập");
            binding.ivManageAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
    }

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