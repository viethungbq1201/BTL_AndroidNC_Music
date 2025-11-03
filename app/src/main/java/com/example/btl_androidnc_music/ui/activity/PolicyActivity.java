package com.example.btl_androidnc_music.ui.activity; // Thay package của bạn

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
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);

        authManager = new AuthManager(this);

        userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null || userEmail.isEmpty()) {
            // Nếu không có email (lỗi), không cho đồng ý
            Toast.makeText(this, "Lỗi xác thực người dùng", Toast.LENGTH_SHORT).show();
            finish(); // Đóng lại
            return;
        }

        scrollViewPolicy = findViewById(R.id.scrollViewPolicy);
        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);

        // Ẩn nút Chấp nhận ban đầu
        btnAccept.setVisibility(View.INVISIBLE);

        // Lắng nghe sự kiện cuộn
        scrollViewPolicy.getViewTreeObserver().addOnScrollChangedListener(() -> {
            // Kiểm tra xem đã cuộn đến cuối chưa
            if (!scrollViewPolicy.canScrollVertically(1)) {
                // Đã ở dưới cùng -> Hiện nút Chấp nhận
                btnAccept.setVisibility(View.VISIBLE);
            }
        });

        // Xử lý nút Từ chối
        btnDecline.setOnClickListener(v -> {
            // Thoát hoàn toàn ứng dụng
            finishAffinity();
        });

        // Xử lý nút Chấp nhận
        btnAccept.setOnClickListener(v -> {
            // 1. SỬA: Lưu trạng thái cho đúng user
            authManager.setPolicyAccepted(userEmail, true);

            // 2. Chuyển đến màn hình chính
            Intent intent = new Intent(PolicyActivity.this, HomeActivity.class);
            startActivity(intent);

            // 3. Đóng màn hình này lại
            finish();
        });
    }
}