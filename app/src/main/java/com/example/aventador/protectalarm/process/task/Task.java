package com.example.aventador.protectalarm.process.task;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.callbacks.GollumCallbackGetConfiguration;
import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.process.Pandwarf;
import com.example.aventador.protectalarm.storage.Configuration;
import com.example.aventador.protectalarm.tools.Logger;
import com.example.aventador.protectalarm.tools.Tools;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;

import static com.example.aventador.protectalarm.events.State.ATTACK_DETECTED;
import static com.example.aventador.protectalarm.events.State.PROTECTION_FAIL;

/**
 * Created by Giangrasso on 30/10/2017.
 * Task is an abstract class, it represent an action requested by the user.
 * Example: {@link StartFastProtection}
 * Each task is executed by {@link TaskPollManager}
 */
abstract class Task extends Thread {

    protected final String TAG = getClass().getSimpleName();

    protected GollumCallbackGetBoolean cbThreadDone;
    protected ActionEvent actionEvent;
    protected Activity activity;

    public Task(Activity activity, ActionEvent actionEvent, GollumCallbackGetBoolean cbThreadDone) {
        this.activity = activity;
        this.actionEvent = actionEvent;
        this.cbThreadDone = cbThreadDone;
    }

    /**
     * Run must be override and called by subclass, because we must apply a little delay between each executed task
     * in this case -> 1 second.
     */
    @Override
    public void run() {
        waiting(1000);
    }

    /**
     *
     * @param frequency
     * @param dbTolerance
     * @param peakTolerance
     * @param marginError
     * @param cbStartDone Called when jamming is started or not...
     */
    protected void startJamming(final String frequency, final String dbTolerance, final int peakTolerance, final int marginError,
                                final GollumCallbackGetBoolean cbStartDone) {
        Logger.d(TAG, "startJamming()");
        Pandwarf.getInstance().stopGuardian(activity, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                Logger.d(TAG, "START_JAMMING, stopGuardian : callback res :" + b);
                waiting(1000);
                Pandwarf.getInstance().startJamming(activity, Integer.valueOf(frequency),
                        new GollumCallbackGetBoolean() {
                            @Override
                            public void done(boolean startSuccess) {
                                if (startSuccess) {
                                    Logger.d(TAG, "Jamming started");
                                    toastShow("Jamming started");
                                    cbStartDone.done(true);
                                } else {
                                    Logger.d(TAG, "Can't start jamming");
                                    toastShow("Can't start jamming");
                                    startGuardian(frequency, dbTolerance, peakTolerance, marginError, new GollumCallbackGetBoolean() {
                                        @Override
                                        public void done(boolean startSuccess) {
                                            if (!startSuccess) {
                                                Logger.e(TAG, "guardian not started");
                                                toastShow("Fail to start protection");
                                                EventBus.getDefault().postSticky(new StateEvent(PROTECTION_FAIL, ""));
                                            } else {
                                                Logger.d(TAG, "guardian started");
                                                String message = "protection started\n frequency: " + frequency +
                                                        "\n db tolerance: " + dbTolerance +
                                                        "\n peak tolerance: " + peakTolerance +
                                                        "\n margin error: " + marginError;
                                                toastShow(message);

                                            }
                                            cbStartDone.done(startSuccess);
                                        }
                                    });
                                }
                            }
                        }, new GollumCallbackGetBoolean() {
                            @Override
                            public void done(boolean b) {
                                waiting(1000);
                                Logger.d(TAG, "START_JAMMING, startJamming : callback res :" + b);
                                startGuardian(frequency, dbTolerance, peakTolerance, marginError, new GollumCallbackGetBoolean() {
                                    @Override
                                    public void done(boolean startSuccess) {
                                        if (!startSuccess) {
                                            Logger.e(TAG, "guardian not started");
                                            toastShow("Fail to start protection");
                                            EventBus.getDefault().postSticky(new StateEvent(PROTECTION_FAIL, ""));
                                        } else {
                                            Logger.d(TAG, "guardian started");
                                            String message = "protection started\n frequency: " + frequency +
                                                    "\n db tolerance: " + dbTolerance +
                                                    "\n peak tolerance: " + peakTolerance +
                                                    "\n margin error: " + marginError;
                                            toastShow(message);

                                        }
                                    }
                                });
                            }
                        });
            }
        });
    }

    /**
     *
     * @param frequency
     * @param dbTolerance
     * @param peakTolerance
     * @param marginError
     * @param cbStartDone Called when guardian is started or not...
     */
    protected void startGuardian(final String frequency, final String dbTolerance, final int peakTolerance, final int marginError, GollumCallbackGetBoolean cbStartDone) {
        Logger.d(TAG, "startGuardian");
        Pandwarf.getInstance().startGuardian(activity, Integer.valueOf(frequency),
                Integer.valueOf(dbTolerance), peakTolerance, marginError, cbStartDone, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        // When an brute force attack is detected...
                        HashMap<String, String> parametersAttackDetected = new HashMap<>();
                        parametersAttackDetected.put(Parameter.DATE.toString(), Tools.getCurrentTime());
                        EventBus.getDefault().postSticky(new StateEvent(ATTACK_DETECTED, parametersAttackDetected));

                        HashMap<String, String> parameters = new HashMap<>();
                        parameters.put(Parameter.FREQUENCY.toString(), frequency);
                        parameters.put(Parameter.RSSI_VALUE.toString(), dbTolerance);
                        parameters.put(Parameter.PEAK_TOLERANCE.toString(), String.valueOf(peakTolerance));
                        parameters.put(Parameter.MARGIN_ERROR.toString(), String.valueOf(marginError));

                        EventBus.getDefault().postSticky(new ActionEvent(Action.START_JAMMING, parameters));
                    }
                });


    }

    /**
     *
     * @param frequency
     * @param cbStartDone Called when fast protect analyzer is started or not...
     */
    protected void startFastProtectionAnalyser(String frequency, GollumCallbackGetBoolean cbStartDone) {
        Logger.d(TAG, "startFastProtectionAnalyser");
        Pandwarf.getInstance().startFastProtectionAnalyser(activity, Integer.valueOf(frequency), cbStartDone, new GollumCallbackGetConfiguration() {
            @Override
            public void done(boolean success, Configuration configuration) {
                /*
                Called when a good configuration is found.
                or not.
                 */
                Pandwarf.getInstance().stopFastProtectionAnalyzer(activity, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean stopSuccess) {
                        if (!stopSuccess) {
                            toastShow("error when terminating fast protection analyzer");
                        } else {
                            toastShow("Fast protection analyzer stopped");
                        }
                    }
                });

                if (!success) {
                    toastShow("Rapid protection analyzer has no result");
                    EventBus.getDefault().postSticky(new StateEvent(com.example.aventador.protectalarm.events.State.FAST_PROTECTION_ANALYZER_FAIL, ""));
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toastShow("Rapid Protection analyzer found the right parameters");
                        }
                    });

                    HashMap<String, String> parameters = new HashMap<>();
                    parameters.put(Parameter.CONFIGURATION.toString(), new Gson().toJson(configuration));
                    EventBus.getDefault().postSticky(new StateEvent(com.example.aventador.protectalarm.events.State.FAST_PROTECTION_ANALYZER_DONE, parameters));
                }
            }
        });
    }

    /**
     *
     * @param frequency
     * @param cbStartDone Called when threshold discover is started or not...
     */
    protected void startThresholdSearch(String frequency, GollumCallbackGetBoolean cbStartDone) {
        Pandwarf.getInstance().startDiscovery(activity, Integer.valueOf(frequency), cbStartDone, new GollumCallbackGetInteger() {
            @Override
            public void done(final int rssi) {
                Log.d(TAG, "Threshold found");
                Pandwarf.getInstance().stopDiscovery(activity, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        HashMap<String, String> parameter = new HashMap<String, String>();
                        parameter.put(Parameter.RSSI_VALUE.toString(), String.valueOf(rssi));
                        EventBus.getDefault().postSticky(new StateEvent(com.example.aventador.protectalarm.events.State.SEARCH_OPTIMAL_PEAK_DONE, parameter));
                    }
                });
            }
        });
    }

    /**
     *
     * @param cbStopDone Called when fast protection analyzer is stopped
     */
    protected void stopFastProtectionAnalyzer(GollumCallbackGetBoolean cbStopDone) {
        Pandwarf.getInstance().stopFastProtectionAnalyzer(activity, cbStopDone);
    }

    /**
     *
     * @param cbStopDone Called when guardian is stopped
     */
    protected void stopGuardian(final GollumCallbackGetBoolean cbStopDone) {
        Pandwarf.getInstance().stopGuardian(activity, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                Pandwarf.getInstance().stopJamming(activity, true, cbStopDone);
            }
        });
    }

    /**
     *
     * @param cbStopDone Called when threshold discover is stopped
     */
    protected void stopThresholdSearch(GollumCallbackGetBoolean cbStopDone) {
        Pandwarf.getInstance().stopDiscovery(activity, cbStopDone);
    }

    /**
     * Simple delay function
     * @param ms millis
     */
    protected void waiting(int ms) {
        try {
            Thread.sleep(ms); // latency ... shit.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param message message to display
     */
    protected void toastShow(final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
            }
        });

    }
}
