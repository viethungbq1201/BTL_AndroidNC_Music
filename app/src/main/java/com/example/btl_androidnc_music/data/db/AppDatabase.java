package com.example.btl_androidnc_music.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.btl_androidnc_music.data.model.Comment;
import com.example.btl_androidnc_music.data.model.Track;
import com.example.btl_androidnc_music.data.model.User;

// Khai báo các entity (bảng) và phiên bản
@Database(entities = {Track.class, User.class, Comment.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TrackDao trackDao();
    public abstract CommentDao commentDao();
    public abstract UserDao userDao(); // <-- THÊM MỚI
}