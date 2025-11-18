package com.example.btl_androidnc_music.ui.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

// XÓA: import com.example.btl_androidnc_music.auth.AuthManager;
import com.google.firebase.auth.FirebaseAuth; // THÊM MỚI
import com.google.firebase.auth.FirebaseUser; // THÊM MỚI

public class MainActivity extends AppCompatActivity {

    // XÓA: private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SỬA: Kiểm tra người dùng Firebase hiện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Đã đăng nhập -> Vào Trang Chủ
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
        } else {
            // Chưa đăng nhập -> Vào Trang Đăng Nhập
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }

        finish(); // Đóng Activity này lại ngay lập tức
    }
}