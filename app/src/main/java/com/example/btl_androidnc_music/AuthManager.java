package com.example.btl_androidnc_music;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class AuthManager {

    private static final String PREFS_FILE = "auth_prefs_secure";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PASSWORD = "user_password";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences sharedPreferences;

    // Khởi tạo trình quản lý
    public AuthManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    PREFS_FILE,  // Tham số 1: String (tên file)
                    masterKeyAlias, // Tham số 2: String (master key)
                    context,     // Tham số 3: Context
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm Đăng Ký: Lưu email và mật khẩu (đã mã hóa)
    public void registerUser(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD, password); // Mật khẩu tự động được mã hóa
        editor.apply();
    }

    // Hàm Đăng Nhập: Kiểm tra email và mật khẩu
    public boolean loginUser(String email, String password) {
        String savedEmail = sharedPreferences.getString(KEY_EMAIL, null);
        String savedPassword = sharedPreferences.getString(KEY_PASSWORD, null);

        // So sánh
        return email.equals(savedEmail) && password.equals(savedPassword);
    }

    // Lưu trạng thái đăng nhập
    public void setLoggedIn(boolean isLoggedIn) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }

    // Kiểm tra xem user đã đăng nhập chưa
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Đăng xuất
    public void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        // Bạn có thể chọn xóa cả email/pass nếu muốn
        // editor.remove(KEY_EMAIL);
        // editor.remove(KEY_PASSWORD);
        editor.apply();
    }
}