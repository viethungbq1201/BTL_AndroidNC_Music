package com.example.btl_androidnc_music;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {

    @PrimaryKey
    @NonNull
    public String username; // Dùng email làm khóa chính

    public User(@NonNull String username) {
        this.username = username;
    }
}