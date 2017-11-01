package com.example.aventador.protectalarm.customViews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.aventador.protectalarm.R;

/**
 * Created by Giangrasso on 02/10/2017.
 */

/**
 * All subviews of Guardian fragment must extends this class
 *
 * sub views are instanciated by calling "instantiate(LayoutInflater inflater, ViewGroup viewGroup)"
 * instantiate is like an onCreateView...
 */
public abstract class GuardianSubView implements View.OnClickListener {
    private int layoutResId;
    private String title;
    protected View layout;

    public int getLayoutResId() {
        return layoutResId;
    }

    public View getLayout() {
        return layout;
    }

    public String getTitle() {
        return title;
    }

    public GuardianSubView(String title, int layoutResId) {
        this.layoutResId = layoutResId;
        this.title = title;
    }

    public abstract View instantiate(LayoutInflater inflater, ViewGroup viewGroup);


}
