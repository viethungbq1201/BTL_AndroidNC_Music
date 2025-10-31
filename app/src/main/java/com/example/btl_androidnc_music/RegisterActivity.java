package com.example.btl_androidnc_music;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.btl_androidnc_music.databinding.ActivityRegisterBinding; // <-- THAY TÊN PACKAGE CỦA BẠN

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = new AuthManager(this);

        // Xử lý nút Đăng Ký
        binding.btnRegister.setOnClickListener(v -> {
            registerUser();
        });

        // Xử lý nút chuyển về Đăng Nhập
        binding.tvGoToLogin.setOnClickListener(v -> {
            finish(); // Đóng màn hình này để quay lại màn hình Login
        });
    }

    private void registerUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lưu tài khoản
        authManager.registerUser(email, password);

        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
        finish(); // Đóng RegisterActivity và quay lại LoginActivity
    }
}