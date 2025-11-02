package com.example.btl_androidnc_music.service; // <-- THAY PACKAGE CỦA BẠN

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class MusicPlayerService extends MediaSessionService {

    private MediaSession mediaSession;
    private ExoPlayer exoPlayer;

    @Override
    public void onCreate() {
        super.onCreate();

        // Khởi tạo ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();

        // Khởi tạo MediaSession, liên kết nó với ExoPlayer
        mediaSession = new MediaSession.Builder(this, exoPlayer).build();

        // (Bạn có thể thêm các Listener ở đây nếu cần, ví dụ:)
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    // Xử lý khi nhạc kết thúc
                }
            }
        });
    }

    // Hàm này được gọi khi một Activity (như PlayerActivity) kết nối
    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    // Hàm này được gọi khi Service bị hủy
    @Override
    public void onDestroy() {
        // Giải phóng Session và Player
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        super.onDestroy();
    }
}