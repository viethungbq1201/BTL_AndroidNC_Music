package com.example.btl_androidnc_music.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.model.User;
import com.example.btl_androidnc_music.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // --- KHỞI TẠO DB ---
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        // --- THÊM LOGIC LƯU USER VÀO ROOM ---
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null && firebaseUser.getEmail() != null) {
                            // Chạy nền để lưu user vào Room
                            insertUserIntoDb(firebaseUser.getEmail(), () -> {
                                // Chỉ chuyển màn hình SAU KHI lưu DB xong
                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                finish(); // Quay lại LoginActivity
                            });
                        } else {
                            setLoading(false);
                            Toast.makeText(RegisterActivity.this, "Tạo user thành công nhưng không thể lấy email", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        setLoading(false);
                    }
                });
    }

    // Thêm user vào Room DB
    private void insertUserIntoDb(String email, Runnable onComplete) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.userDao().insertUser(new User(email));

            runOnUiThread(onComplete);
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnRegister.setEnabled(!isLoading);
    }
}