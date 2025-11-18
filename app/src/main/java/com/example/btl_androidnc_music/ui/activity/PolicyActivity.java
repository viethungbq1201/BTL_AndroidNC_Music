package com.example.btl_androidnc_music.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_androidnc_music.R;
import com.example.btl_androidnc_music.auth.AuthManager;

public class PolicyActivity extends AppCompatActivity {

    private ScrollView scrollViewPolicy;
    private Button btnAccept, btnDecline;
    private AuthManager authManager;
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);

        authManager = new AuthManager(this);

        // Lấy UID từ Intent
        userUid = getIntent().getStringExtra("USER_UID");
        if (userUid == null || userUid.isEmpty()) {
            Toast.makeText(this, "Lỗi xác thực người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scrollViewPolicy = findViewById(R.id.scrollViewPolicy);
        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);

        // Ẩn nút Chấp nhận ban đầu
        btnAccept.setVisibility(View.INVISIBLE);

        // Lắng nghe sự kiện cuộn
        scrollViewPolicy.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (!scrollViewPolicy.canScrollVertically(1)) {
                btnAccept.setVisibility(View.VISIBLE);
            }
        });

        // Xử lý nút Từ chối
        btnDecline.setOnClickListener(v -> {
            finishAffinity();
        });

        // Xử lý nút Chấp nhận
        btnAccept.setOnClickListener(v -> {
            // 1. Lưu trạng thái cho đúng user UID
            authManager.setPolicyAccepted(userUid, true);

            // 2. Chuyển đến màn hình chính
            Intent intent = new Intent(PolicyActivity.this, HomeActivity.class);
            startActivity(intent);

            finish();
        });
    }
}