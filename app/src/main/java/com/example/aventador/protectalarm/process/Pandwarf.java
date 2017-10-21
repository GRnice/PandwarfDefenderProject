package com.example.aventador.protectalarm.process;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Log;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.process.Runners.DiscoverThread;
import com.example.aventador.protectalarm.process.Runners.GuardianThread;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Aventador on 25/09/2017.
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
    private static int NB_SCAN_BY_SEQUENCE = 4; // interval -> [1 , MAX_INT] one sequence -> 20 scan for each sequence.

    /*
    For Guardian mode
     */
    private static int GUARDIAN_NB_SEQUENCES = 40; // one sequence -> 20 scans.

    /*
    For discovery mode
     */
    private static int DISCOVERY_NB_SEQUENCES = 8; // 8 sequences of scan will be executed

    private static Pandwarf instance;
    private boolean isConnected;
    private AtomicBoolean guardianIsRunning;
    private AtomicBoolean discoverIsRunning;
    private boolean specanIsRunning;
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
        guardianIsRunning = new AtomicBoolean(false);
        discoverIsRunning = new AtomicBoolean(false);
        specanIsRunning = false;
        isConnected = false;
    }

    /**
     *
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

        if (!isAvailableForNewStart(activity) || !guardianIsRunning.compareAndSet(false, true)) {
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
                    guardianThread.start();
                }
                cbStartGuardianDone.done(startSuccess);
            }
        });
    }

    /**
     *
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

        if (!isAvailableForNewStart(activity) || !discoverIsRunning.compareAndSet(false, true)) {
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
                }
                cbStartDiscoveryDone.done(success);


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
                specanIsRunning = true;
                Logger.d(TAG, "specan started");
                cbDone.done(true);
            }
        });
    }

    /**
     *
     * @param activity
     */
    private void stopSpecan(final Activity activity,final  GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stop specan");
        GollumDongle.getInstance(activity).rfSpecanStop(0, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                specanIsRunning = false;
                cbStopDone.done(true);
            }
        });
    }

    /**
     *
     * @param activity
     */
    public void stopDiscovery(Activity activity, GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stop discovery");
        Logger.d(TAG, "discoveryIsRunning ? : " + discoverIsRunning.get());
        if (!discoverIsRunning.compareAndSet(true, false)) {
            cbStopDone.done(false);
        }
        if (discoverThread != null) {
            discoverThread.kill();
        }

        stopSpecan(activity, cbStopDone);

    }

    public void stopGuardian(final Activity activity, GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopGuardian()");
        Logger.d(TAG, "guardianIsRunning ? : " + guardianIsRunning.get());
        if (!guardianIsRunning.compareAndSet(true, false)) {
            cbStopDone.done(false);
            return;
        }
        if (guardianThread != null) {
            guardianThread.kill();
        }

        stopSpecan(activity, cbStopDone);

    }

    /**
     * Indicate if the dongle has stopped specan
     * @return
     */
    public boolean specanIsRunning() {
        return specanIsRunning;
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
    public boolean isAvailableForNewStart(Activity activity) {
        return (isConnected && GollumDongle.getInstance(activity).isDeviceConnected() && !specanIsRunning() && !discoveryProcessIsRunning() && !guardianProcessIsRunning());
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean state) {
        isConnected = state;
    }

    public void close(Activity activity) {
        GollumDongle.getInstance(activity).closeDevice();
    }

}
