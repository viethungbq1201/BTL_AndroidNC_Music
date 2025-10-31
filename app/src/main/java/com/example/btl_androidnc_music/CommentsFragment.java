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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommentsBinding.inflate(inflater, container, false);

        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        authManager = new AuthManager(requireContext());
        currentTrack = MusicPlayerManager.getInstance(requireContext()).getCurrentTrack();

        setupRecyclerView();
        setupSendButton();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật trạng thái đăng nhập và tải bình luận mỗi khi tab này được hiển thị
        checkLoginState();
        loadComments();
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

    private void loadComments() {
        if (currentTrack == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            List<CommentWithUser> parentComments = db.commentDao().getParentCommentsForTrack(currentTrack.id);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    commentList.clear();
                    commentList.addAll(parentComments);
                    adapter.notifyDataSetChanged();
                });
            }
        });
    }
}