package com.example.btl_androidnc_music; // Thay package của bạn

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

// Adapter cho bình luận CHA (cuộn dọc)
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<CommentWithUser> commentList;
    private CommentDao commentDao;
    private OnReplyClickListener replyClickListener;
    private Context context; // Thêm Context

    public interface OnReplyClickListener {
        void onReplyClick(CommentWithUser parentComment);
    }

    public CommentAdapter(List<CommentWithUser> commentList, CommentDao commentDao, OnReplyClickListener listener) {
        this.commentList = commentList;
        this.commentDao = commentDao;
        this.replyClickListener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext(); // Lưu lại context
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentWithUser commentWithUser = commentList.get(position);
        Comment comment = commentWithUser.comment;
        User user = commentWithUser.user;

        // Hiển thị tên
        if (user != null) {
            // Cắt chuỗi email để lấy tên trước @
            holder.tvUsername.setText(user.username.split("@")[0]);
        } else {
            holder.tvUsername.setText(comment.username.split("@")[0]);
        }

        holder.tvContent.setText(comment.content);
        holder.tvTimestamp.setText(formatTimestamp(comment.timestamp)); // Dùng hàm format mới
        holder.tvLikeCount.setText(String.valueOf(comment.likeCount));

        holder.btnReply.setOnClickListener(v -> replyClickListener.onReplyClick(commentWithUser));

        holder.btnLike.setOnClickListener(v -> {
            comment.likeCount++;
            holder.tvLikeCount.setText(String.valueOf(comment.likeCount));
            // (Tạm thời chưa xử lý trạng thái đã like)
            Executors.newSingleThreadExecutor().execute(() -> commentDao.updateComment(comment));
        });

        // Tải và hiển thị các bình luận trả lời (CON)
        loadReplies(holder, comment);
    }

    private void loadReplies(CommentViewHolder holder, Comment parentComment) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<CommentWithUser> replies = commentDao.getRepliesForComment(parentComment.commentId);

            if (replies != null && !replies.isEmpty()) {
                holder.itemView.post(() -> {
                    ReplyAdapter replyAdapter = new ReplyAdapter(replies, commentDao, context); // Truyền context
                    holder.rvReplies.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
                    holder.rvReplies.setAdapter(replyAdapter);
                    holder.rvReplies.setVisibility(View.VISIBLE);
                });
            } else {
                holder.itemView.post(() -> holder.rvReplies.setVisibility(View.GONE));
            }
        });
    }

    // Hàm format thời gian đẹp hơn (ví dụ: "vừa xong", "5 phút trước")
    private String formatTimestamp(long timestamp) {
        try {
            long now = System.currentTimeMillis();
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    timestamp, now, DateUtils.MINUTE_IN_MILLIS);
            return relativeTime.toString();
        } catch (Exception e) {
            return "vừa xong";
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvTimestamp, tvContent, tvLikeCount, btnReply;
        ImageButton btnLike;
        ImageView ivAvatar;
        RecyclerView rvReplies;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnLike = itemView.findViewById(R.id.btnLikeComment);
            rvReplies = itemView.findViewById(R.id.rvReplies);
        }
    }
}

// --- ADAPTER CHO BÌNH LUẬN CON (REPLY) ---
class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {

    private List<CommentWithUser> replyList;
    private CommentDao commentDao;
    private Context context;

    public ReplyAdapter(List<CommentWithUser> replyList, CommentDao commentDao, Context context) {
        this.replyList = replyList;
        this.commentDao = commentDao;
        this.context = context;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Dùng lại layout list_item_comment
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_comment, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        CommentWithUser replyWithUser = replyList.get(position);
        Comment reply = replyWithUser.comment;
        User user = replyWithUser.user;

        if (user != null) {
            holder.tvUsername.setText(user.username.split("@")[0]);
        } else {
            holder.tvUsername.setText(reply.username.split("@")[0]);
        }

        holder.tvContent.setText(reply.content);
        holder.tvTimestamp.setText(formatTimestamp(reply.timestamp));
        holder.tvLikeCount.setText(String.valueOf(reply.likeCount));

        // Ẩn nút "Trả lời" và "RecyclerView con"
        holder.btnReply.setVisibility(View.GONE);
        holder.rvReplies.setVisibility(View.GONE);

        holder.btnLike.setOnClickListener(v -> {
            reply.likeCount++;
            holder.tvLikeCount.setText(String.valueOf(reply.likeCount));
            Executors.newSingleThreadExecutor().execute(() -> commentDao.updateComment(reply));
        });
    }

    private String formatTimestamp(long timestamp) {
        try {
            long now = System.currentTimeMillis();
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    timestamp, now, DateUtils.MINUTE_IN_MILLIS);
            return relativeTime.toString();
        } catch (Exception e) {
            return "vừa xong";
        }
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvTimestamp, tvContent, tvLikeCount, btnReply;
        ImageButton btnLike;
        ImageView ivAvatar;
        RecyclerView rvReplies;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnLike = itemView.findViewById(R.id.btnLikeComment);
            rvReplies = itemView.findViewById(R.id.rvReplies);
        }
    }
}