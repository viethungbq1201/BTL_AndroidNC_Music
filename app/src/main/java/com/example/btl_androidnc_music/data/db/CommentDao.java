package com.example.btl_androidnc_music.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.btl_androidnc_music.data.model.Comment;
import com.example.btl_androidnc_music.data.model.CommentWithUser;

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
    @Query("SELECT * FROM comments WHERE parentId = :parentCommentId ORDER BY timestamp DESC")
    List<CommentWithUser> getRepliesForComment(int parentCommentId);
    @Transaction
    @Query("SELECT * FROM comments WHERE trackId = :trackId AND parentId IS NULL ORDER BY timestamp DESC")
    List<CommentWithUser> getCommentsForTrack(int trackId);

}