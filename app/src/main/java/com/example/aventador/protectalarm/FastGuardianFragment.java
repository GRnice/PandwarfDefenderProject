package com.example.aventador.protectalarm;


import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapProgressBar;
import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.State;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.process.Pandwarf;
import com.example.aventador.protectalarm.storage.Configuration;
import com.example.aventador.protectalarm.tools.Logger;
import com.example.aventador.protectalarm.tools.Tools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;

import static com.example.aventador.protectalarm.FastGuardianFragment.FastGuardianState.NO_PROCESS_ON_RUN;
import static com.example.aventador.protectalarm.FastGuardianFragment.FastGuardianState.PROTECTION_ON_RUN;
import static com.example.aventador.protectalarm.FastGuardianFragment.FastGuardianState.SCAN_ON_RUN;


/**
 * A simple {@link Fragment} subclass.
 */

/**
 * FastGuardianFragment :
 * calculates the best possible parameters (dbTolerance, Margin error, peak tolerance).
 * By providing optimal protection.
 */
public class FastGuardianFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "FastGuardianFragment";
    private FastGuardianState currentState;
    private boolean frequencyChangeEventReceived;

    private CheckBox mCheckBoxRunProtectionAfterScan;
    private BootstrapButton mButtonScanProtection;
    private BootstrapProgressBar mProgressBar;
    private EditText mEditTextFrequency;
    private Configuration currentConfiguration = null;


    public FastGuardianFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fast_guardian, container, false);
        frequencyChangeEventReceived = false;

        mProgressBar = (BootstrapProgressBar) view.findViewById(R.id.fast_guardian_progress_bar);
        mProgressBar.setProgress(100);
        mButtonScanProtection = (BootstrapButton) view.findViewById(R.id.button_fast_guardian_scan_protect);
        mButtonScanProtection.setOnClickListener(this);
        mCheckBoxRunProtectionAfterScan = (CheckBox) view.findViewById(R.id.checkBox_fast_guardian_start_after_scan);
        mEditTextFrequency = (EditText) view.findViewById(R.id.frequency_fast_guardian_edittext);
        mEditTextFrequency.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                frequencyChanged();
            }
        });
        resetFragment();

        return view;
    }

    @Override
    @CallSuper
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    @CallSuper
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void resetFragment() {
        currentState = NO_PROCESS_ON_RUN;
        if (Pandwarf.getInstance().isConnected()) {
            mButtonScanProtection.setEnabled(true);
        } else {
            mButtonScanProtection.setEnabled(false);
        }

        mButtonScanProtection.setBackgroundColor(getResources().getColor(R.color.successColor));
        if (currentConfiguration == null) {
            mButtonScanProtection.setText("Scan");
        } else {
            mButtonScanProtection.setText("Start protection");
        }
        mProgressBar.setVisibility(View.GONE);
    }

    private void startScan() {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(Parameter.FREQUENCY.toString(), mEditTextFrequency.getText().toString());
        mButtonScanProtection.setText("Stop Scan");

        mButtonScanProtection.setBackgroundColor(getResources().getColor(R.color.bootstrap_brand_danger));
        mProgressBar.setVisibility(View.VISIBLE);
        EventBus.getDefault().postSticky(new ActionEvent(Action.START_FAST_PROTECTION_ANALYZER, parameters));
    }

    private void stopScan() {
        resetFragment();
        EventBus.getDefault().postSticky(new ActionEvent(Action.STOP_FAST_PROTECTION_ANALYZER, ""));
    }

    private void startProtection() {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(Parameter.FREQUENCY.toString(), String.valueOf(currentConfiguration.getFrequency()));
        parameters.put(Parameter.RSSI_VALUE.toString(), String.valueOf(currentConfiguration.getDbTolerance()));
        parameters.put(Parameter.PEAK_TOLERANCE.toString(), String.valueOf(currentConfiguration.getPeakTolerance()));
        parameters.put(Parameter.MARGIN_ERROR.toString(), String.valueOf(currentConfiguration.getMarginError()));

        mButtonScanProtection.setText("Stop Protection");

        mButtonScanProtection.setBackgroundColor(getResources().getColor(R.color.bootstrap_brand_danger));
        mProgressBar.setVisibility(View.VISIBLE);
        EventBus.getDefault().postSticky(new ActionEvent(Action.START_PROTECTION, parameters));

    }

    private void stopProtection() {
        resetFragment();
        EventBus.getDefault().postSticky(new ActionEvent(Action.STOP_PROTECTION, ""));
    }

    private void frequencyChanged() {
        String frequency = mEditTextFrequency.getText().toString();
        if (Tools.isValidFrequency(frequency) && !frequencyChangeEventReceived) {
            Logger.d(TAG, "post new frequency");
            HashMap<String, String> parameters = new HashMap<String, String>();
            parameters.put(Parameter.FREQUENCY.toString(), frequency);
            org.greenrobot.eventbus.EventBus.getDefault().postSticky(new StateEvent(State.FREQUENCY_SELECTED, parameters));
        } else {
            frequencyChangeEventReceived = false;
        }
    }

    @Override
    public void onClick(View view) {
        Logger.d(TAG, "onClick");
        if (view == mButtonScanProtection) {
            Logger.d(TAG, "onClick: mButtonScanProtection");
            if (currentState == NO_PROCESS_ON_RUN && currentConfiguration == null) {
                Logger.d(TAG, "onClick: startScan");
                currentState = SCAN_ON_RUN;
                startScan();
            }  else if (currentState == NO_PROCESS_ON_RUN && currentConfiguration != null){
                Logger.d(TAG, "onClick: startProtection");
                currentState = PROTECTION_ON_RUN;
                startProtection();
            } else if (currentState == SCAN_ON_RUN) {
                Logger.d(TAG, "onClick: stopScan");
                currentState = NO_PROCESS_ON_RUN;
                stopScan();
            } else if (currentState == PROTECTION_ON_RUN) {
                Logger.d(TAG, "onClick: stopProtection");
                currentState = NO_PROCESS_ON_RUN;
                stopProtection();
            }
        }


    }

    /**
     * Used by EventBus
     * Called when a Publisher send a state.
     * @param stateEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(StateEvent stateEvent) {
        switch (stateEvent.getState()) {
            /**
             * Event from DongleCallbacks {@link com.example.aventador.protectalarm.callbacks.DongleCallbacks}
             */
            case CONNECTED:
            case DISCONNECTED: {
                Logger.d(TAG, "event: CONNECTED");
                resetFragment();
                break;
            }

            /**
             * Event from ThresholdFragment or GuardianFragment{@link ThresholdFragment}
             */
            case FREQUENCY_SELECTED: {
                Logger.d(TAG, "event: FREQUENCY_SELECTED");
                frequencyChangeEventReceived = true;
                String frequencySelected = stateEvent.getParameters().getString(Parameter.FREQUENCY.toString());
                this.mEditTextFrequency.setText(frequencySelected);
                frequencyChangeEventReceived = false;
                break;
            }
        }
    }

    protected enum FastGuardianState {
        NO_PROCESS_ON_RUN,
        SCAN_ON_RUN,
        PROTECTION_ON_RUN;
    }
}
