package com.example.aventador.protectalarm.process;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Aventador on 21/09/2017.
 */

public class ThresholdFinder {
    private static int DELAY_PACKETS_RSSI_MS = 700;
    private AsyncSearchOptimalPeak asyncSearchOptimalPeak = null;
    private AtomicBoolean specanRunning;
    private static ThresholdFinder instance;

    public static ThresholdFinder getInstance() {
        if (instance == null) {
            instance = new ThresholdFinder();
        }

        return instance;
    }

    private ThresholdFinder() {
        specanRunning = new AtomicBoolean(false);
    }

    public void find(final Activity activity, final String frequency, final GollumCallbackGetInteger cbDone) {
        GollumDongle gollumDongle = GollumDongle.getInstance(activity);
        gollumDongle.rfSpecanSetPktDelay(0, DELAY_PACKETS_RSSI_MS, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                startSpecan(activity, frequency, cbDone);
            }
        });


    }

    private void startSpecan(Activity activity, String frequency, final GollumCallbackGetInteger cbDone) {
        if (specanRunning.get()) {
            return;
        }
        specanRunning.set(true);
        asyncSearchOptimalPeak = new AsyncSearchOptimalPeak(activity, Integer.valueOf(frequency), cbDone);
        GollumDongle.getInstance(activity).rfSpecanStart(0, Integer.valueOf(frequency), 2, 2, DELAY_PACKETS_RSSI_MS, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                Log.d("ThresholdFinder", "specan started");
                asyncSearchOptimalPeak.execute();
            }
        });

    }

    public void stopSpecan(Activity activity) {
        if (!specanRunning.get()) {
            return;
        }
        specanRunning.set(false);
        asyncSearchOptimalPeak.cancel(true);
        GollumDongle.getInstance(activity).rfSpecanStop(0, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {

            }
        });
    }

    private class AsyncSearchOptimalPeak extends AsyncTask {

        private int frequency;
        private Activity activity;
        final private GollumCallbackGetInteger cbSearchDone;

        public AsyncSearchOptimalPeak(Activity activity, int frequency, final GollumCallbackGetInteger cbSearchDone) {
            this.frequency = frequency;
            this.activity = activity;
            this.cbSearchDone = cbSearchDone;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d("ThresholdFinder", "start asynctask");
            final int minRssi = -120;
            final int maxRssi = 0;
            int currentRssi = minRssi;
            int nbPeak = 1;
            final int maxRssiRead = 8;
            int nbRssiRead = 0;
            byte[] rssi_buffer = new byte[2];
            while (nbPeak > 0 && currentRssi <= maxRssi) {
                nbPeak = 0;
                nbRssiRead = 0;
                currentRssi += 10;
                while (maxRssiRead > nbRssiRead) {
                    nbRssiRead++;
                    try {
                        Thread.sleep(DELAY_PACKETS_RSSI_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                    GollumDongle.getInstance(activity).rfSpecanGetRssi(rssi_buffer, 2);
                    Log.d("ThresholdFinder", "rssi: " + rssi_buffer[1]);
                    if (rssi_buffer[1] > currentRssi) {
                        nbPeak++;
                    }
                }
                Log.d("ThresholdFinder", "nb Peak :" + nbPeak + " for Rssi :" + currentRssi);

            }
            final int finalRssi = currentRssi;
            GollumDongle.getInstance(activity).rfSpecanStop(0, new GollumCallbackGetInteger() {
                @Override
                public void done(int i) {
                    Log.d("ThresholdFinder", "specan stopped");
                    specanRunning.set(false);
                    cbSearchDone.done(finalRssi);
                }
            });
            return null;
        }
    }
}
