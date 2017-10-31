package com.example.aventador.protectalarm.process;

import android.app.Activity;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.callbacks.GollumCallbackGetConfiguration;
import com.example.aventador.protectalarm.process.Runners.DiscoverThread;
import com.example.aventador.protectalarm.process.Runners.GuardianThread;
import com.example.aventador.protectalarm.storage.Configuration;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Giangrasso on 25/09/2017.
 */

/**
 * Pandwarf is responsible for :
 *                              calculating the tolerance threshold making it possible to distinguish a brute force attack.
 *                              monitor if an attack is in progress.
 *
 *
 */
public class Pandwarf {

    private static int DELAY_PACKETS_RSSI_MS = 100;
    private static int FREQUENCY_BY_CHANNELS = 25000;
    private static int NB_CHANNELS = 5; // 5 by default.
    private static int NB_SCAN_BY_SEQUENCE = 4; // interval -> [1 , MAX_INT] , in this case : 4 scan for each sequence.

    /*
    For Guardian mode
     */
    private static int GUARDIAN_NB_SEQUENCES = 25; // 20 sequences, one sequence -> 4 scans.

    /*
    For discovery mode
     */
    private static int DISCOVERY_NB_SEQUENCES = 8; // 8 sequences of scan will be executed

    /*
    For fast protection analyzer
     */
    private static int FAST_PROTECTION_ANALYZER_NB_SEQUENCES = GUARDIAN_NB_SEQUENCES;

    private static Pandwarf instance;
    private boolean isConnected;
    private AtomicBoolean guardianIsRunning;
    private AtomicBoolean discoverIsRunning;
    private AtomicBoolean fastProtectionAnalyseIsRunning;

    private AtomicBoolean specanIsRunning;

    private FastProtectionAnalyser fastProtectionAnalyser;
    private GuardianThread guardianThread;
    private DiscoverThread discoverThread;
    private static final String TAG = "Pandwarf";

    public static Pandwarf getInstance() {
        if (instance == null) {
            instance = new Pandwarf();
        }

        return instance;
    }

    private Pandwarf() {
        reset();
    }

    private boolean reset() {
        if (isConnected()) {
            Logger.e(TAG, "reset: dongle must be disconnected");
            return false;
        }
        Jammer.getInstance().init(); // init Jammer
        guardianIsRunning = new AtomicBoolean(false);
        discoverIsRunning = new AtomicBoolean(false);
        fastProtectionAnalyseIsRunning = new AtomicBoolean(false);
        specanIsRunning = new AtomicBoolean(false);
        isConnected = false;
        return true;
    }

    //  55f3395f1e6713f4bef36c4e5a41b434

    /**
     * Start the protection, is called when user click on button "start protection" of guardian fragment.
     *
     * - cbStartGuardianDone is called when the guardian is started (true) or not (false).
     * - cbAttackDetected is called when the guardian detects an attack.
     * @param activity // TODO: when Pandwarf SDK will be upgraded, demand a Context.
     * @param frequency
     * @param dbTolerance
     * @param cbStartGuardianDone Called when guardian is started.
     * @param cbAttackDetected Called when an attack is detected.
     * @return
     */
    public void startGuardian(final Activity activity, final int frequency, final int dbTolerance,
                              final int peakTolerance, final int marginError,
                              final GollumCallbackGetBoolean cbStartGuardianDone, final GollumCallbackGetBoolean cbAttackDetected) {
        Logger.d(TAG, "startGuardian: discoverIsRunning.get():" +
                discoverIsRunning.get() + " guardianIsRunning ?: " +
                guardianIsRunning.get() + " specan run:" + specanIsRunning);

        if (!isAvailable(activity) || !guardianIsRunning.compareAndSet(false, true)) {
            // if PandwaRF is not isAvailable for a new task  or  if the guardian is already on run.
            Logger.e(TAG, "startGuardian: pandwarf not ready");
            cbStartGuardianDone.done(false);
            return;
        }
        Logger.d(TAG, "Start Guardian");
        startSpecan(activity, frequency, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean startSuccess) {
                Logger.d(TAG, "startGuardian: success ? " + startSuccess);
                if (startSuccess) {
                    guardianThread = new GuardianThread(activity, GUARDIAN_NB_SEQUENCES, NB_SCAN_BY_SEQUENCE, NB_CHANNELS, dbTolerance, peakTolerance, marginError, cbAttackDetected);
                    guardianThread.start(true);
                } else {
                    Logger.e(TAG, "startGuardian: specan not started");
                }
                cbStartGuardianDone.done(startSuccess);
            }
        });
    }

    /**
     * start searching the threshold value in decibel
     * - cbStartDiscoveryDone called with "true"  when the specan is started
     * - cbDiscoveryDone called when a threshold value is found
     * @param activity
     * @param frequency
     * @param cbStartDiscoveryDone Called when discovery is started
     * @param cbDiscoveryDone Called when discovery is done, db tolerance value will be given.
     * @return
     */
    public void startDiscovery(final Activity activity, int frequency, final GollumCallbackGetBoolean cbStartDiscoveryDone, final GollumCallbackGetInteger cbDiscoveryDone) {
        Logger.d(TAG, "startDiscovery: " +
                "guardianIsRunning.get():" + guardianIsRunning.get() +
                " discoverIsRunning ?: " + discoverIsRunning.get() +
                " specan run:" + specanIsRunning);

        if (!isAvailable(activity) || !discoverIsRunning.compareAndSet(false, true)) {
            Logger.e(TAG, "startDiscovery: pandwarf not ready");
            cbStartDiscoveryDone.done(false);
            return;
        }
        Logger.d(TAG, "Start discovery");
        startSpecan(activity, frequency, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean success) {
                if (success) {
                    discoverThread = new DiscoverThread(activity, NB_SCAN_BY_SEQUENCE, DISCOVERY_NB_SEQUENCES, NB_CHANNELS, cbDiscoveryDone);
                    discoverThread.start();
                } else {
                    discoverIsRunning.set(false); // reset to false.
                }
                cbStartDiscoveryDone.done(success);


            }
        });
    }

    /**
     *
     * @param activity
     * @param frequency
     * @param cbFastProtectionAnalyzerStarted Called when Fast protection analyzer is started.
     * @param cbFastProtectionAnalyseDone Called when Fast protection analyzer is done.
     */
    public void startFastProtectionAnalyser(final Activity activity, final int frequency,
                                            final GollumCallbackGetBoolean cbFastProtectionAnalyzerStarted, final GollumCallbackGetConfiguration cbFastProtectionAnalyseDone) {
        if (!isAvailable(activity) || !fastProtectionAnalyseIsRunning.compareAndSet(false, true)) {
            Logger.e(TAG, "startFastProtectionAnalyser: pandwarf not ready");
            cbFastProtectionAnalyzerStarted.done(false);
            return;
        }

        Logger.d(TAG, "start fast protection analyser");
        startSpecan(activity, frequency, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean specanStartSuccess) {
                if (specanStartSuccess) {
                    fastProtectionAnalyser = new FastProtectionAnalyser(activity, frequency,
                            NB_SCAN_BY_SEQUENCE, FAST_PROTECTION_ANALYZER_NB_SEQUENCES, NB_CHANNELS, new GollumCallbackGetConfiguration() {
                        @Override
                        public void done(final boolean success, final Configuration configuration) {
                            cbFastProtectionAnalyseDone.done(success, configuration);
                            stopSpecan(activity, new GollumCallbackGetBoolean() {
                                @Override
                                public void done(boolean successStop) {
                                    if (successStop) {
                                        Logger.d(TAG, "startFastProtectionAnalyser: specan is stopped");
                                    } else {
                                        Logger.e(TAG, "startFastProtectionAnalyser: specan isn't stopped");
                                    }

                                }
                            });
                        }
                    });
                    fastProtectionAnalyser.start();
                } else {
                    fastProtectionAnalyseIsRunning.set(false);
                }
                cbFastProtectionAnalyzerStarted.done(specanStartSuccess);
            }
        });

    }

    public void startJamming(Activity activity, int frequency,  GollumCallbackGetBoolean cbStartDone, GollumCallbackGetBoolean cbJammingTerminate) {
        Logger.d(TAG, "startJamming()");
        Jammer.getInstance().startJamming(activity, frequency, cbStartDone, cbJammingTerminate);
    }

    /**
     * Try to start specan in background
     * @param activity
     * @param frequency
     * @param cbDone called in all cases, success/failure to start the specan.
     */
    private void startSpecan(final Activity activity, int frequency, final GollumCallbackGetBoolean cbDone) {
        Logger.d(TAG, "startSpecan()");
        if (!specanIsRunning.compareAndSet(false, true)) {
            Logger.e(TAG, "specan already started");
            cbDone.done(true);
            return;
        }
        GollumDongle.getInstance(activity).rfSpecanStart(0, frequency, FREQUENCY_BY_CHANNELS, NB_CHANNELS, DELAY_PACKETS_RSSI_MS, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                /*
                If i < 0 the specan doesn't correctly started
                If i == 0 specan correctly started. -> specanIsRunning will be set to true.
                */
                if (i < 0) {
                    Logger.e(TAG, "startSpecan: specan not started");
                    specanIsRunning.set(false); // Finally, specan can't be started. retry ...
                    cbDone.done(false);
                } else {
                    Logger.d(TAG, "specan started");

                    cbDone.done(true);
                }
            }
        });
    }

    /**
     * Try to stop specan in background
     * @param activity
     * @param cbStopDone
     */
    private void stopSpecan(final Activity activity, final GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopSpecan()");
        if (!specanIsRunning.compareAndSet(true, false)) {
            Logger.e(TAG, "specan is already stopped");
            cbStopDone.done(true);
            return;
        }
        GollumDongle.getInstance(activity).rfSpecanStop(0, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                if (i < 0) {
                    /*
                    If i < 0 the specan doesn't correctly stopped
                    If i == 0 specan correctly stopped.
                     */
                    Logger.e(TAG, "startSpecan: specan not stopped");
                    specanIsRunning.set(true); // not stopped finally, reset to true specanIsRunning boolean.
                    cbStopDone.done(false);
                } else {
                    Logger.d(TAG, "specan is stopped");

                    cbStopDone.done(true);
                }
            }
        });
    }

    /**
     * stop searching the threshold value in decibel
     * @param activity
     */
    public void stopDiscovery(Activity activity, GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopDiscovery()");
        Logger.d(TAG, "discoveryIsRunning ? : " + discoverIsRunning.get());
        if (!discoverIsRunning.compareAndSet(true, false)) {
            Logger.e(TAG, "stopDiscovery: already stopped");
            cbStopDone.done(true);
        }
        if (discoverThread != null) {
            discoverThread.kill();
        }

        stopSpecan(activity, cbStopDone);

    }

    /**
     * Stop the guardian if it's running
     * @param cbStopDone called when all process are stopped.
     */
    public void stopGuardian(Activity activity, final GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopGuardian()");
        Logger.d(TAG, "guardianIsRunning ? : " + guardianIsRunning.get());
        if (!guardianIsRunning.compareAndSet(true, false)) {
            Logger.e(TAG, "stopGuardian: guardian already stopped");
        } else if (guardianThread != null) {
            Logger.d(TAG, "stopGuardian: kill");
            guardianThread.kill();
            Logger.d(TAG, "stopGuardian: end kill");
        }
        stopSpecan(activity, cbStopDone);
    }

    public void stopFastProtectionAnalyzer(final Activity activity, final GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopFastProtectionAnalyzer()");
        Logger.d(TAG, "protection analyzer is running ? : " + fastProtectionAnalyseIsRunning.get());
        if (! fastProtectionAnalyseIsRunning.compareAndSet(true, false)) {
            Logger.e(TAG, "stopFastProtectionAnalyzer: analyzer already stopped");
            cbStopDone.done(true); // true because fastProtectionAnalyzer is stopped.
            return;
        }

        fastProtectionAnalyser.stop(new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                Logger.d(TAG, "fast protection routine is breaked");
                Logger.d(TAG, "now stopSpecan will be called");
                stopSpecan(activity, cbStopDone);
            }
        });
    }

    public void stopJamming(final Activity activity, boolean stopByUser, final GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopJamming()");
        Jammer.getInstance().stopJamming(activity, stopByUser, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                stopSpecan(activity, cbStopDone);
            }
        });
    }

    /**
     * Indicate if the dongle has stopped specan
     * @return
     */
    public boolean specanIsRunning() {
        return specanIsRunning.get();
    }

    /**
     * Indicate if the discovery thread is running
     * @return
     */
    public boolean discoveryProcessIsRunning() {
        return discoverIsRunning.get();
    }

    /**
     * Indicate if the gardian thread is running
     * @return
     */
    public boolean guardianProcessIsRunning() {
        return guardianIsRunning.get();
    }

    /**
     * Indicate if user can call startDiscovery or startGuardian
     * @return
     */
    public boolean isAvailable(Activity activity) {
        Logger.d(TAG, "isAvailable()");
        Logger.d(TAG, "isAvailable(): isConnected" + isConnected);
        Logger.d(TAG, "isAvailable(): isDeviceConnected()" + GollumDongle.getInstance(activity).isDeviceConnected());
        Logger.d(TAG, "isAvailable(): specanIsRunning()" + specanIsRunning());
        Logger.d(TAG, "isAvailable() discoveryProcessIsRunning()" + discoveryProcessIsRunning());
        Logger.d(TAG, "isAvailable() guardianProcessIsRunning" + guardianProcessIsRunning());
        Logger.d(TAG, "isAvailable() Jammer.getInstance().isRunning()" + Jammer.getInstance().isRunning());
        Logger.d(TAG, "isAvailable() fastProtectionAnalyseIsRunning.get()" + fastProtectionAnalyseIsRunning.get());

        return (isConnected && GollumDongle.getInstance(activity).isDeviceConnected() &&
                !specanIsRunning() && !discoveryProcessIsRunning() && !guardianProcessIsRunning() && !Jammer.getInstance().isRunning() &&
                !fastProtectionAnalyseIsRunning.get());
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean state) {
        isConnected = state;
    }

    /**
     * close the device.
     * setConnected(false) will be called automatically
     * @param activity
     */
    public void close(Activity activity) {
        GollumDongle.getInstance(activity).closeDevice();
    }
}
