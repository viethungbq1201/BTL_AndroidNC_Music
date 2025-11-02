package com.example.btl_androidnc_music.data.db; // Thay package của bạn

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.btl_androidnc_music.data.model.User;

@Dao
public interface UserDao {
    // onConflict = OnConflictStrategy.IGNORE: Nếu đã có user, bỏ qua
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertUser(User user);

    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);
}