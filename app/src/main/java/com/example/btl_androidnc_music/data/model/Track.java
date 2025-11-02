package com.example.btl_androidnc_music.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "tracks")
public class Track implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String artist;
    public String genre;
    public String filePath; // <-- Rất quan trọng: Đây là đường dẫn file trong internal storage
    public boolean isFavorite = false;
    public String imagePath;
    public String duration;
}