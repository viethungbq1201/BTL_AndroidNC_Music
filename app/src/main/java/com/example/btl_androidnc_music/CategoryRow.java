package com.example.btl_androidnc_music;

import java.util.List;

public class CategoryRow {
    String genreTitle;
    List<Track> trackList;

    public CategoryRow(String genreTitle, List<Track> trackList) {
        this.genreTitle = genreTitle;
        this.trackList = trackList;
    }
}
