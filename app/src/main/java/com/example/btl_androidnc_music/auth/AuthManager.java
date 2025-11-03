package com.example.btl_androidnc_music.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.example.btl_androidnc_music.service.MusicPlayerService;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AuthManager {

    private static final String PREFS_FILE = "auth_prefs_secure";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PASSWORD = "user_password";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LOGGED_IN_USER = "logged_in_user";

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
    public boolean createUser(String email, String password) {
        // Kiểm tra xem email (key) đã tồn tại chưa
        if (sharedPreferences.contains(email)) {
            // Đã tồn tại -> không thể tạo -> trả về false
            return false;
        }

        // Chưa tồn tại -> tạo mới
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(email, password); // Lưu email làm key, password làm value
        editor.apply();
        return true; // Tạo thành công
    }

    // Hàm Đăng Nhập: Kiểm tra email và mật khẩu
    public boolean loginUser(String email, String password) {
        String storedPassword = sharedPreferences.getString(email, null);
        if (storedPassword != null && storedPassword.equals(password)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.putString(KEY_LOGGED_IN_USER, email); // <-- Nó lưu email ở đây
            editor.apply();
            return true;
        }
        return false;
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
    public void logout(Context context) { // <-- Thêm Context
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_LOGGED_IN_USER);
        editor.apply();

        Intent intent = new Intent(context, MusicPlayerService.class);
        context.stopService(intent);
    }

    public String getLoggedInUsername() {
        return sharedPreferences.getString(KEY_LOGGED_IN_USER, null);
    }
     //Lưu trạng thái người dùng đã đồng ý điều khoản
     public void setPolicyAccepted(String username, boolean accepted) {
         SharedPreferences.Editor editor = sharedPreferences.edit();
         // Tạo một key duy nhất cho user, ví dụ: "user@gmail.com_policy_accepted"
         editor.putBoolean(username + "_policy_accepted", accepted);
         editor.apply();
     }

    /**
     * SỬA: Kiểm tra trạng thái đồng ý policy CỦA USER CỤ THỂ
     */
    public boolean hasAcceptedPolicy(String username) {
        // Đọc key duy nhất của user đó
        return sharedPreferences.getBoolean(username + "_policy_accepted", false);
    }
}