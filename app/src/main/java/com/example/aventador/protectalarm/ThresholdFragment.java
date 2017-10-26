package com.example.aventador.protectalarm;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.State;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.tools.Logger;
import com.example.aventador.protectalarm.tools.Tools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;

import static android.view.View.GONE;


public class ThresholdFragment extends Fragment {

    private static final String TAG = "ThresholdFragment";
    private LinearLayout layoutSelectFrequency;
    private EditText frequencyEditText;

    private LinearLayout layoutSearchOptimalThreshold;
    private ProgressBar progressBarSearchOptimalThreshold;
    private TextView rssiTextView;
    private String rssiTolerance;
    private Button searchOptimalThresholdButton;

    public ThresholdFragment() {
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

        // -------------------- //
        View bodyView = inflater.inflate(R.layout.fragment_threshold, container, false);
        layoutSelectFrequency = (LinearLayout) bodyView.findViewById(R.id.frequency_select_layout);
        frequencyEditText = (EditText) bodyView.findViewById(R.id.frequencyEditText);

        frequencyEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    frequencyEditText.clearFocus();
                }
                return false;
            }
        });

        // -------------------- //
        layoutSearchOptimalThreshold = (LinearLayout) bodyView.findViewById(R.id.layout_search_optimal);
        progressBarSearchOptimalThreshold = (ProgressBar) bodyView.findViewById(R.id.progressBarSearchOptimalThreshoold);
        progressBarSearchOptimalThreshold.setVisibility(View.INVISIBLE);
        rssiTextView = (TextView) bodyView.findViewById(R.id.rssiValuetextView);
        rssiTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogDbTolerance();
            }
        });
        searchOptimalThresholdButton = (Button) bodyView.findViewById(R.id.searchOptimalThresholdButton);
        searchOptimalThresholdButton.setEnabled(false);
        searchOptimalThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearchOptimalThreshold();
            }
        });
        frequencyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String frequency = frequencyEditText.getText().toString();
                if (Tools.isValidFrequency(frequency)) {
                    Logger.d(TAG, "post new frequency");
                    HashMap<String, String> parameters = new HashMap<String, String>();
                    parameters.put(Parameter.FREQUENCY.toString(), frequency);
                    EventBus.getDefault().postSticky(new StateEvent(State.FREQUENCY_SELECTED, parameters));
                }
            }
        });
        return bodyView;
    }

    /**
     * Update widgets and send an event "START_SEARCH_THRESHOLD" to {@link Main2Activity}
     */
    private void startSearchOptimalThreshold() {
        progressBarSearchOptimalThreshold.setVisibility(View.VISIBLE);
        searchOptimalThresholdButton.setText("Stop Searching");
        searchOptimalThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSearchOptimalThreshold(); // behavior of searchOptimalThresholdButton changed.
            }
        });

        String frequency = frequencyEditText.getText().toString();
        if (!Tools.isValidFrequency(frequency)) { // if frequency is wrong.
            Toast toast = Toast.makeText(getContext(), "Wrong frequency", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            return;
        }
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(Parameter.FREQUENCY.toString(), frequency);

        EventBus.getDefault().postSticky(new ActionEvent(Action.START_SEARCH_THRESHOLD, parameters));

    }

    /**
     * Update widgets and send an event "STOP_SEARCH_THRESHOLD" to {@link Main2Activity}
     */
    private void stopSearchOptimalThreshold() {
        resetFragment();
        EventBus.getDefault().postSticky(new ActionEvent(Action.STOP_SEARCH_THRESHOLD, "")); // Post event to Activity.
    }

    /**
     * Displays a alertDialog to change the DB value.
     * When user has modified the db value, an event "DB_TOLERANCE_SELECTED" is posted.
     */
    private void showDialogDbTolerance() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Set db tolerance (for experimented user)");

        // Set up the input
        final EditText input = new EditText(getContext());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                rssiTolerance = input.getText().toString();
                rssiTextView.setText("Threshold: " + rssiTolerance);
                HashMap<String, String> parameters = new HashMap<String, String>();
                parameters.put(Parameter.RSSI_VALUE.toString(), rssiTolerance);
                EventBus.getDefault().postSticky(new StateEvent(State.DB_TOLERANCE_SELECTED, parameters));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * Reset the view:
     * - text button is reset
     * - progress bar hidden
     * - startSearchOptimalThreshold() called when searchOptimalThresholdButton is pressed
     *
     */
    public void resetFragment() {
        searchOptimalThresholdButton.setText("Search Optimal Threshold");
        progressBarSearchOptimalThreshold.setVisibility(View.GONE);
        searchOptimalThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearchOptimalThreshold();
            }
        });
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
            case CONNECTED: {
                resetFragment();
                searchOptimalThresholdButton.setEnabled(true);
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case DISCONNECTED: {
                resetFragment();
                searchOptimalThresholdButton.setEnabled(false);
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case SEARCH_OPTIMAL_PEAK_DONE: {
                resetFragment();
                String rssi = stateEvent.getParameters().getString(Parameter.RSSI_VALUE.toString());
                rssiTolerance = rssi;
                rssiTextView.setText("Threshold: " + rssi);
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case SEARCH_OPTIMAL_PEAK_FAIL: {
                resetFragment();
                searchOptimalThresholdButton.setEnabled(true);
                break;
            }
        }
    }
}
