package com.example.btl_androidnc_music; // Thay package của bạn

import androidx.room.Embedded;
import androidx.room.Relation;

public class CommentWithUser {
    @Embedded
    public Comment comment;

    @Relation(
            parentColumn = "username", // Cột trong Comment
            entityColumn = "username"  // Cột trong User
    )
    public User user;
}