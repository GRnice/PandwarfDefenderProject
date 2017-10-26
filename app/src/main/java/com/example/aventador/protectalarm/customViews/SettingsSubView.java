package com.example.aventador.protectalarm.customViews;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.GollumException;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetGeneric;
import com.example.aventador.protectalarm.R;
import com.example.aventador.protectalarm.storage.Configuration;
import com.example.aventador.protectalarm.storage.FileManager;

/**
 * Created by Aventador on 03/10/2017.
 */

/**
 * SettingsSubView is responsible for displaying two seekbar (peak tolerance, margin error)
 * and two buttons in charge of load and store different Configurations {@link Configuration}
 * SettingsSubView is the controller of the settings sub view. It extend GuardianSubView
 */
public class SettingsSubView extends GuardianSubView {

    private int tolerance = 50; // default value
    private int marginError = 10; // default value

    /**
     * cbLoadCalled is called when user press load button.
     * cbSaveCalled is called when user press save button.
     */
    private GollumCallbackGetBoolean cbLoadCalled, cbSaveCalled;

    private TextView toleranceTextView;
    private TextView marginErrorTextView;
    private BootstrapButton saveButton;
    private BootstrapButton loadButton;
    private SeekBar seekBarTolerance;
    private SeekBar seekBarMarginError;



    private ViewPager viewPager;
    private Context context;

    public SettingsSubView(Context context, String title, int layoutResId, ViewPager viewPager) {
        super(title, layoutResId);
        this.context = context;
        this.viewPager = viewPager;
    }

    public int getPeakTolerance() {
        return tolerance;
    }

    public int getMarginError() {
        return marginError;
    }

    /**
     * Like onCreateView, this method inflate the view and gets all widgets.
     * @param inflater
     * @param viewGroup
     * @return
     */
    @Override
    public View instantiate(LayoutInflater inflater, ViewGroup viewGroup) {
        layout = inflater.inflate(getLayoutResId(), viewGroup, false);
        ImageView switchToHistoryImageView = (ImageView) layout.findViewById(R.id.switch_page_image_view);
        switchToHistoryImageView.setOnClickListener(this);

        toleranceTextView = (TextView) layout.findViewById(R.id.tolerance_textview);
        marginErrorTextView = (TextView) layout.findViewById(R.id.margin_error_textview);

        seekBarTolerance = (SeekBar) layout.findViewById(R.id.tolerance_seekbar);
        seekBarTolerance.setOnSeekBarChangeListener(this);
        seekBarTolerance.setProgress(tolerance);

        seekBarMarginError = (SeekBar) layout.findViewById(R.id.margin_error_seekbar);
        seekBarMarginError.setOnSeekBarChangeListener(this);
        seekBarMarginError.setProgress(marginError);

        saveButton = (BootstrapButton) layout.findViewById(R.id.save_config_button);
        saveButton.setOnClickListener(this);

        loadButton = (BootstrapButton) layout.findViewById(R.id.load_config_button);
        loadButton.setOnClickListener(this);

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
                // if switch button is pressed -> go to history sub view.
                this.viewPager.setCurrentItem(this.viewPager.getCurrentItem() + 1);
                break;
            }
            case R.id.save_config_button: {
                cbSaveCalled.done(true); // main activity will be notified
                break;
            }
            case R.id.load_config_button: {
                cbLoadCalled.done(true); // main activity will be notified
                break;
            }
        }
    }

    /**
     * Called by main activity
     * cbSaveCalled  is called when user press load button
     * @param cbDone
     */
    public void setOnLoadConfig(GollumCallbackGetBoolean cbDone) {
        this.cbLoadCalled = cbDone;
    }

    /**
     * Called by main activity
     * cbSaveCalled  is called when user press save button
     * @param cbDone
     */
    public void setOnSaveConfig(GollumCallbackGetBoolean cbDone) {
        this.cbSaveCalled = cbDone;
    }

    public void setPeakTolerance(int peakTolerance) {
        this.seekBarTolerance.setProgress(peakTolerance);
    }

    public void setMarginError(int marginError) {
        this.seekBarMarginError.setProgress(marginError);
    }
}
