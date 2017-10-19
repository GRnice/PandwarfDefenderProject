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
        currentConfiguration = new Configuration();
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
        customPagerAdapter.getSettingsSubView().setOnLoadConfig(new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                FileManager.getInstance().load(getContext(), new GollumCallbackGetGeneric<Configuration>() {
                    @Override
                    public void done(Configuration configuration, GollumException e) {
                        if (configuration != null) {
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
            @Override
            public void done(boolean b) {
                refreshCurrentConfiguration();
                new MaterialDialog.Builder(getContext())
                        .title("Save config")
                        .content("Set file name please")
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                String fileName = dialog.getInputEditText().getText().toString();
                                currentConfiguration.setTitle(fileName);
                                FileManager.getInstance().save(getContext(), fileName, currentConfiguration);
                            }
                        })
                        .input("file name", "", new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {

                            }
                        }).show();
            }
        });
        viewPager.setAdapter(customPagerAdapter);
        viewPager.setOffscreenPageLimit(customPagerAdapter.getCount());
        refreshCurrentConfiguration();
        return bodyView;
    }

    private void setCurrentConfiguration(Configuration configuration) {
        this.frequencyEditText.setText("" + configuration.getFrequency());
        this.dbToleranceEditText.setText("" + configuration.getDbTolerance());
        CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        customPagerAdapter.getSettingsSubView().setPeakTolerance(configuration.getPeakTolerance());
        customPagerAdapter.getSettingsSubView().setMarginError(configuration.getMarginError());
    }

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

    private void startProtection() {
        if (!Tools.isValidDb(dbToleranceEditText.getText().toString()) ||
                !Tools.isValidFrequency(frequencyEditText.getText().toString())) {
            Toast toast = Toast.makeText(getContext(), "wrong parameters", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            return;
        }
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
                startStopProtectionButton.setEnabled(true);
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case DISCONNECTED: {
                startStopProtectionButton.setEnabled(false);
                resetFragment();
                break;
            }

            /**
             * Event from ThresholdFragment {@link ThresholdFragment}
             */
            case FREQUENCY_SELECTED: {
                String frequencySelected = stateEvent.getParameters().getString(Parameter.FREQUENCY.toString());
                this.frequencyEditText.setText(frequencySelected);
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case SEARCH_OPTIMAL_PEAK_DONE: {
                String dbTolerance  = stateEvent.getParameters().getString(Parameter.RSSI_VALUE.toString());
                this.dbToleranceEditText.setText(dbTolerance);
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case ATTACK_DETECTED: {
                String dateAttack = stateEvent.getParameter(Parameter.DATE);
                CustomPagerAdapter customPagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
                HistoryLog historyLog = new HistoryLog(HistoryLog.WARNING_LEVEL.HIGH, dateAttack);
                customPagerAdapter.getHistorySubView().addLog(historyLog);
                if (!historyViewIsShow()) {
                    viewPager.setCurrentItem(HISTORY_PAGE);
                }
                break;
            }

            /**
             * Event from Main2Activity {@link Main2Activity}
             */
            case PROTECTION_FAIL: {
                resetFragment();
                break;
            }
        }
    }
}
