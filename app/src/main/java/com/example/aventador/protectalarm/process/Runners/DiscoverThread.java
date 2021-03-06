package com.example.aventador.protectalarm.process.Runners;

import android.app.Activity;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Giangrasso on 21/10/2017.
 */

/**
 * This Thread is responsible for calculating the decibel tolerance threshold making it possible to distinguish a brute force attack.
 */
public class DiscoverThread extends Runner {

    private static final String TAG = "DiscoverThread";
    private GollumCallbackGetInteger cbDone;
    private ArrayList<Integer> allMeans;

    /**
     *
     * @param activity
     * @param nbScans
     * @param nbSequences
     * @param nbChannels
     * @param cbDone Called when the thread is terminated, db tolerance value will be given.
     */
    public DiscoverThread(Activity activity, int nbScans, int nbSequences, int nbChannels, GollumCallbackGetInteger cbDone) {
        super(activity, nbScans, nbSequences, nbChannels);
        this.cbDone = cbDone;
        this.allMeans = new ArrayList<>();
    }

    @Override
    public void run() {
        this.allMeans = new ArrayList<>();
        this.run.set(true);
        int sum;
        int nbRssiRceived;
        byte[] rssi_buffer;

        for (int i = nbSequences; i > 0; i--) { // for each sequence
            // a sequence represent the mean of all scan's result in the current sequence.
            Logger.d(TAG, "new sequence: " + i);
            sum = 0;
            nbRssiRceived = 0;
            for (int j = nbScans; j > 0; j--) { // for each scan.
                // for each sequence, NB_SCAN_BY_SEQUENCE scans (getRssi) will be executed.
                if (!this.run.get()) {
                    return;
                }

                rssi_buffer = scan(); // scan
                if (rssi_buffer != null) { // if null -> buffer not ready.
                    Logger.d(TAG, "rssi: " + Arrays.toString(rssi_buffer));
                    for (int k = 0; k < nbChannels; k++) {
                        nbRssiRceived++;
                        sum += rssi_buffer[k]; // this sum will be divided by nbRssiRceived
                    }
                }
            }
            if (nbRssiRceived == 0) {
                Logger.d(TAG, "DiscoverThread : sum has not been modified");
                continue;
            } else {
                allMeans.add(sum / nbRssiRceived);
            }

        }

        int meansOfAllSequences = 0;
        for (Integer means : allMeans) {
            meansOfAllSequences += means;
        }

        if (allMeans.size() == 0) {
            Logger.e(TAG, "Impossible to estimates the average signal value");
            cbDone.done(Integer.MIN_VALUE);

        } else {
            int meanFinal = meansOfAllSequences / allMeans.size();
            Logger.d(TAG, "meanFinal is : " + meanFinal);
            cbDone.done(meanFinal);
        }

    }
}
