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
 * It's impossible to start specan if jamming is running.
 * It's impossible to start jamming if specan is running.
 * It's impossible to start specan if the pandwarf has not stop correctly jamming or previous specan.
 * It's impossible to start jamming if the pandwarf has not stop correctly jamming or previous specan.
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
        guardianIsRunning = new AtomicBoolean(false);
        discoverIsRunning = new AtomicBoolean(false);
        fastProtectionAnalyseIsRunning = new AtomicBoolean(false);
        specanIsRunning = new AtomicBoolean(false);
        isConnected = false;
        return true;
    }

    /**
     * Start the protection, is called when user click on button "start protection" of guardian fragment.
     *
     * - cbStartGuardianDone is called when the guardian is started (true) or not (false).
     * - cbAttackDetected is called when the guardian detects an attack.
     * @param activity
     * @param frequency
     * @param dbTolerance
     * @param cbStartGuardianDone
     * @param cbAttackDetected
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
     * @param cbStartDiscoveryDone
     * @param cbDiscoveryDone
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
     * @param cbFastProtectionAnalyzerStarted
     * @param cbFastProtectionAnalyseDone
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

    /**
     *
     * @param activity
     * @param frequency
     * @param cbDone
     */
    private void startSpecan(final Activity activity, int frequency, final GollumCallbackGetBoolean cbDone) {
        GollumDongle.getInstance(activity).rfSpecanStart(0, frequency, FREQUENCY_BY_CHANNELS, NB_CHANNELS, DELAY_PACKETS_RSSI_MS, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                if (i < 0) {
                    Logger.e(TAG, "startSpecan: specan not started");
                    cbDone.done(false);
                } else {
                    Logger.d(TAG, "specan started");
                    specanIsRunning.set(true);
                    cbDone.done(true);
                }
            }
        });
    }

    /**
     *
     * @param activity
     */
    private void stopSpecan(final Activity activity, final GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stop specan");
        GollumDongle.getInstance(activity).rfSpecanStop(0, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                if (i < 0) {
                    Logger.e(TAG, "startSpecan: specan not stopped");
                    cbStopDone.done(false);
                } else {
                    Logger.d(TAG, "specan is stopped");
                    specanIsRunning.set(false);
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
        Logger.d(TAG, "stop discovery");
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
     * Stop the protection
     * @param activity
     * @param cbStopDone
     */
    public void stopGuardian(final Activity activity, GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopGuardian()");
        Logger.d(TAG, "guardianIsRunning ? : " + guardianIsRunning.get());
        if (!guardianIsRunning.compareAndSet(true, false)) {
            Logger.e(TAG, "stopGuardian: guardian already stopped");
            cbStopDone.done(true);
            return;
        }
        if (guardianThread != null) {
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
