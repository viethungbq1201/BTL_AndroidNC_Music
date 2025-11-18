package com.example.btl_androidnc_music.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.example.btl_androidnc_music.service.MusicPlayerService;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AuthManager {

    private static final String PREFS_FILE = "user_prefs_secure";
    private SharedPreferences sharedPreferences;

    public AuthManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    PREFS_FILE,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Fallback nếu mã hóa lỗi
            sharedPreferences = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        }
    }

    // Hàm Đăng xuất
    public void logout(Context context) {
        // 1. Đăng xuất khỏi Firebase
        FirebaseAuth.getInstance().signOut();

        // 2. Dừng Service nhạc
        Intent intent = new Intent(context, MusicPlayerService.class);
        context.stopService(intent);
    }

    // Hàm lưu Policy, dùng UID của Firebase làm key
    public void setPolicyAccepted(String uid, boolean accepted) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Lưu key theo dạng "UID_policy_accepted"
        editor.putBoolean(uid + "_policy_accepted", accepted);
        editor.apply();
    }

    // Hàm kiểm tra Policy, dùng UID của Firebase
    public boolean hasAcceptedPolicy(String uid) {
        if (uid == null) {
            return false;
        }
        return sharedPreferences.getBoolean(uid + "_policy_accepted", false);
    }
}