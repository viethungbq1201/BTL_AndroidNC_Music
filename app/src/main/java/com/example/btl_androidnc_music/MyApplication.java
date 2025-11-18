package com.example.btl_androidnc_music;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Khởi tạo Firebase ngay khi ứng dụng bắt đầu
        FirebaseApp.initializeApp(this);
    }
}