package com.example.aventador.protectalarm.customViews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.aventador.protectalarm.R;

/**
 * Created by Aventador on 03/10/2017.
 */

public class HistorySubView extends GuardianSubView {

    public HistorySubView(String title, int layoutResId) {
        super(title, layoutResId);
    }

    @Override
    public View instantiate(LayoutInflater inflater, ViewGroup viewGroup) {
        return inflater.inflate(getLayoutResId(), viewGroup, false);
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
