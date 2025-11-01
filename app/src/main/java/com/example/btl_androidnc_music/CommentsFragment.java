package com.example.btl_androidnc_music; // Thay package của bạn

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import com.example.btl_androidnc_music.databinding.FragmentCommentsBinding;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class CommentsFragment extends Fragment {

    private FragmentCommentsBinding binding;
    private AppDatabase db;
    private CommentAdapter adapter;
    private List<CommentWithUser> commentList = new ArrayList<>(); // <-- Sửa kiểu List
    private Track currentTrack;
    private AuthManager authManager; // <-- Thêm

    private int replyingToCommentId = 0; // 0 = bình luận mới, > 0 = trả lời
    private String replyingToUsername = "";
    private ExoPlayer exoPlayer; // <-- THÊM BIẾN NÀY
    private Player.Listener playerListener; // <-- THÊM BIẾN NÀY
    private boolean isViewCreated = false; // Cờ kiểm tra View

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommentsBinding.inflate(inflater, container, false);

        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        authManager = new AuthManager(requireContext());
        exoPlayer = MusicPlayerManager.getInstance(requireContext()).getExoPlayer();
        currentTrack = MusicPlayerManager.getInstance(requireContext()).getCurrentTrack();

        setupRecyclerView();
        setupSendButton();
        setupPlayerListener(); // <-- THÊM MỚI

        isViewCreated = true; // Đánh dấu là View đã được tạo

        return binding.getRoot();
    }

    private void checkLoginState() {
        if (!authManager.isLoggedIn()) {
            binding.etCommentInput.setHint("Vui lòng đăng nhập để bình luận");
            binding.etCommentInput.setEnabled(false);
            binding.btnSendComment.setEnabled(false);
        } else {
            binding.etCommentInput.setHint("Viết bình luận...");
            binding.etCommentInput.setEnabled(true);
            binding.btnSendComment.setEnabled(true);
        }
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter(commentList, db.commentDao(),
                // Xử lý khi nhấn nút "Trả lời"
                (commentWithUser) -> {
                    replyingToCommentId = commentWithUser.comment.commentId;
                    replyingToUsername = commentWithUser.user.username;
                    binding.etCommentInput.setHint("Trả lời " + replyingToUsername + "...");
                    binding.etCommentInput.requestFocus();
                    // Hiện bàn phím
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(binding.etCommentInput, InputMethodManager.SHOW_IMPLICIT);
                }
        );
        binding.rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvComments.setAdapter(adapter);
    }

    private void setupSendButton() {
        binding.btnSendComment.setOnClickListener(v -> {
            String content = binding.etCommentInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Bình luận không thể trống", Toast.LENGTH_SHORT).show();
                return;
            }

            String loggedInUsername = authManager.getLoggedInUsername();
            if (currentTrack == null || loggedInUsername == null) {
                Toast.makeText(requireContext(), "Lỗi: Cần đăng nhập", Toast.LENGTH_SHORT).show();
                checkLoginState();
                return;
            }

            Comment newComment = new Comment();
            newComment.trackId = currentTrack.id;
            newComment.username = loggedInUsername;
            newComment.content = content;
            newComment.timestamp = new Date().getTime();
            newComment.parentCommentId = replyingToCommentId;

            // Lưu vào DB (chạy nền)
            Executors.newSingleThreadExecutor().execute(() -> {
                db.commentDao().insertComment(newComment);

                // Sau khi lưu, tải lại toàn bộ bình luận
                loadComments();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.etCommentInput.setText("");
                        binding.etCommentInput.setHint("Viết bình luận...");
                        replyingToCommentId = 0;
                        replyingToUsername = "";
                        // Ẩn bàn phím
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(binding.etCommentInput.getWindowToken(), 0);
                    });
                }
            });
        });
    }

    private void setupPlayerListener() {
        if (exoPlayer == null) return;

        playerListener = new Player.Listener() {
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                // Khi bài hát được chuyển, lấy lại track mới
                currentTrack = MusicPlayerManager.getInstance(requireContext()).getCurrentTrack();
                // Tải lại bình luận
                loadComments();
            }
        };
        exoPlayer.addListener(playerListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLoginState();

        // --- SỬA LẠI CHÚT ---
        // Cập nhật lại currentTrack và tải comment
        // (Phòng trường hợp bài hát đã chuyển khi tab này bị ẩn)
        currentTrack = MusicPlayerManager.getInstance(requireContext()).getCurrentTrack();
        loadComments();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false; // Đặt lại cờ

        // Gỡ listener để tránh crash (zombie listener)
        if (exoPlayer != null && playerListener != null) {
            exoPlayer.removeListener(playerListener);
        }
        binding = null; // Tránh memory leak
    }

    private void loadComments() {
        // --- THÊM 2 DÒNG KIỂM TRA NÀY ---
        // Nếu View chưa được tạo (binding=null) hoặc bài hát là null, thì dừng
        if (!isViewCreated || currentTrack == null) {
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            List<CommentWithUser> parentComments = db.commentDao().getParentCommentsForTrack(currentTrack.id);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Kiểm tra binding một lần nữa
                    if (binding != null) {
                        commentList.clear();
                        commentList.addAll(parentComments);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }
}