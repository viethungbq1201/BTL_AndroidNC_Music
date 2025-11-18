package com.example.btl_androidnc_music.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_androidnc_music.R;
import com.example.btl_androidnc_music.data.model.CategoryRow;

import java.util.List;

// Adapter cho RecyclerView DỌC (chứa các hàng thể loại)
public class HomeCategoryAdapter extends RecyclerView.Adapter<HomeCategoryAdapter.CategoryViewHolder> {

    private List<CategoryRow> categoryRows;
    private CategorySongAdapter.OnSongClickListener innerSongClickListener;

    public HomeCategoryAdapter(List<CategoryRow> categoryRows, CategorySongAdapter.OnSongClickListener listener) {
        this.categoryRows = categoryRows;
        this.innerSongClickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_category_row, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryRow row = categoryRows.get(position);

        // 1. Đặt tên thể loại
        holder.tvTitle.setText(row.genreTitle);

        // 2. Tạo Adapter BÊN TRONG (cuộn ngang)
        CategorySongAdapter innerAdapter = new CategorySongAdapter(row.trackList, innerSongClickListener);

        // 3. Gán Adapter bên trong vào RecyclerView bên trong
        holder.rvInner.setAdapter(innerAdapter);
    }

    @Override
    public int getItemCount() {
        return categoryRows.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        RecyclerView rvInner;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvCategoryTitle);
            rvInner = itemView.findViewById(R.id.rvInnerSongs);
        }
    }
}