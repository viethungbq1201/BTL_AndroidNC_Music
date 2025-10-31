package com.example.btl_androidnc_music; // Thay package của bạn

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "comments",
        foreignKeys = {
                @ForeignKey(entity = Track.class,
                        parentColumns = "id",
                        childColumns = "trackId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class,
                        parentColumns = "username",  // Liên kết với cột 'username' của User
                        childColumns = "username",   // Bằng cột 'username' của Comment
                        onDelete = ForeignKey.CASCADE)
        })
public class Comment {
    @PrimaryKey(autoGenerate = true)
    public int commentId;

    public int trackId; // Liên kết với bài hát

    @NonNull
    public String username; // Liên kết với người bình luận

    public String content; // Nội dung bình luận
    public long timestamp; // Thời gian đăng
    public int likeCount = 0; // Số lượt tim
    public int parentCommentId = 0; // 0 = bình luận gốc, >0 = trả lời
}