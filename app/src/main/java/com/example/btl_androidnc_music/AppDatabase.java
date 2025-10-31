package com.example.btl_androidnc_music;

import androidx.room.Database;
import androidx.room.RoomDatabase;

// Khai báo các entity (bảng) và phiên bản
@Database(entities = {Track.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Khai báo trừu tượng hàm DAO
    public abstract TrackDao trackDao(); // <-- Hàm này bạn đã gọi trong UploadActivity
}