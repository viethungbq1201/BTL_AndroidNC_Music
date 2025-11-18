package com.example.btl_androidnc_music.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {

    private static final String PREFS_NAME = "search_history_prefs";
    private static final String KEY_HISTORY = "search_history_json";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public SearchHistoryManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }


    // Lấy danh sách lịch sử từ JSON string
    public List<String> getSearchHistory() {
        String jsonHistory = sharedPreferences.getString(KEY_HISTORY, null);
        if (jsonHistory == null) {
            return new ArrayList<>(); // Trả về list rỗng nếu chưa có
        }

        // Dùng Gson để chuyển JSON string về lại List<String>
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(jsonHistory, type);
    }

    // Lưu một từ khóa mới
    public void saveSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        String cleanQuery = query.trim();

        // 1. Lấy danh sách cũ
        List<String> historyList = getSearchHistory();

        // 2. Xóa từ khóa này nếu đã tồn tại (để đưa nó lên đầu)
        historyList.remove(cleanQuery);

        // 3. Thêm từ khóa mới vào đầu danh sách
        historyList.add(0, cleanQuery);

        // 4. Giới hạn danh sách chỉ 10 mục
        if (historyList.size() > 10) {
            historyList = historyList.subList(0, 10);
        }

        // 5. Dùng Gson chuyển List thành JSON string
        String jsonHistory = gson.toJson(historyList);

        // 6. Lưu chuỗi JSON mới
        sharedPreferences.edit().putString(KEY_HISTORY, jsonHistory).apply();
    }

    public void deleteSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        List<String> historyList = getSearchHistory();
        if (historyList.contains(query)) {
            historyList.remove(query);

            // Dùng Gson chuyển List thành JSON string
            String jsonHistory = gson.toJson(historyList);

            // Lưu lại chuỗi JSON mới
            sharedPreferences.edit().putString(KEY_HISTORY, jsonHistory).apply();
        }
    }
}