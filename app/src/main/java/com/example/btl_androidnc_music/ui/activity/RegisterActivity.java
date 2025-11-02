package com.example.btl_androidnc_music.ui.activity; // Thay package của bạn

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.btl_androidnc_music.auth.AuthManager;
import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.model.User;
import com.example.btl_androidnc_music.databinding.ActivityRegisterBinding;

import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthManager authManager;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = new AuthManager(this);
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        // Xử lý nút Đăng Ký
        binding.btnRegister.setOnClickListener(v -> {
            registerUser(); // Gọi hàm đăng ký ở dưới
        });

        // Xử lý nút chuyển về Đăng Nhập
        binding.tvGoToLogin.setOnClickListener(v -> {
            finish();
        });
    }

    /**
     * Hàm này xử lý toàn bộ logic đăng ký
     */
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

        // --- SỬA LẠI LOGIC GỌI HÀM ---
        // Gọi hàm createUser() mới
        if (authManager.createUser(email, password)) {

            // 1. Tạo user trong Room DB (cho bình luận)
            User newUser = new User(email);
            Executors.newSingleThreadExecutor().execute(() -> {
                db.userDao().insertUser(newUser);
            });

            // 2. Thông báo và quay lại Login
            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // 3. Nếu createUser trả về false -> email đã tồn tại
            Toast.makeText(this, "Email này đã được sử dụng", Toast.LENGTH_SHORT).show();
        }
        // --- KẾT THÚC SỬA ---
    }
}