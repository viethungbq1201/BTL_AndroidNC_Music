package com.example.btl_androidnc_music.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import androidx.room.Delete;

import com.example.btl_androidnc_music.data.model.Track;

@Dao // <-- Bắt buộc
public interface TrackDao {

    @Insert
    void insertTrack(Track track); // <-- Hàm này bạn đã gọi trong UploadActivity

    @Query("SELECT * FROM tracks")
    List<Track> getAllTracks(); // <-- Thêm hàm này để sau này dùng

    // Bạn có thể thêm các hàm khác như Delete, Update ở đây
    @Update // <-- THÊM MỚI
    void updateTrack(Track track);
    @Delete // <-- THÊM MỚI
    void deleteTrack(Track track);

    @Query("SELECT * FROM tracks WHERE isFavorite = 1") // <-- THÊM MỚI
    List<Track> getFavoriteTracks();

    @Query("SELECT * FROM tracks WHERE genre = :genre")
    List<Track> getTracksByGenre(String genre);

    @Query("SELECT DISTINCT genre FROM tracks WHERE genre IS NOT NULL AND genre != ''")
    List<String> getAllUniqueGenres();

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    List<Track> searchTracks(String query);

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    Track getTrackById(int trackId);
    // Thêm hàm này vào file TrackDao.java
}