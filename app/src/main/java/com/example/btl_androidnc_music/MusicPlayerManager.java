package com.example.btl_androidnc_music;

import android.content.Context;
import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Lớp này sẽ lưu trữ và quản lý ExoPlayer
public class MusicPlayerManager {

    private static MusicPlayerManager instance;
    private ExoPlayer exoPlayer;
    private List<Track> currentPlaylist;

    // Hàm khởi tạo (private để đảm bảo là Singleton)
    private MusicPlayerManager(Context context) {
        exoPlayer = new ExoPlayer.Builder(context).build();
    }

    // Hàm để lấy Manager
    public static synchronized MusicPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayerManager(context.getApplicationContext());
        }
        return instance;
    }

    // Lấy trình phát nhạc
    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    // Bắt đầu phát một danh sách mới
    public void play(List<Track> trackList, int startPosition) {
        this.currentPlaylist = trackList;

        List<MediaItem> mediaItems = new ArrayList<>();
        for (Track track : trackList) {
            mediaItems.add(MediaItem.fromUri(Uri.fromFile(new File(track.filePath))));
        }

        exoPlayer.setMediaItems(mediaItems);
        exoPlayer.seekTo(startPosition, 0);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    // Lấy bài hát hiện tại
    public Track getCurrentTrack() {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            return null;
        }
        return currentPlaylist.get(exoPlayer.getCurrentMediaItemIndex());
    }

    // Giải phóng trình phát nhạc (khi app tắt hẳn)
    public void stopAndRelease() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.clearMediaItems();
            exoPlayer.release();
            exoPlayer = null;
        }
        currentPlaylist = null;
        instance = null; // Hủy instance để lần đăng nhập sau tạo mới
    }
}