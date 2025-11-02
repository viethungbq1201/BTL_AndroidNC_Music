package com.example.btl_androidnc_music.data.model;

import java.util.List;

public class CategoryRow {
    public String genreTitle;
    public List<Track> trackList;

    public CategoryRow(String genreTitle, List<Track> trackList) {
        this.genreTitle = genreTitle;
        this.trackList = trackList;
    }
}
