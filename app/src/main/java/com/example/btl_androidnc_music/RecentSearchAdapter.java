package com.example.btl_androidnc_music;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.ViewHolder> {

    private List<String> queries;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener; // <-- THÊM MỚI

    // Interface cho sự kiện Click
    public interface OnItemClickListener {
        void onItemClick(String query);
    }

    // <-- THÊM MỚI: Interface cho sự kiện Long Click -->
    public interface OnItemLongClickListener {
        void onItemLongClick(String query, int position);
    }

    // <-- SỬA LẠI: Cập nhật Constructor -->
    public RecentSearchAdapter(List<String> queries, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
        this.queries = queries;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recent_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = queries.get(position);
        holder.tvQuery.setText(query);

        // Sự kiện click (giữ nguyên)
        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(query));

        // <-- THÊM MỚI: Sự kiện long click (nhấn giữ) -->
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(query, position);
                return true; // Đánh dấu là đã xử lý
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return queries.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuery;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuery = itemView.findViewById(R.id.tvRecentQuery);
        }
    }
}