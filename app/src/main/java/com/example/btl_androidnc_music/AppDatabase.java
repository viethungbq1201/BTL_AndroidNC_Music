package com.example.btl_androidnc_music;

import androidx.room.Database;
import androidx.room.RoomDatabase;

// Khai báo các entity (bảng) và phiên bản
@Database(entities = {Track.class, User.class, Comment.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TrackDao trackDao();
    public abstract CommentDao commentDao();
    public abstract UserDao userDao(); // <-- THÊM MỚI
}