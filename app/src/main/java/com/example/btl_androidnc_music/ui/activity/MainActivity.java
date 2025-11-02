package com.example.btl_androidnc_music.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;

import com.example.btl_androidnc_music.auth.AuthManager;

public class MainActivity extends AppCompatActivity {

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = new AuthManager(this);

        // Quyết định xem đi đâu
        if (authManager.isLoggedIn()) {
            // Đã đăng nhập -> Vào Trang Chủ
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
        } else {
            // Chưa đăng nhập -> Vào Trang Đăng Nhập
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }

        finish(); // Đóng Activity này lại ngay lập tức
    }
}