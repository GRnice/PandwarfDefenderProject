package com.example.aventador.protectalarm;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.example.aventador.protectalarm.customViews.SettingsSubView;
import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.storage.Configuration;
import com.example.aventador.protectalarm.storage.FileManager;
import com.example.aventador.protectalarm.tools.Logger;
import com.example.aventador.protectalarm.tools.Tools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;


public class GuardianFragment extends Fragment {

    private final static String TAG = "GuardianFragment";
    private LinearLayout layoutStartStopProtection;
    private BootstrapButton startStopProtectionButton;
    private TextView frequencyTextView;
    private EditText frequencyEditText;
    private TextView dbToleranceTextView;
    private EditText dbToleranceEditText;
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
        frequencyTextView = (TextView) bodyView.findViewById(R.id.frequency_guardian_textview);
        frequencyEditText = (EditText) bodyView.findViewById(R.id.frequency_guardian_edittext);
        dbToleranceTextView = (TextView) bodyView.findViewById(R.id.db_tolerance_guardian_textview);
        dbToleranceEditText = (EditText) bodyView.findViewById(R.id.dbtolerance_guardian_edittext);

        layoutStartStopProtection = (LinearLayout) bodyView.findViewById(R.id.start_protection_layout);
        startStopProtectionButton = (BootstrapButton) bodyView.findViewById(R.id.start_stop_protection_button);

        startStopProtectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startProtection(); // when user click on this button , the protection is started.

            }
        });
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

    /**
     * Set all widgets from the given Configuration values, this Configuration was loaded.
     * @param configuration
     */
    private void setCurrentConfiguration(Configuration configuration) {
        this.frequencyEditText.setText(String.valueOf(configuration.getFrequency()));
        this.dbToleranceEditText.setText(String.valueOf(configuration.getDbTolerance()));
        CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        customPagerAdapter.getSettingsSubView().setPeakTolerance(configuration.getPeakTolerance());
        customPagerAdapter.getSettingsSubView().setMarginError(configuration.getMarginError());
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
        CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        currentConfiguration.setPeakTolerance(customPagerAdapter.getSettingsSubView().getPeakTolerance());
        currentConfiguration.setMarginError(customPagerAdapter.getSettingsSubView().getMarginError());
    }

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
     * Returns value of the "peak tolerance" seekbar
     * @return
     */
    private int getPeakTolerance() {
        CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        SettingsSubView settingsSubView = customPagerAdapter.getSettingsSubView();
        return settingsSubView.getPeakTolerance();
    }

    private void addLog(HistoryLog.WARNING_LEVEL level, String message) {
        CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        HistoryLog historyLog = new HistoryLog(level, Tools.getCurrentTime(), message);
        customPagerAdapter.getHistorySubView().addLog(historyLog);
    }

    /**
     * Returns value of the "margin error" seekbar
     * @return
     */
    private int getMargingError() {
        CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        SettingsSubView settingsSubView = customPagerAdapter.getSettingsSubView();
        return settingsSubView.getMarginError();
    }

    /**
     * Reset the fragment and send "STOP_PROTECTION" event to the mainActivity.
     */
    private void stopProtection() {
        resetFragment();
        EventBus.getDefault().postSticky(new ActionEvent(Action.STOP_PROTECTION, ""));
    }

    /**
     * Reset color, text, and OnClick callback of startStopProtectionButton button
     *
     */
    private void resetFragment() {
        startStopProtectionButton.setBackgroundColor(getResources().getColor(R.color.successColor)); // success color
        startStopProtectionButton.setText(R.string.start_protection_text_button);
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
                String frequencySelected = stateEvent.getParameters().getString(Parameter.FREQUENCY.toString());
                this.frequencyEditText.setText(frequencySelected);
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
}
