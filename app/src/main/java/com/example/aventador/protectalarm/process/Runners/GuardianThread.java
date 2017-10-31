package com.example.aventador.protectalarm.process.Runners;

import android.app.Activity;
import android.support.annotation.CallSuper;
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
 * Created by Giangrasso on 21/10/2017.
 */

/**
 * This thread is reponsible for monitor if an attack is in progress.
 *
 * In this Thread we try to detect attacks brute force.
 */
public class GuardianThread extends Runner {

    private static final String TAG = "GuardianThread";

    private GollumCallbackGetBoolean cbAttackDetected;
    private double marginError; // expressed as a decibel, if this value is excedeed during a scan, a dB peak is detected.
    private int peakTolerance; // expressed as a simple integer, if this value is excedeed during a sequence, a brute force attack is detected.

    private boolean retriggerable; // if False: break the thread when all sequences was executed, otherwise the thread continue.
    private GollumCallbackGetBoolean cbThreadDone;

    /**
     *
     * @param activity
     * @param nbSequences
     * @param nbScan
     * @param nbChannels
     * @param dbTolerance
     * @param peakTolerance Expressed as a percentage [0,100]
     * @param marginError Expressed as a percentage [0,100]
     * @param cbAttackDetected Called when an brute force attack is detected.
     */
    public GuardianThread(Activity activity, int nbSequences, int nbScan, int nbChannels, int dbTolerance, int peakTolerance, int marginError,
                          GollumCallbackGetBoolean cbAttackDetected) {
        this(activity, nbSequences, nbScan, nbChannels, dbTolerance, peakTolerance, marginError, cbAttackDetected, null);
    }


    /**
     *
     * @param activity
     * @param nbSequences
     * @param nbScan
     * @param nbChannels
     * @param dbTolerance
     * @param peakTolerance Expressed as a percentage [0,100]
     * @param marginError Expressed as a percentage [0,100]
     * @param cbAttackDetected Called when an brute force attack is detected.
     * @param cbThreadDone Called when the thread is terminate
     */
    public GuardianThread(Activity activity, int nbSequences, int nbScan, int nbChannels, int dbTolerance, int peakTolerance, int marginError,
                          GollumCallbackGetBoolean cbAttackDetected, @Nullable GollumCallbackGetBoolean cbThreadDone) {
        super(activity, nbScan, nbSequences, nbChannels);
        this.cbAttackDetected = cbAttackDetected;
        this.peakTolerance = (int) ((peakTolerance / 100.0) * nbSequences);
        this.marginError = (dbTolerance + Math.abs((marginError / 100.0) * dbTolerance));
        this.cbThreadDone = cbThreadDone;
    }

    @CallSuper
    public void start(boolean retriggerable) {
        this.retriggerable = retriggerable;
        super.start();
    }

    /**
     * according to the tolerance in decibel, the tolerance of the peaks rssi and the margin of error.
     * we are able to detect an attack
     */
    @Override
    public void run() {
        run = new AtomicBoolean(true);
        int average; // Represents the average in decibel of the received signals, in one Scan !
        // if average is greater than margin error, it's a peak.

        int peakDetected = 0; // Represents the number of peaks detected in N sequences.
        // if peakDetected is greater than (tolerance of peaks), we have detects an BRUTE FORCE ATTACK !!!

        int sum = 0; // It's the sum of all rssi signals received, for each calls to rfSpecanGetRssi
        int nbRssiReceived = 0; // Its' the sum of number of rssi signals received, in one Scan !


        byte[] rssi_buffer;
        while (run.get()) {

            peakDetected = 0;

            for (int i = nbSequences; i > 0; i--) { // for each sequence

                sum = 0;
                nbRssiReceived = 0;

                if (peakToleranceExceeded(peakDetected)) {
                    attackDetected();
                    return; // break the thread, an attack was detected.
                }

                for (int j = nbScans; j > 0; j--) { // for each scan.
                    if (!run.get()) {
                        return;
                    }

                    rssi_buffer = scan();
                    if (rssi_buffer != null) {
                        Logger.d(TAG, "rssi: " + Arrays.toString(rssi_buffer));
                        sum += sum(rssi_buffer);
                        nbRssiReceived += nbChannels;
                    }
                }
                if (nbRssiReceived == 0) {
                    Log.e(TAG, "rssi_buffer always empty !");
                    continue;
                } else {
                    average = sum / nbRssiReceived;
                    Log.d(TAG, "means is: " + average);
                }


                if (marginOfErrorExceeded(average)) {
                    peakDetected++;
                    Logger.d(TAG, "peak detected, peakDetected: " + peakDetected);
                }
            }

            if (!isRetriggerable()) {
                run.set(false);
            }
        }

        if (cbThreadDone != null) {
            cbThreadDone.done(true);
        }


    }

    /**
     * sum up all the items in the list
     * @param rssi_buffer
     * @return
     */
    private int sum(byte[] rssi_buffer) {
        int sum = 0;
        for (int k = 0; k < nbChannels; k++) {
            sum += rssi_buffer[k];
        }

        return sum;
    }

    /**
     * Called when an attack is detected.
     */
    private void attackDetected() {
        Logger.d(TAG, "attack detected");
        this.kill();
        this.cbAttackDetected.done(true);
    }

    /**
     * Indicates if the peak tolerance is exceeded. If that is the case -> brute force attack detected.
     * @param peakDetected
     * @return
     */
    private boolean peakToleranceExceeded(int peakDetected) {
        return (peakDetected > peakTolerance);
    }

    /**
     * Indicates if the margin of error is exceeded, If that is the case --> dB peak detected.
     * @param average
     * @return
     */
    private boolean marginOfErrorExceeded(int average) {
        return (average > marginError);
    }

    public boolean isRetriggerable() {
        return retriggerable;
    }
}
