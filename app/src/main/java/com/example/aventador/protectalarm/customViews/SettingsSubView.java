package com.example.aventador.protectalarm.customViews;

import android.support.v4.view.ViewPager;
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

public class SettingsSubView extends GuardianSubView {
    private TextView toleranceTextView;
    private int tolerance;

    private TextView marginErrorTextView;
    private int marginError;

    private ViewPager viewPager;

    public SettingsSubView(String title, int layoutResId, ViewPager viewPager) {
        super(title, layoutResId);
        this.viewPager = viewPager;
    }

    public int getPeakTolerance() {
        return tolerance;
    }

    public int getMarginError() {
        return marginError;
    }

    @Override
    public View instantiate(LayoutInflater inflater, ViewGroup viewGroup) {
        layout = inflater.inflate(getLayoutResId(), viewGroup, false);
        ImageView switchToHistoryImageView = (ImageView) layout.findViewById(R.id.switch_page_image_view);
        switchToHistoryImageView.setOnClickListener(this);

        SeekBar seekBarTolerance = (SeekBar) layout.findViewById(R.id.tolerance_seekbar);
        seekBarTolerance.setOnSeekBarChangeListener(this);
        SeekBar seekBarMarginError = (SeekBar) layout.findViewById(R.id.margin_error_seekbar);
        seekBarMarginError.setOnSeekBarChangeListener(this);

        toleranceTextView = (TextView) layout.findViewById(R.id.tolerance_textview);
        marginErrorTextView = (TextView) layout.findViewById(R.id.margin_error_textview);

        return layout;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        switch (seekBar.getId()) {
            case R.id.tolerance_seekbar: {
                tolerance = i;
                toleranceTextView.setText("Tolerance: " + tolerance + "%");
                break;
            }
            case R.id.margin_error_seekbar: {
                marginError = i;
                marginErrorTextView.setText("Margin error : " + marginError + "%");
                break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.switch_page_image_view: {
                this.viewPager.setCurrentItem(this.viewPager.getCurrentItem() + 1);
                break;
            }
        }
    }
}
