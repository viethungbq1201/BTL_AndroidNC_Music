package com.example.btl_androidnc_music.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_androidnc_music.auth.AuthManager;
import com.example.btl_androidnc_music.databinding.ActivityLoginBinding; // <-- THAY TÊN PACKAGE CỦA BẠN

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = new AuthManager(this);

        // Xử lý nút Đăng Nhập
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show();
                return;
            }

            if (authManager.loginUser(email, password)) {
                // Đăng nhập thành công
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                // --- SỬA LOGIC ĐIỀU HƯỚNG Ở ĐÂY ---
                // Kiểm tra xem đã đồng ý điều khoản chưa
                if (authManager.hasAcceptedPolicy()) {
                    // Nếu rồi -> Vào thẳng Home
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                } else {
                    // Nếu chưa -> Vào màn hình Điều khoản
                    startActivity(new Intent(LoginActivity.this, PolicyActivity.class));
                }

                finish(); // Đóng LoginActivity trong mọi trường hợp
                // --- KẾT THÚC SỬA ---

            } else {
                // Đăng nhập thất bại
                Toast.makeText(this, "Sai email hoặc mật khẩu", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý nút chuyển qua Đăng Ký
        binding.tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void goToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish(); // Đóng LoginActivity
    }
}