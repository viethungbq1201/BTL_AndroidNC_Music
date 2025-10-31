package com.example.btl_androidnc_music; // Thay package của bạn

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;

@Dao
public interface CommentDao {
    @Insert
    void insertComment(Comment comment);

    @Update
    void updateComment(Comment comment);

    // Lấy bình luận gốc (kèm thông tin user)
    @Transaction
    @Query("SELECT * FROM comments WHERE trackId = :trackId AND parentCommentId = 0 ORDER BY timestamp DESC")
    List<CommentWithUser> getParentCommentsForTrack(int trackId);

    // Lấy bình luận trả lời (kèm thông tin user)
    @Transaction
    @Query("SELECT * FROM comments WHERE parentCommentId = :parentCommentId ORDER BY timestamp ASC")
    List<CommentWithUser> getRepliesForComment(int parentCommentId);
}