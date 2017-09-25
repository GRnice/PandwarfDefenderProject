package com.example.aventador.protectalarm.process;

import android.app.Activity;
import android.util.Log;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Aventador on 25/09/2017.
 */

public class WatchMan {
    private static int DELAY_PACKETS_RSSI_MS = 500;

    private static int WATCH_INTERVAL = 10;
    private static int PEAK_TOLERANCE = 5;

    private static WatchMan instance;
    private AtomicBoolean watchManIsRunning;
    private boolean specanIsRunning;
    private WatchManThread watchManThread;
    private static final String TAG = "WatchMan";

    public static WatchMan getInstance() {
        if (instance == null) {
            instance = new WatchMan();
        }

        return instance;
    }

    private WatchMan() {
        watchManIsRunning = new AtomicBoolean(false);
        specanIsRunning = false;
    }

    public boolean start(final Activity activity, final int frequency, final int dbTolerance, final GollumCallbackGetBoolean cbAttackDetected) {
        if (!watchManIsRunning.compareAndSet(false, true) || specanIsRunning) {
            return false;
        }
        Log.d(TAG, "Start WatchMan");
        startSpecan(activity, frequency, dbTolerance, cbAttackDetected);
        return true;
    }

    private void startSpecan(final Activity activity, int frequency, final int dbTolerance, final GollumCallbackGetBoolean cbAttackDetected) {
        GollumDongle.getInstance(activity).rfSpecanStart(0, frequency, 2, 2, DELAY_PACKETS_RSSI_MS, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                specanIsRunning = true;
                Log.d(TAG, "specan started");
                watchManThread = new WatchManThread(activity, dbTolerance, cbAttackDetected);
                watchManThread.start();
                Log.d(TAG, "asynctask started");
            }
        });
    }

    public boolean stop(final Activity activity) {
        Log.d(TAG, "stop()");
        Log.d(TAG, "watchManIsRunning ? : " + watchManIsRunning.get());
        if (!watchManIsRunning.compareAndSet(true, false)) {
            return false;
        }
        watchManThread.kill();
        try {
            watchManThread.join();
            GollumDongle.getInstance(activity).rfSpecanStop(0, new GollumCallbackGetInteger() {
                @Override
                public void done(int i) {
                    specanIsRunning = false;
                }
            });
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private class WatchManThread extends Thread {

        private int dbTolerance;
        private GollumCallbackGetBoolean cbAttackDetected;
        private Activity activity;
        private AtomicBoolean run;

        public WatchManThread(Activity activity, int dbTolerance, final GollumCallbackGetBoolean cbAttackDetected) {
            this.activity = activity;
            this.dbTolerance = dbTolerance;
            this.cbAttackDetected = cbAttackDetected;
        }
        @Override
        public void run() {
            run = new AtomicBoolean(true);
            int scanIteration = 0;
            int peakDetected = 0;
            byte[] rssi_buffer = new byte[2];
            while (run.get()) {
                while (scanIteration < WatchMan.WATCH_INTERVAL) {
                    if (!run.get()) {
                        return;
                    }
                    try {
                        Thread.sleep(DELAY_PACKETS_RSSI_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }

                    GollumDongle.getInstance(activity).rfSpecanGetRssi(rssi_buffer, 2);
                    Log.d(TAG, "rssi: " + rssi_buffer[1]);
                    if (rssi_buffer[1] > dbTolerance) {
                        peakDetected++;
                    }

                    scanIteration++;
                }
                if (peakDetected > WatchMan.PEAK_TOLERANCE) {
                    Log.d(TAG, "attack detected");
                    this.cbAttackDetected.done(true);
                }
                scanIteration = 0;
                peakDetected = 0;
            }
        }

        synchronized void kill() {
            run.set(false);
        }
    }
}
