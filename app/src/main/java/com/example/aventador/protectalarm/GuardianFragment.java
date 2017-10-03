package com.example.aventador.protectalarm;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapBadge;
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.api.attributes.BootstrapBrand;
import com.example.aventador.protectalarm.customViews.CustomPagerAdapter;
import com.example.aventador.protectalarm.customViews.SettingsSubView;
import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.tools.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.w3c.dom.Text;

import java.util.HashMap;

import static android.view.View.GONE;


public class GuardianFragment extends Fragment {

    private final static String TAG = "GuardianFragment";
    private LinearLayout layoutStartStopProtection;
    private BootstrapButton startStopProtectionButton;
    private TextView frequencyTextView;
    private EditText frequencyEditText;
    private TextView dbToleranceTextView;
    private EditText dbToleranceEditText;
    private ViewPager viewPager;


    public GuardianFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    @CallSuper
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View bodyView = inflater.inflate(R.layout.fragment_guardian, container, false);
        // -------------------- //
        frequencyTextView = (TextView) bodyView.findViewById(R.id.frequency_guardian_textview);
        frequencyEditText = (EditText) bodyView.findViewById(R.id.frequency_guardian_edittext);
        dbToleranceTextView = (TextView) bodyView.findViewById(R.id.db_tolerance_guardian_textview);
        dbToleranceEditText = (EditText) bodyView.findViewById(R.id.dbtolerance_guardian_edittext);

        layoutStartStopProtection = (LinearLayout) bodyView.findViewById(R.id.start_protection_layout);
        startStopProtectionButton = (BootstrapButton) bodyView.findViewById(R.id.start_stop_protection_button);

        startStopProtectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startProtection();

            }
        });
        startStopProtectionButton.setEnabled(false);
        viewPager = (ViewPager) bodyView.findViewById(R.id.guardian_view_pager);
        CustomPagerAdapter customPagerAdapter = new CustomPagerAdapter(this.getContext(), viewPager);
        viewPager.setAdapter(customPagerAdapter);
        viewPager.setOffscreenPageLimit(customPagerAdapter.getCount());
        return bodyView;
    }

    private void startProtection() {
        startStopProtectionButton.setBackgroundColor(0xFFD9534F); // warning color
        startStopProtectionButton.setText("Stop protection");
        HashMap<String, String> parameters = new HashMap<>();

        parameters.put(Parameter.FREQUENCY.toString(), frequencyEditText.getText().toString());
        parameters.put(Parameter.RSSI_VALUE.toString(), dbToleranceEditText.getText().toString());
        parameters.put(Parameter.PEAK_TOLERANCE.toString(), String.valueOf(getPeakTolerance()));
        parameters.put(Parameter.MARGIN_ERROR.toString(), String.valueOf(getMargingError()));
        EventBus.getDefault().postSticky(new ActionEvent(Action.START_PROTECTION, parameters));
        startStopProtectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopProtection();
            }
        });
    }

    private int getPeakTolerance() {
        CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        SettingsSubView settingsSubView = customPagerAdapter.getSettingsSubView();
        return settingsSubView.getPeakTolerance();
    }

    private int getMargingError() {
        CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        SettingsSubView settingsSubView = customPagerAdapter.getSettingsSubView();
        return settingsSubView.getMarginError();
    }

    private void stopProtection() {
        resetFragment();
        EventBus.getDefault().postSticky(new ActionEvent(Action.STOP_PROTECTION, ""));
    }

    private void resetFragment() {
        startStopProtectionButton.setBackgroundColor(0xFF5CB85C); // success color
        startStopProtectionButton.setText("Start protection");
        startStopProtectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startProtection();
            }
        });
    }

    /**
     * Used by EventBus
     * Called when a Publisher send a action to be executed.
     * @param actionEvent
     */
    @Subscribe
    public void onMessageEvent(ActionEvent actionEvent) {
    }

    /**
     * Used by EventBus
     * Called when a Publisher send a state.
     * @param stateEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(StateEvent stateEvent) {
        switch (stateEvent.getState()) {
            case CONNECTED: {
                startStopProtectionButton.setEnabled(true);
                break;
            }
            case DISCONNECTED: {
                startStopProtectionButton.setEnabled(false);
                resetFragment();
                break;
            }
            case FREQUENCY_SELECTED: {
                String frequencySelected = stateEvent.getParameters().getString(Parameter.FREQUENCY.toString());
                this.frequencyEditText.setText(frequencySelected);
                break;
            }
            case SEARCH_OPTIMAL_PEAK_DONE: {
                String dbTolerance  = stateEvent.getParameters().getString(Parameter.RSSI_VALUE.toString());
                this.dbToleranceEditText.setText(dbTolerance);
                break;
            }
            case PROTECTION_FAIL: {
                resetFragment();
                break;
            }
        }
    }
}
