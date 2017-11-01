package com.example.aventador.protectalarm;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.comthings.gollum.api.gollumandroidlib.GollumException;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetGeneric;
import com.example.aventador.protectalarm.customViews.CustomPagerAdapter;
import com.example.aventador.protectalarm.customViews.HistoryLog;
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

import java.util.HashMap;

import static com.example.aventador.protectalarm.GuardianFragment.GuardianState.NO_PROCESS_ON_RUN;
import static com.example.aventador.protectalarm.GuardianFragment.GuardianState.PROTECTION_ON_RUN;
import static com.example.aventador.protectalarm.GuardianFragment.GuardianState.SCAN_ON_RUN;


public class GuardianFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private final static String TAG = "GuardianFragment";

    private GuardianState currentState;

    private boolean frequencyChangeEventReceived;

    private LinearLayout layoutStartStopProtection;
    private BootstrapButton startStopProtectionButton;

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

    private ProgressBar progressBarPandwarfRunning;

    private ViewPager viewPager;
    private Configuration currentConfiguration;

    private final int SETTINGS_PAGE = 0;
    private final int HISTORY_PAGE = 1;


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
                dbToleranceEditText.setEnabled(checked);
                seekBarTolerance.setEnabled(checked);
                seekBarMarginError.setEnabled(checked);
            }
        });

        progressBarPandwarfRunning = (ProgressBar) bodyView.findViewById(R.id.pandwarf_running_progressbar);
        progressBarPandwarfRunning.setIndeterminate(true);
        progressBarPandwarfRunning.setVisibility(View.INVISIBLE);

        layoutStartStopProtection = (LinearLayout) bodyView.findViewById(R.id.start_protection_layout);
        startStopProtectionButton = (BootstrapButton) bodyView.findViewById(R.id.start_stop_protection_button);

        startStopProtectionButton.setOnClickListener(this);
        startStopProtectionButton.setEnabled(false); // will be true when user connect the App to a PandwaRF.
        viewPager = (ViewPager) bodyView.findViewById(R.id.guardian_view_pager); // view pager store two layout (Settings layout & History layout)
        CustomPagerAdapter customPagerAdapter = new CustomPagerAdapter(this.getContext(), viewPager);
        customPagerAdapter.getSettingsSubView().setOnLoadConfig(new GollumCallbackGetBoolean() {
            // When user want to load a session.
            @Override
            public void done(boolean b) {
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

        customPagerAdapter.getSettingsSubView().setOnSaveConfig(new GollumCallbackGetBoolean() {
            // When user want to store a session.
            @Override
            public void done(boolean b) {
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

        viewPager.setAdapter(customPagerAdapter);
        viewPager.setOffscreenPageLimit(customPagerAdapter.getCount());
        refreshCurrentConfiguration();
        return bodyView;
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
        progressBarPandwarfRunning.setVisibility(View.VISIBLE);
        startStopProtectionButton.setBackgroundColor(getResources().getColor(R.color.warningColor)); // warning color
        startStopProtectionButton.setText(R.string.stop_protection_text_button); // change text value
        HashMap<String, String> parameters = new HashMap<>(); // parameters will contains the frequency, db, peak tolerance & margin error values.

        parameters.put(Parameter.FREQUENCY.toString(), String.valueOf(currentConfiguration.getFrequency()));
        parameters.put(Parameter.RSSI_VALUE.toString(), String.valueOf(currentConfiguration.getDbTolerance()));
        parameters.put(Parameter.PEAK_TOLERANCE.toString(), String.valueOf(currentConfiguration.getPeakTolerance()));
        parameters.put(Parameter.MARGIN_ERROR.toString(), String.valueOf(currentConfiguration.getMarginError()));
        startStopProtectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopProtection(); // button's behavior changed. now if user click on it, the protection will be stopped.
            }
        });
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

    private void startScan() {
        refreshCurrentConfiguration();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(Parameter.FREQUENCY.toString(), String.valueOf(currentConfiguration.getFrequency()));
        startStopProtectionButton.setText("Stop Scan");

        startStopProtectionButton.setBackgroundColor(getResources().getColor(R.color.bootstrap_brand_danger));
        progressBarPandwarfRunning.setVisibility(View.VISIBLE);
        EventBus.getDefault().postSticky(new ActionEvent(Action.START_FAST_PROTECTION_ANALYZER, parameters));
    }

    private void stopScan() {
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
        CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        HistoryLog historyLog = new HistoryLog(level, Tools.getCurrentTime(), message);
        customPagerAdapter.getHistorySubView().addLog(historyLog);
    }


    /**
     * Reset color, text, and OnClick callback of startStopProtectionButton button
     *
     */
    private void resetFragment() {
        Logger.d(TAG, "resetFragment():");
        startStopProtectionButton.setBackgroundColor(getResources().getColor(R.color.successColor)); // success color
        startStopProtectionButton.setText(R.string.start_protection_text_button);
        progressBarPandwarfRunning.setVisibility(View.INVISIBLE);
        currentState = NO_PROCESS_ON_RUN;
        startStopProtectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startProtection();
            }
        });
    }

    /**
     * Indicate if the sub view showed is the history page.
     * @return
     */
    private boolean historyViewIsShow() {
        return viewPager.getCurrentItem() == HISTORY_PAGE; // PAGE 1 is the historic view
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
            if (currentState == NO_PROCESS_ON_RUN && !checkBoxAdvancedMode.isChecked()) {
                if (checkBoxAdvancedMode.isChecked()) {
                    Logger.d(TAG, "onClick: startProtection");
                    startProtection(); // when user click on this button , the protection is started.
                } else {
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
                                    currentState = PROTECTION_ON_RUN;
                                    startProtection();
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    // No
                                    Logger.d(TAG, "onClick: startScan");
                                    currentState = SCAN_ON_RUN;
                                    startScan();
                                }
                            }).show();
                }
            }  else if (currentState == NO_PROCESS_ON_RUN && checkBoxAdvancedMode.isChecked()){
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

            case FAST_PROTECTION_ANALYZER_DONE: {
                Logger.d(TAG, "event: FAST_PROTECTION_ANALYZER_DONE");
                String configurationSerialized = stateEvent.getParameters().getString(Parameter.CONFIGURATION.toString());
                Configuration configuration = new Gson().fromJson(configurationSerialized, Configuration.class);
                if (configuration != null) {
                    setCurrentConfiguration(configuration);
                    resetFragment();
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
                if (!historyViewIsShow()) {
                    viewPager.setCurrentItem(HISTORY_PAGE);
                }
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
