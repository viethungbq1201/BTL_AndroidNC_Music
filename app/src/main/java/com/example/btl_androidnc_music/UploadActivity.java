package com.example.btl_androidnc_music;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room; // Cần import Room
import com.example.btl_androidnc_music.databinding.ActivityUploadBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import android.media.MediaMetadataRetriever;

public class UploadActivity extends AppCompatActivity {

    private ActivityUploadBinding binding;
    private Uri selectedFileUri; // Lưu Uri của file nhạc đã chọn
    private AppDatabase db; // Database Room của bạn
    private Uri selectedImageUri;
    public static final String EXTRA_TRACK_TO_EDIT = "TRACK_TO_EDIT";
    private Track mTrackToEdit = null;
    private String selectedFileDuration = null;

    // Trình xử lý kết quả sau khi chọn file
    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri; // Lưu Uri của file

                    // Cập nhật UI ngay lập tức
                    binding.tvSelectedFileName.setText("Đang lấy thời lượng...");

                    // Chạy tác vụ lấy thời lượng trên luồng nền
                    Executors.newSingleThreadExecutor().execute(() -> {
                        // Lấy thời lượng (hàm này bạn đã có)
                        final String duration;
                        try {
                            duration = getDurationFromUri(uri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        // Lưu thời lượng vào biến thành viên
                        selectedFileDuration = duration;

                        // Cập nhật TextView trên luồng UI chính
                        runOnUiThread(() -> {
                            binding.tvSelectedFileName.setText("Thời lượng: " + duration);
                        });
                    });
                }
            }
    );

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.ivImagePreview.setImageURI(uri);
                    binding.ivImagePreview.setVisibility(View.VISIBLE); // Hiển thị ảnh
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo DB (Nên dùng Singleton)
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration() // <-- THÊM DÒNG NÀY
                .build();

        if (getIntent().hasExtra(EXTRA_TRACK_TO_EDIT)) {
            // Đây là chế độ Sửa
            mTrackToEdit = (Track) getIntent().getSerializableExtra(EXTRA_TRACK_TO_EDIT);
            setTitle("Sửa thông tin bài hát"); // Đổi tiêu đề
            populateUiForEdit(mTrackToEdit);
        } else {
            // Đây là chế độ Tải lên (như cũ)
            setTitle("Tải nhạc lên");
            binding.btnUpload.setText("Tải Lên");
        }

        // Nút chọn file: Mở trình chọn file hệ thống
        binding.btnSelectFile.setOnClickListener(v -> {
            filePickerLauncher.launch("audio/*"); // Chỉ cho phép chọn file âm thanh
        });

        binding.btnSelectImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        // Nút Tải Lên (Import)
        binding.btnUpload.setOnClickListener(v -> {
            saveTrack();
        });
    }

    // Đổi tên hàm:
    private void saveTrack() {
        String title = binding.etTrackName.getText().toString().trim();
        String artist = binding.etArtistName.getText().toString().trim();
        String genre = binding.etGenre.getText().toString().trim();

        // Kiểm tra: Nếu là chế độ TẠO MỚI (mTrackToEdit == null) thì BẮT BUỘC phải chọn file nhạc
        if (mTrackToEdit == null && selectedFileUri == null) {
            Toast.makeText(this, "Vui lòng chọn file nhạc", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() || artist.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Tên và Tác giả", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            // Xác định xem có phải Sửa hay không
            boolean isEditMode = (mTrackToEdit != null);

            // Lấy track: nếu là sửa thì lấy track cũ, nếu tạo mới thì tạo track mới
            Track track = isEditMode ? mTrackToEdit : new Track();

            // 1. Xử lý file nhạc
            if (selectedFileUri != null) {
                // Người dùng đã chọn 1 file nhạc MỚI (cả khi Sửa và Tải lên)
                String newFilePath = copyFileToInternalStorage(selectedFileUri, "track_" + System.currentTimeMillis());
                if (newFilePath != null) {
                    track.filePath = newFilePath;
                    // Chỉ lấy thời lượng khi file nhạc thay đổi
                    track.duration = selectedFileDuration;
                }
            }
            // (Nếu selectedFileUri == null và đang ở chế độ Sửa, ta giữ nguyên filePath và duration cũ)

            // 2. Xử lý file ảnh
            if (selectedImageUri != null) {
                // Người dùng đã chọn 1 file ảnh MỚI
                String newImagePath = copyFileToInternalStorage(selectedImageUri, "image_" + System.currentTimeMillis());
                track.imagePath = newImagePath;
            }
            // (Nếu selectedImageUri == null và đang ở chế độ Sửa, ta giữ nguyên imagePath cũ)

            // 3. Cập nhật thông tin text
            track.title = title;
            track.artist = artist;
            track.genre = genre;

            // 4. Lưu vào Database
            if (isEditMode) {
                db.trackDao().updateTrack(track); // <-- Gọi hàm UPDATE
            } else {
                db.trackDao().insertTrack(track); // <-- Gọi hàm INSERT
            }

            // 5. Thông báo thành công và quay lại
            runOnUiThread(() -> {
                String message = isEditMode ? "Cập nhật thành công!" : "Tải lên thành công!";
                Toast.makeText(UploadActivity.this, message, Toast.LENGTH_SHORT).show();

                setResult(Activity.RESULT_OK); // <-- BÁO KẾT QUẢ OK
                finish();
            });
        });
    }
    // Thêm hàm mới này vào UploadActivity.java
    private void populateUiForEdit(Track track) {
        binding.etTrackName.setText(track.title);
        binding.etArtistName.setText(track.artist);
        binding.etGenre.setText(track.genre);

        // Hiển thị tên file nhạc cũ
        if (track.filePath != null) {
            binding.tvSelectedFileName.setText("Thời lượng: " + track.duration);
        }

        // Hiển thị ảnh cũ (nếu có)
        if (track.imagePath != null) {
            binding.ivImagePreview.setImageURI(Uri.fromFile(new File(track.imagePath)));
            binding.ivImagePreview.setVisibility(View.VISIBLE);
        }

        // Đổi chữ trên nút
        binding.btnUpload.setText("Cập nhật");
    }
    private String getDurationFromUri(Uri uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            String durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long durationMs = Long.parseLong(durationMsStr);
            long minutes = (durationMs / 1000) / 60;
            long seconds = (durationMs / 1000) % 60;
            return String.format("%d:%02d", minutes, seconds);
        } catch (Exception e) {
            e.printStackTrace();
            return "N/A";
        } finally {
            retriever.release();
        }
    }

    // Hàm Helper để sao chép file từ Uri vào Internal Storage
    private String copyFileToInternalStorage(Uri uri, String newFileName) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            File file = new File(getFilesDir(), newFileName); // getFilesDir() là thư mục internal
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
            }
            return file.getAbsolutePath(); // Trả về đường dẫn tuyệt đối của file mới
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}