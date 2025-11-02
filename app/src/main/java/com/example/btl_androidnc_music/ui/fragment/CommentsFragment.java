package com.example.btl_androidnc_music.ui.fragment; // Thay package của bạn

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
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController; // <-- SỬA

import com.example.btl_androidnc_music.auth.AuthManager;
import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.model.Comment;
import com.example.btl_androidnc_music.data.model.CommentWithUser;
import com.example.btl_androidnc_music.data.model.Track;
import com.example.btl_androidnc_music.databinding.FragmentCommentsBinding;
import com.example.btl_androidnc_music.ui.activity.PlayerActivity;
import com.example.btl_androidnc_music.ui.adapter.CommentAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class CommentsFragment extends Fragment {

    private FragmentCommentsBinding binding;
    private AppDatabase db;
    private CommentAdapter adapter;
    private List<CommentWithUser> commentList = new ArrayList<>();
    private Track currentTrack;
    private AuthManager authManager;

    // --- SỬA: Dùng MediaController thay vì Manager ---
    private MediaController mediaController;
    private Player.Listener playerListener;
    private boolean isViewCreated = false;
    // --- KẾT THÚC SỬA ---

    private int replyingToCommentId = 0;
    private String replyingToUsername = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommentsBinding.inflate(inflater, container, false);

        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        authManager = new AuthManager(requireContext());

        // --- SỬA LẠI KHỐI NÀY ---
        if (getActivity() != null) {
            mediaController = ((PlayerActivity) getActivity()).getServiceMediaController();
        }
        // --- KẾT THÚC SỬA ---

        setupRecyclerView();
        setupSendButton();
        setupPlayerListener();

        isViewCreated = true;

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLoginState();
        updateCurrentTrackAndLoadComments(); // Cập nhật khi tab được hiển thị
    }

    // --- THÊM HÀM MỚI ---
    // Lắng nghe sự kiện chuyển bài
    private void setupPlayerListener() {
        if (mediaController == null) return;

        playerListener = new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                // Khi bài hát được chuyển, tải lại bình luận
                updateCurrentTrackAndLoadComments();
            }
        };
        mediaController.addListener(playerListener);
    }

    // --- THÊM HÀM MỚI ---
    // Hàm trung gian để lấy Track ID và tải comment
    private void updateCurrentTrackAndLoadComments() {
        if (mediaController == null || !isAdded()) {
            loadComments(null); // Xóa comment nếu không có media
            return;
        }

        MediaItem currentItem = mediaController.getCurrentMediaItem();
        if (currentItem == null || currentItem.mediaId == null) {
            loadComments(null); // Xóa comment
            return;
        }

        try {
            int trackId = Integer.parseInt(currentItem.mediaId);
            // Lấy Track từ DB (chạy nền) và sau đó tải comment
            Executors.newSingleThreadExecutor().execute(() -> {
                currentTrack = db.trackDao().getTrackById(trackId);
                // Sau khi lấy được track, gọi loadComments
                loadComments(currentTrack);
            });
        } catch (NumberFormatException e) {
            loadComments(null); // ID lỗi
        }
    }

    // SỬA: Hàm này nhận vào Track (hoặc null)
    private void loadComments(@Nullable Track track) {
        if (!isViewCreated) {
            return;
        }

        if (track == null) {
            // Nếu không có bài hát, xóa list
            if(getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        commentList.clear();
                        adapter.notifyDataSetChanged();
                    }
                });
            }
            return;
        }

        // Nếu có bài hát, tải comment cho bài đó
        Executors.newSingleThreadExecutor().execute(() -> {
            List<CommentWithUser> parentComments = db.commentDao().getParentCommentsForTrack(track.id);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        commentList.clear();
                        commentList.addAll(parentComments);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    // --- SỬA: Hàm này cần `currentTrack` phải được cập nhật ---
    private void setupSendButton() {
        binding.btnSendComment.setOnClickListener(v -> {
            String content = binding.etCommentInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Bình luận không thể trống", Toast.LENGTH_SHORT).show();
                return;
            }

            String loggedInUsername = authManager.getLoggedInUsername();
            // SỬA: Kiểm tra currentTrack
            if (currentTrack == null || loggedInUsername == null) {
                Toast.makeText(requireContext(), "Lỗi: Cần đăng nhập hoặc bài hát không hợp lệ", Toast.LENGTH_SHORT).show();
                checkLoginState();
                return;
            }

            Comment newComment = new Comment();
            newComment.trackId = currentTrack.id;
            newComment.username = loggedInUsername;
            newComment.content = content;
            newComment.timestamp = new Date().getTime();
            newComment.parentCommentId = replyingToCommentId;

            Executors.newSingleThreadExecutor().execute(() -> {
                db.commentDao().insertComment(newComment);
                loadComments(currentTrack); // Tải lại

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

    // --- SỬA: Cập nhật hàm reply (lấy tên user) ---
    private void setupRecyclerView() {
        adapter = new CommentAdapter(commentList, db.commentDao(),
                (commentWithUser) -> {
                    replyingToCommentId = commentWithUser.comment.commentId;

                    String username = "Người dùng"; // Tên mặc định
                    if(commentWithUser.user != null && commentWithUser.user.username != null) {
                        username = commentWithUser.user.username.split("@")[0];
                    }

                    replyingToUsername = username;
                    binding.etCommentInput.setHint("Trả lời " + replyingToUsername + "...");
                    binding.etCommentInput.requestFocus();
                    // Hiện bàn phím
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(binding.etCommentInput, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
        );
        binding.rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvComments.setAdapter(adapter);
    }

    private void checkLoginState() {
        if(binding == null) return; // Thêm kiểm tra
        // (code checkLoginState cũ giữ nguyên)
    }

    // --- THÊM HÀM NÀY ---
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false; // Đặt lại cờ

        if (mediaController != null && playerListener != null) {
            mediaController.removeListener(playerListener);
        }
        binding = null; // Tránh memory leak
    }
}