package com.example.aventador.protectalarm.customViews;

/**
 * Created by Aventador on 02/10/2017.
 */
public class GuardianSubView {
    private int layoutResId;
    private String title;

    public int getLayoutResId() {
        return layoutResId;
    }

    public String getTitle() {
        return title;
    }

    public GuardianSubView(String title, int layoutResId) {
        this.layoutResId = layoutResId;
        this.title = title;
    }
}
