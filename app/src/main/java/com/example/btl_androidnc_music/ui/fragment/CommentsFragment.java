package com.example.btl_androidnc_music.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import com.example.btl_androidnc_music.auth.AuthManager;
import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.db.CommentDao;
import com.example.btl_androidnc_music.data.model.Comment;
import com.example.btl_androidnc_music.data.model.CommentWithUser;
import com.example.btl_androidnc_music.databinding.FragmentCommentsBinding;
import com.example.btl_androidnc_music.ui.activity.PlayerActivity;
import com.example.btl_androidnc_music.ui.adapter.CommentAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class CommentsFragment extends Fragment implements CommentAdapter.OnReplyClickListener {

    private FragmentCommentsBinding binding;
    private AppDatabase db;
    private CommentDao commentDao;
    private CommentAdapter commentAdapter;
    private List<CommentWithUser> commentList = new ArrayList<>();
    private MediaController mediaController;
    private Player.Listener playerListener;

    private AuthManager authManager;
    private FirebaseAuth mAuth;

    private int currentTrackId = -1;
    private CommentWithUser replyingToComment = null;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommentsBinding.inflate(inflater, container, false);

        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();
        commentDao = db.commentDao();

        mAuth = FirebaseAuth.getInstance(); // <-- KHỞI TẠO FIREBASE AUTH
        authManager = new AuthManager(requireContext());

        setupRecyclerView();
        setupSendButton();

        if (getActivity() instanceof PlayerActivity) {
            mediaController = ((PlayerActivity) getActivity()).getServiceMediaController();
            if (mediaController != null) {
                setupPlayerListener();
                updateCommentsForCurrentTrack();
            }
        }

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(commentList, commentDao, this);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvComments.setAdapter(commentAdapter);
    }

    private void setupSendButton() {
        binding.btnSendComment.setOnClickListener(v -> {
            String content = binding.etCommentInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(getContext(), "Bình luận không thể trống", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lấy user từ Firebase Auth
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null || currentUser.getEmail() == null) {
                Toast.makeText(getContext(), "Bạn cần đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
                return;
            }

            String loggedInUsername = currentUser.getEmail();

            if (currentTrackId == -1) {
                Toast.makeText(getContext(), "Không thể tải ID bài hát", Toast.LENGTH_SHORT).show();
                return;
            }

            Comment newComment = new Comment();
            newComment.trackId = currentTrackId;
            newComment.username = loggedInUsername;
            newComment.content = content;
            newComment.timestamp = System.currentTimeMillis();

            // Kiểm tra xem đây là trả lời hay bình luận mới
            if (replyingToComment != null) {
                newComment.parentId = replyingToComment.comment.commentId;
                binding.tvReplyingTo.setVisibility(View.GONE); // Ẩn thanh "đang trả lời"
                replyingToComment = null; // Reset
            }

            // Lưu vào DB
            Executors.newSingleThreadExecutor().execute(() -> {
                commentDao.insertComment(newComment);
                // Tải lại bình luận
                loadComments(currentTrackId);
                // Xóa input
                handler.post(() -> binding.etCommentInput.setText(""));
            });
        });
    }

    // Hàm này được gọi khi nhấn "trả lời"
    @Override
    public void onReplyClick(CommentWithUser parentComment) {
        replyingToComment = parentComment;
        String replyToName = parentComment.user != null ? parentComment.user.username.split("@")[0] : parentComment.comment.username.split("@")[0];
        binding.tvReplyingTo.setText("Đang trả lời: " + replyToName);
        binding.tvReplyingTo.setVisibility(View.VISIBLE);
        binding.etCommentInput.requestFocus();
    }

    // Lắng nghe service để biết khi nào chuyển bài
    private void setupPlayerListener() {
        playerListener = new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                updateCommentsForCurrentTrack();
            }
        };
        mediaController.addListener(playerListener);
    }

    private void updateCommentsForCurrentTrack() {
        if (mediaController != null && mediaController.getCurrentMediaItem() != null) {
            String mediaId = mediaController.getCurrentMediaItem().mediaId;
            try {
                currentTrackId = Integer.parseInt(mediaId);
                loadComments(currentTrackId);
            } catch (NumberFormatException e) {
                currentTrackId = -1;
            }
        }
    }

    // Tải bình luận từ Room DB
    private void loadComments(int trackId) {
        if (trackId == -1) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            List<CommentWithUser> newComments = commentDao.getCommentsForTrack(trackId);
            handler.post(() -> {
                commentList.clear();
                commentList.addAll(newComments);
                commentAdapter.notifyDataSetChanged();
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaController != null && playerListener != null) {
            mediaController.removeListener(playerListener);
        }
        binding = null;
        handler.removeCallbacksAndMessages(null);
    }
}