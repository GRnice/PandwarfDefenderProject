package com.example.aventador.protectalarm.process.Runners;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Log;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Aventador on 21/10/2017.
 */

/**
 * This thread is reponsible for monitor if an attack is in progress.
 */
public class GuardianThread extends Runner {

    private static final String TAG = "GuardianThread";

    private int dbTolerance;
    private GollumCallbackGetBoolean cbAttackDetected;
    private double marginError;
    private int peakTolerance;


    public GuardianThread(Activity activity, int nbSequences, int nbScan, int nbChannels, int dbTolerance, int peakTolerance, int marginError, GollumCallbackGetBoolean cbAttackDetected) {
        super(activity, nbScan, nbSequences, nbChannels);
        this.dbTolerance = dbTolerance;
        this.cbAttackDetected = cbAttackDetected;
        this.peakTolerance = (int) ((peakTolerance / 100.0) * nbSequences);
        this.marginError = marginError / 100.0;
    }

    @Override
    public void run() {
        run = new AtomicBoolean(true);
        int peakDetected = 0;
        int sum = 0;
        int nbRssiReceived = 0;
        int means;
        byte[] rssi_buffer;
        while (run.get()) {
            for (int i = nbSequences; i > 0; i--) { // for each sequence

                sum = 0;
                nbRssiReceived = 0;

                if (peakDetected > peakTolerance) {
                    Logger.d(TAG, "attack detected");
                    this.kill();
                    this.cbAttackDetected.done(true);
                    return;
                }

                for (int j = nbScans; j > 0; j--) { // for each scan.
                    if (!run.get()) {
                        return;
                    }

                    rssi_buffer = scan();
                    if (rssi_buffer != null) {
                        Logger.d(TAG, "rssi: " + Arrays.toString(rssi_buffer));
                        for (int k = 0; k < nbChannels; k++) {
                            sum += rssi_buffer[k];
                            nbRssiReceived++;
                        }
                    }
                }
                if (nbRssiReceived == 0) {
                    Log.e(TAG, "rssi_buffer always empty !");
                    continue;
                } else {
                    means = sum / nbChannels;
                    Log.d(TAG, "means is: " + means);
                }


                if (means > (dbTolerance + Math.abs(marginError * dbTolerance))) {
                    peakDetected++;
                    Logger.d(TAG, "peak detected, peakDetected: " + peakDetected);
                }
            }
        }


    }
}
