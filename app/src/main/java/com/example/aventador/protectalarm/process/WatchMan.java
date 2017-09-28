package com.example.aventador.protectalarm.process;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Log;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Aventador on 25/09/2017.
 */

/**
 * WatchMan is responsible for :
 *                              calculating the tolerance threshold making it possible to distinguish a brute force attack.
 *                              monitor if an attack is in progress.
 */
public class WatchMan {

    private static int DELAY_PACKETS_RSSI_MS = 100;
    private static int FREQUENCY_BY_CHANNELS = 25000;
    private static int NB_CHANNELS = 5; // 5 by default.
    private static int NB_SCAN_BY_SEQUENCE = 4; // interval -> [1 , MAX_INT] one sequence -> 20 scan for each sequence.

    /*
    For Guardian mode
     */
    private static int GUARDIAN_NB_SEQUENCES = 40; // one sequence -> 20 scans.
    private static int PEAK_TOLERANCE = 20; // interval -> [1 , GUARDIAN_NB_SCAN_BY_SEQUENCE] in one sequence, if number of anormal peack is better than PEAK_TOLERANCE
                                            // it's a brute force attack.
                                            // TODO: PEAK_TOLERANCE should be represented by a seek bar.
    private static double MARGIN_ERROR = 0.1; // If the Peak detected is better than PEAK_TOLERANCE (10% of PEAK_TOLERANCE) it's a potential brute force attack


    /*
    For discovery mode
     */
    private static int DISCOVERY_NB_SEQUENCES = 8; // 8 sequences of scan will be executed

    private static WatchMan instance;
    private AtomicBoolean guardianIsRunning;
    private AtomicBoolean discoverIsRunning;
    private boolean specanIsRunning;
    private GuardianThread guardianThread;
    private DiscoverThread discoverThread;
    private static final String TAG = "WatchMan";

    public static WatchMan getInstance() {
        if (instance == null) {
            instance = new WatchMan();
        }

        return instance;
    }

    private WatchMan() {
        guardianIsRunning = new AtomicBoolean(false);
        discoverIsRunning = new AtomicBoolean(false);
        specanIsRunning = false;
    }

    /**
     *
     * @param activity
     * @param frequency
     * @param dbTolerance
     * @param cbAttackDetected
     * @return
     */
    public boolean startGuardian(final Activity activity, final int frequency, final int dbTolerance, final GollumCallbackGetBoolean cbAttackDetected) {
        Logger.d(TAG, "discoverIsRunning.get():" + discoverIsRunning.get() + " guardianIsRunning ?: " + guardianIsRunning.get() + " specan run:" + specanIsRunning);
        if (discoverIsRunning.get() || !guardianIsRunning.compareAndSet(false, true) || specanIsRunning) {
            return false;
        }
        Logger.d(TAG, "Start Guardian");
        startSpecan(activity, frequency, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                guardianThread = new GuardianThread(activity, dbTolerance, cbAttackDetected);
                guardianThread.start();
            }
        });
        return true;
    }

    /**
     *
     * @param activity
     * @param frequency
     * @param cbDiscoveryDone
     * @return
     */
    public boolean startDiscovery(final Activity activity, int frequency, final GollumCallbackGetInteger cbDiscoveryDone) {
        Logger.d(TAG, "guardianIsRunning.get():" + guardianIsRunning.get() + " discoverIsRunning ?: " + discoverIsRunning.get() + " specan run:" + specanIsRunning);
        if (guardianIsRunning.get() || !discoverIsRunning.compareAndSet(false, true) || specanIsRunning) {
            return false;
        }
        Logger.d(TAG, "Start discovery");
        startSpecan(activity, frequency, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                discoverThread = new DiscoverThread(activity, cbDiscoveryDone);
                discoverThread.start();
            }
        });
        return true;
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
            return ;
        }
        if (discoverThread != null) {
            discoverThread.kill();
            try {
                discoverThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
            try {
                Logger.d(TAG, "join requested");
                guardianThread.join();
                Logger.d(TAG, "join done");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        stopSpecan(activity, cbStopDone);

    }

    /**
     * This Thread is responsible for calculating the tolerance threshold making it possible to distinguish a brute force attack.
     */
    private class DiscoverThread extends Thread {
        private GollumCallbackGetInteger cbDone;
        private Activity activity;
        private ArrayList<Integer> allMeans;
        private AtomicBoolean run;
        // a bucket represent the mean of a sequence of scans. In this case, for each bucket, 4 scan will be executed (scan is a getRssi())

        public DiscoverThread(Activity activity, GollumCallbackGetInteger cbDone) {
            this.activity = activity;
            this.cbDone = cbDone;
            this.allMeans = new ArrayList<>();
            this.run = new AtomicBoolean(false);
        }

        @Override
        public void run() {
            this.allMeans = new ArrayList<>();
            this.run.set(true);
            int sum = 0;
            int nbElements = 0;
            byte[] rssi_buffer;

            for (int i = DISCOVERY_NB_SEQUENCES; i > 0; i--) { // for each sequence
                // a sequence represent the mean of all scan's result in the current sequence.
                Logger.d(TAG, "new bucket: " + i);
                for (int j = NB_SCAN_BY_SEQUENCE; j > 0; j--) { // for each scan.
                    // for each sequence, NB_SCAN_BY_SEQUENCE scans (getRssi) will be executed.
                    if (!this.run.get()) {
                        return;
                    }

                    rssi_buffer = scan(); // scan
                    if (rssi_buffer != null) { // if null -> buffer not ready.
                        Logger.d(TAG, "rssi: " + Arrays.toString(rssi_buffer));
                        for (int k = 0; k < NB_CHANNELS; k++) {
                            sum += rssi_buffer[k];
                            nbElements++;
                        }
                    }
                }
                if (nbElements == 0) {
                    Logger.d(TAG, "DiscoverThread : rssi_buffer always empty");
                    allMeans.add(Integer.MIN_VALUE);
                } else {
                    allMeans.add(sum / nbElements);

                }

                sum = 0;
                nbElements = 0;
            }

            int meansAllBuckets = 0;
            for (Integer means : allMeans) {
                meansAllBuckets += means;
            }
            cbDone.done(meansAllBuckets / allMeans.size());
        }

        /**
         *
         * @return
         */
        @Nullable
        private byte[] scan() {
            byte[] rssi_buffer = new byte[NB_CHANNELS];
            int check = GollumDongle.getInstance(activity).rfSpecanGetRssi(rssi_buffer, NB_CHANNELS);
            Logger.d(TAG, "check: " + check);
            if (check != NB_CHANNELS) {
                return null;
            }
            return rssi_buffer;
        }

        public synchronized void kill() {
            this.run.set(false);
        }
    }

    /**
     * This thread is reponsible for monitor if an attack is in progress.
     */
    private class GuardianThread extends Thread {

        private int dbTolerance;
        private GollumCallbackGetBoolean cbAttackDetected;
        private Activity activity;
        private AtomicBoolean run;

        public GuardianThread(Activity activity, int dbTolerance, final GollumCallbackGetBoolean cbAttackDetected) {
            this.activity = activity;
            this.dbTolerance = dbTolerance;
            this.cbAttackDetected = cbAttackDetected;
        }
        @Override
        public void run() {
            run = new AtomicBoolean(true);
            int peakDetected = 0;
            int sum = 0;
            int nbElements = 0;
            int means;
            byte[] rssi_buffer;

            while (run.get()) {
                for (int i = GUARDIAN_NB_SEQUENCES; i > 0; i--) { // for each sequence
                    for (int j = NB_SCAN_BY_SEQUENCE; j > 0; j--) { // for each scan.
                        if (!run.get()) {
                            return;
                        }

                        rssi_buffer = scan();
                        if (rssi_buffer != null) {
                            Logger.d(TAG, "rssi: " + Arrays.toString(rssi_buffer));
                            for (int k = 0 ; k < NB_CHANNELS ; k++) {
                                sum += rssi_buffer[k];
                                nbElements++;
                            }
                        }
                    }
                    if (nbElements == 0) {
                        Log.e(TAG, "rssi_buffer always empty !");
                        means = Integer.MIN_VALUE;
                    } else {
                        means = sum / nbElements;
                    }
                    Log.d(TAG, "means is: " + means);
                    nbElements = 0;
                    sum = 0;
                    if (means > (dbTolerance + Math.abs(MARGIN_ERROR * dbTolerance))) {
                        peakDetected++;
                        Logger.d(TAG, "peak detected, peakDetected: " + peakDetected);
                    }

                }

                if (peakDetected > WatchMan.PEAK_TOLERANCE) {
                    Logger.d(TAG, "attack detected");
                    this.cbAttackDetected.done(true);
                }
                peakDetected = 0;
            }
        }

        /**
         *
         * @return
         */
        @Nullable
        private byte[] scan() {
            byte[] rssi_buffer = new byte[NB_CHANNELS];
            int check = GollumDongle.getInstance(activity).rfSpecanGetRssi(rssi_buffer, NB_CHANNELS);
            if (check != NB_CHANNELS) {
                return null;
            }
            return rssi_buffer;
        }

        synchronized void kill() {
            run.set(false);
        }
    }
}
