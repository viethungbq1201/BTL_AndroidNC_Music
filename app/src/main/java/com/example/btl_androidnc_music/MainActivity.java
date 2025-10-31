package com.example.btl_androidnc_music;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

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