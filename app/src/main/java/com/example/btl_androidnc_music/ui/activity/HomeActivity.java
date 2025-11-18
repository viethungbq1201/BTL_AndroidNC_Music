package com.example.btl_androidnc_music.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.room.Room;

import com.example.btl_androidnc_music.R;
import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.model.Track;
import com.example.btl_androidnc_music.databinding.ActivityHomeBinding;
import com.example.btl_androidnc_music.service.MusicPlayerService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;
    private Player.Listener playerListener;
    private AppDatabase db;
    private Track mCurrentTrack;

    private View miniPlayerContainer;
    private ImageButton btnMiniPlayPause, btnMiniFavorite, btnMiniNext;
    private TextView tvMiniTitle, tvMiniArtist;
    private ImageView ivMiniCoverArt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        miniPlayerContainer = binding.miniPlayerContainer;
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniFavorite = findViewById(R.id.btnMiniFavorite);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        ivMiniCoverArt = findViewById(R.id.ivMiniCoverArt);

        setupMiniPlayerClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectToService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnectFromService();
    }

    private void connectToService() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicPlayerService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                setupMediaControllerListener();
                updateMiniPlayerUi();
            } catch (ExecutionException | InterruptedException e) {
                // Lỗi
            }
        }, MoreExecutors.directExecutor());
    }

    private void disconnectFromService() {
        if (mediaController != null && playerListener != null) {
            mediaController.removeListener(playerListener);
        }
        MediaController.releaseFuture(controllerFuture);
        mediaController = null;
    }

    private void setupMediaControllerListener() {
        playerListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    miniPlayerContainer.setVisibility(View.GONE);
                } else {
                    miniPlayerContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                updateMiniPlayerUi();
            }
        };
        mediaController.addListener(playerListener);
    }

    private void updateMiniPlayerUi() {
        if (mediaController == null || mediaController.getCurrentMediaItem() == null) {
            miniPlayerContainer.setVisibility(View.GONE);
            return;
        }

        MediaItem currentItem = mediaController.getCurrentMediaItem();
        String mediaId = currentItem.mediaId;
        if (mediaId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int trackId = Integer.parseInt(mediaId);
                mCurrentTrack = db.trackDao().getTrackById(trackId);

                if (mCurrentTrack != null) {
                    runOnUiThread(() -> {
                        tvMiniTitle.setText(mCurrentTrack.title);
                        tvMiniArtist.setText(mCurrentTrack.artist);

                        // Dùng file cục bộ
                        if (ivMiniCoverArt != null && mCurrentTrack.imagePath != null && !mCurrentTrack.imagePath.isEmpty()) {
                            ivMiniCoverArt.setImageURI(Uri.fromFile(new File(mCurrentTrack.imagePath)));
                        } else if (ivMiniCoverArt != null) {
                            ivMiniCoverArt.setImageResource(R.drawable.ic_music_note);
                        }

                        btnMiniFavorite.setImageResource(mCurrentTrack.isFavorite ?
                                R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
                        btnMiniPlayPause.setImageResource(mediaController.isPlaying() ?
                                R.drawable.ic_pause : R.drawable.ic_play);
                        miniPlayerContainer.setVisibility(View.VISIBLE);
                    });
                }
            } catch (NumberFormatException e) {
                // ID không hợp lệ
            }
        });
    }

    private void setupMiniPlayerClickListeners() {
        miniPlayerContainer.setOnClickListener(v -> {
            startActivity(new Intent(this, PlayerActivity.class));
        });

        btnMiniPlayPause.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });

        btnMiniNext.setOnClickListener(v -> {
            if (mediaController != null && mediaController.hasNextMediaItem()) {
                mediaController.seekToNextMediaItem();
            } else if (mediaController != null) {
                mediaController.seekTo(0, 0);
            }
        });

        // Click nút Favorite
        btnMiniFavorite.setOnClickListener(v -> {
            if (mCurrentTrack != null) {
                // (Logic này vẫn OK vì 'isFavorite' vẫn còn trong model Track)
                mCurrentTrack.isFavorite = !mCurrentTrack.isFavorite;
                btnMiniFavorite.setImageResource(mCurrentTrack.isFavorite ?
                        R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);

                Executors.newSingleThreadExecutor().execute(() -> {
                    db.trackDao().updateTrack(mCurrentTrack);
                });
            }
        });
    }
}