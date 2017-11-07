package com.example.aventador.protectalarm;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.comthings.gollum.api.gollumandroidlib.GollumException;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetGeneric;
import com.example.aventador.protectalarm.customViews.HistoryLog;
import com.example.aventador.protectalarm.customViews.adapters.LogAdapter;
import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.State;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.storage.Configuration;
import com.example.aventador.protectalarm.storage.FileManager;
import com.example.aventador.protectalarm.tools.Logger;
import com.example.aventador.protectalarm.tools.Tools;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;

import static com.example.aventador.protectalarm.GuardianFragment.GuardianState.NO_PROCESS_ON_RUN;
import static com.example.aventador.protectalarm.GuardianFragment.GuardianState.PROTECTION_ON_RUN;
import static com.example.aventador.protectalarm.GuardianFragment.GuardianState.SCAN_ON_RUN;


public class GuardianFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private final static String TAG = "GuardianFragment";

    private GuardianState currentState;

    private boolean frequencyChangeEventReceived;

    private LinearLayout layoutStartStopProtection;
    private Button showHistoryButton;
    private Button startStopProtectionButton;

    private TextView frequencyTextView;
    private EditText frequencyEditText;
    private TextView dbToleranceTextView;
    private EditText dbToleranceEditText;
    private TextView toleranceTextView;
    private TextView marginErrorTextView;
    private SeekBar seekBarTolerance;
    private SeekBar seekBarMarginError;
    private int tolerance = 50; // default value
    private int marginError = 10; // default value

    private CheckBox checkBoxAdvancedMode;
    private boolean advancedModeEnable = false;

    private ProgressBar progressBarPandwarfRunning;

    private Configuration currentConfiguration;

    private ArrayList<HistoryLog> historyLogArrayList;


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
        currentConfiguration = new Configuration(); // contains values of frequency, dbTolerance, peak tolerance & margin error.
        frequencyChangeEventReceived = false;
        historyLogArrayList = new ArrayList<>();
        currentState = NO_PROCESS_ON_RUN;
        frequencyTextView = (TextView) bodyView.findViewById(R.id.frequency_guardian_textview);
        frequencyEditText = (EditText) bodyView.findViewById(R.id.frequency_guardian_edittext);
        frequencyEditText.addTextChangedListener(new TextWatcher() {
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
        dbToleranceTextView = (TextView) bodyView.findViewById(R.id.db_tolerance_guardian_textview);
        dbToleranceEditText = (EditText) bodyView.findViewById(R.id.dbtolerance_guardian_edittext);
        dbToleranceEditText.setEnabled(false);

        toleranceTextView = (TextView) bodyView.findViewById(R.id.tolerance_textview);
        marginErrorTextView = (TextView) bodyView.findViewById(R.id.margin_error_textview);

        seekBarTolerance = (SeekBar) bodyView.findViewById(R.id.tolerance_seekbar);
        seekBarTolerance.setOnSeekBarChangeListener(this);
        seekBarTolerance.setProgress(tolerance);
        seekBarTolerance.setEnabled(false);

        seekBarMarginError = (SeekBar) bodyView.findViewById(R.id.margin_error_seekbar);
        seekBarMarginError.setOnSeekBarChangeListener(this);
        seekBarMarginError.setProgress(marginError);
        seekBarMarginError.setEnabled(false);

        checkBoxAdvancedMode = (CheckBox) bodyView.findViewById(R.id.advanced_mode_checkbox);
        checkBoxAdvancedMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                Logger.d(TAG, "onCheckedChanged: checked:" + checked);
                dbToleranceEditText.setEnabled(checked);
                seekBarTolerance.setEnabled(checked);
                seekBarMarginError.setEnabled(checked);
                advancedModeEnable = checked;
            }
        });

        showHistoryButton = (Button) bodyView.findViewById(R.id.history_protection_button);
        showHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHistory();
            }
        });
        showHistoryButton.setVisibility(View.INVISIBLE);

        progressBarPandwarfRunning = (ProgressBar) bodyView.findViewById(R.id.pandwarf_running_progressbar);
        progressBarPandwarfRunning.setIndeterminate(true);
        progressBarPandwarfRunning.setVisibility(View.INVISIBLE);

        layoutStartStopProtection = (LinearLayout) bodyView.findViewById(R.id.start_protection_layout);
        startStopProtectionButton = (Button) bodyView.findViewById(R.id.start_stop_protection_button);

        startStopProtectionButton.setOnClickListener(this);
        startStopProtectionButton.setEnabled(false); // will be true when user connect the App to a PandwaRF.
        Button loadButton = (Button) bodyView.findViewById(R.id.load_configuration_button);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileManager.getInstance().load(getContext(), new GollumCallbackGetGeneric<Configuration>() {
                    @Override
                    public void done(Configuration configuration, GollumException e) {
                        if (configuration != null) {
                            // if the loaded config is not null, a message is showed and the loaded config is setted on the fragment
                            Toast toast = Toast.makeText(getContext(), "config loaded : " + configuration.getTitle(), Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                            toast.show();
                            setCurrentConfiguration(configuration);
                        }
                    }
                });
            }
        });

        Button saveButton = (Button) bodyView.findViewById(R.id.save_configuration_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshCurrentConfiguration(); // force refresh of the object currentConfiguration.
                new MaterialDialog.Builder(getContext())
                        .title(R.string.dialog_title_config_save)
                        .content(R.string.dialog_content_config_save)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            // When user click on "Ok"  or something similar like "Yes"...
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                String fileName = dialog.getInputEditText().getText().toString(); // get the entered name --> it's the configuration title
                                currentConfiguration.setTitle(fileName);
                                FileManager.getInstance().save(getContext(), fileName, currentConfiguration); // store the config.
                            }
                        })
                        .input("file name", "", new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                // nothing...
                            }
                        }).show();
            }
        });

        refreshCurrentConfiguration();
        return bodyView;
    }

    /**
     * Show dialog with a list of all events during the current protection session
     */
    private void showHistory() {
        new AlertDialog.Builder(getContext())
                .setTitle("History")
                .setNeutralButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        historyLogArrayList.clear();
                        dialogInterface.dismiss();
                        showHistoryButton.setText("No attacks detected");
                    }
                })
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setAdapter(new LogAdapter(getContext(), 0, historyLogArrayList), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .create().show();
    }

    ///////////////// CONFIGURATION
    /**
     * Set all widgets from the given Configuration values, this Configuration was loaded.
     * @param configuration
     */
    private void setCurrentConfiguration(Configuration configuration) {
        Logger.d(TAG, "setCurrentConfiguration():");
        Logger.d(TAG, "setCurrentConfiguration: " + configuration.toString());
        currentConfiguration = configuration;
        this.frequencyEditText.setText(String.valueOf(configuration.getFrequency()));
        this.dbToleranceEditText.setText(String.valueOf(configuration.getDbTolerance()));
        setPeakTolerance(configuration.getPeakTolerance());
        setMarginError(configuration.getMarginError());
    }

    /**
     * Set the widgets values (freq, db, peak tolerance, margin error) into the currentConfiguration object
     */
    private void refreshCurrentConfiguration() {
        if (Tools.isValidFrequency(frequencyEditText.getText().toString())) {
            currentConfiguration.setFrequency(Integer.valueOf(frequencyEditText.getText().toString()));
        }

        if (Tools.isValidDb(dbToleranceEditText.getText().toString())) {
            currentConfiguration.setDbTolerance(Integer.valueOf(dbToleranceEditText.getText().toString()));
        }

        currentConfiguration.setPeakTolerance(getPeakTolerance());
        currentConfiguration.setMarginError(getMargingError());
    }


    ///////////////// Protection
    /**
     * Start protection, check if frequency && decibel values are regular.
     * If not a toast will be showed.
     *
     * Otherwise an event "START_PROTECTION" will be sent to the main activity.
     */
    private void startProtection() {

        if (!Tools.isValidDb(dbToleranceEditText.getText().toString()) ||
                !Tools.isValidFrequency(frequencyEditText.getText().toString())) {
            Toast toast = Toast.makeText(getContext(), "wrong parameters", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            return;
        }
        refreshCurrentConfiguration();
        historyLogArrayList.clear();
        resetFragment();
        showHistoryButton.setVisibility(View.VISIBLE);
        showHistoryButton.setText(getString(R.string.no_attack_detected_flat_button));
        currentState = PROTECTION_ON_RUN;
        progressBarPandwarfRunning.setVisibility(View.VISIBLE);
        startStopProtectionButton.setText(R.string.stop_protection_text_button); // change text value
        HashMap<String, String> parameters = new HashMap<>(); // parameters will contains the frequency, db, peak tolerance & margin error values.

        parameters.put(Parameter.FREQUENCY.toString(), String.valueOf(currentConfiguration.getFrequency()));
        parameters.put(Parameter.RSSI_VALUE.toString(), String.valueOf(currentConfiguration.getDbTolerance()));
        parameters.put(Parameter.PEAK_TOLERANCE.toString(), String.valueOf(currentConfiguration.getPeakTolerance()));
        parameters.put(Parameter.MARGIN_ERROR.toString(), String.valueOf(currentConfiguration.getMarginError()));
        EventBus.getDefault().postSticky(new ActionEvent(Action.START_PROTECTION, parameters));
        addLog(HistoryLog.WARNING_LEVEL.LOW, "Protection started");

    }

    /**
     * Reset the fragment and send "STOP_PROTECTION" event to the mainActivity.
     */
    private void stopProtection() {
        resetFragment();
        EventBus.getDefault().postSticky(new ActionEvent(Action.STOP_PROTECTION, ""));
    }

    /**
     * Start the process calculating the right parameters.
     */
    private void startScan() {
        refreshCurrentConfiguration(); // update currentConfiguration object
        resetFragment(); // reset View
        showHistoryButton.setVisibility(View.INVISIBLE);
        currentState = SCAN_ON_RUN; // now we are in SCAN_ON_RUN mode.
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(Parameter.FREQUENCY.toString(), String.valueOf(currentConfiguration.getFrequency()));
        startStopProtectionButton.setText(getString(R.string.stop_scan_text_button));

        progressBarPandwarfRunning.setVisibility(View.VISIBLE);
        EventBus.getDefault().postSticky(new ActionEvent(Action.START_FAST_PROTECTION_ANALYZER, parameters));
    }

    private void stopScan() {
        Logger.d(TAG, "stopScan");
        resetFragment();
        EventBus.getDefault().postSticky(new ActionEvent(Action.STOP_FAST_PROTECTION_ANALYZER, ""));
    }

    ///////////////// GETTER/SETTER
    /**
     * Returns value of the "peak tolerance" seekbar
     * @return
     */
    private int getPeakTolerance() {
        return tolerance;
    }


    public void setPeakTolerance(int peakTolerance) {
        this.seekBarTolerance.setProgress(peakTolerance);
    }

    /**
     * Returns value of the "margin error" seekbar
     * @return
     */
    private int getMargingError() {
        return marginError;
    }

    /**
     * @param marginError
     */
    public void setMarginError(int marginError) {
        this.seekBarMarginError.setProgress(marginError);
    }


    private void addLog(HistoryLog.WARNING_LEVEL level, String message) {
        historyLogArrayList.add(new HistoryLog(level, Tools.getCurrentTime(), message));
    }


    /**
     * Reset color, text, and OnClick callback of startStopProtectionButton button
     *
     */
    private void resetFragment() {
        Logger.d(TAG, "resetFragment():");
        startStopProtectionButton.setText(R.string.start_protection_text_button);
        progressBarPandwarfRunning.setVisibility(View.INVISIBLE);
        currentState = NO_PROCESS_ON_RUN;
    }

    private void frequencyChanged() {
        String frequency = frequencyEditText.getText().toString();
        if (Tools.isValidFrequency(frequency) && !frequencyChangeEventReceived) {
            Logger.d(TAG, "post new frequency");
            HashMap<String, String> parameters = new HashMap<String, String>();
            parameters.put(Parameter.FREQUENCY.toString(), frequency);
            EventBus.getDefault().postSticky(new StateEvent(State.FREQUENCY_SELECTED, parameters));
        } else {
            frequencyChangeEventReceived = false;
        }
    }

    @Override
    public void onClick(View view) {
        Logger.d(TAG, "onClick");
        if (view == startStopProtectionButton) {
            Logger.d(TAG, "onClick: mButtonScanProtection");
            Logger.d(TAG, "onClick: mButtonScanProtection: currentState: -> " + currentState + "; checkBoxAdvancedMode: -> " + checkBoxAdvancedMode.isChecked());
            if (currentState == NO_PROCESS_ON_RUN && !advancedModeEnable) {
                    new MaterialDialog.Builder(getContext())
                            .title("Protection")
                            .content("Use the current parameters ? ")
                            .positiveText("Yes")
                            .negativeText("No, check for me the best parameters")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    // Yes
                                    Logger.d(TAG, "onClick: startProtection");
                                    startProtection();
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    // No
                                    Logger.d(TAG, "onClick: startScan");
                                    startScan();
                                }
                            }).show();
            }  else if (currentState == NO_PROCESS_ON_RUN && advancedModeEnable){
                Logger.d(TAG, "onClick: startProtection");
                startProtection();
            } else if (currentState == SCAN_ON_RUN) {
                Logger.d(TAG, "onClick: stopScan");
                stopScan();
            } else if (currentState == PROTECTION_ON_RUN) {
                Logger.d(TAG, "onClick: stopProtection");
                stopProtection();
            }
        }


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
            /**
             * Event from DongleCallbacks {@link com.example.aventador.protectalarm.callbacks.DongleCallbacks}
             */
            case CONNECTED: {
                Logger.d(TAG, "event: CONNECTED");
                startStopProtectionButton.setEnabled(true);
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case DISCONNECTED: {
                Logger.d(TAG, "event: DISCONNECTED");
                startStopProtectionButton.setEnabled(false);
                resetFragment();
                break;
            }

            /**
             * Event from ThresholdFragment {@link ThresholdFragment}
             */
            case FREQUENCY_SELECTED: {
                Logger.d(TAG, "event: FREQUENCY_SELECTED");
                frequencyChangeEventReceived = true;
                String frequencySelected = stateEvent.getParameters().getString(Parameter.FREQUENCY.toString());
                this.frequencyEditText.setText(frequencySelected);
                frequencyChangeEventReceived = false;
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case FAST_PROTECTION_ANALYZER_DONE: {
                Logger.d(TAG, "event: FAST_PROTECTION_ANALYZER_DONE");
                String configurationSerialized = stateEvent.getParameters().getString(Parameter.CONFIGURATION.toString());
                Configuration configuration = new Gson().fromJson(configurationSerialized, Configuration.class);
                if (configuration != null) {
                    setCurrentConfiguration(configuration);
                    startProtection();
                }
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case SEARCH_OPTIMAL_PEAK_DONE: {
                Logger.d(TAG, "event: SEARCH_OPTIMAL_PEAK_DONE");
                String dbTolerance  = stateEvent.getParameters().getString(Parameter.RSSI_VALUE.toString());
                this.dbToleranceEditText.setText(dbTolerance);
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case ATTACK_DETECTED: {
                Logger.d(TAG, "event: ATTACK_DETECTED");
                addLog(HistoryLog.WARNING_LEVEL.HIGH, "Attack detected");
                int nbAttacksDetected = 0;
                for (HistoryLog historyLog : historyLogArrayList) {
                    if (historyLog.getWarningLevel().equals(HistoryLog.WARNING_LEVEL.HIGH)) {
                        nbAttacksDetected++;
                    }
                }
                showHistoryButton.setText("" + nbAttacksDetected + " Attacks detected !");
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case PROTECTION_FAIL: {
                Logger.d(TAG, "event: PROTECTION_FAIL");
                resetFragment();
                break;
            }
        }
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

    protected enum GuardianState {
        NO_PROCESS_ON_RUN,
        SCAN_ON_RUN,
        PROTECTION_ON_RUN;
    }
}
