package com.example.aventador.protectalarm.process;

/**
 * Created by Giangrasso on 27/09/2017.
 */

import android.app.Activity;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.tools.Logger;
import com.example.aventador.protectalarm.tools.Recaller;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.aventador.protectalarm.tools.Recaller.JAMMER_JOB_TAG;

/**
 * Jammer is dedicated to apply a jamming.
 * It's used only when a brute force attack is detected.
 * /!\ IT MUSTN'T BE USED FOR OTHER THINGS /!\
 * you are warned...
 */
class Jammer {
    private static final String TAG = "Jammer";
    private static Jammer instance;
    private boolean isInit;
    private AtomicBoolean jammingIsRunning; // indicate if jamming is on run.

    private GollumCallbackGetBoolean cbJammingDone; // called when the jamming is done.

    private static final int DATA_RATE = 4000; // data rate
    private static final int MODULATION = 0x30; // ASK/OOK
    private static final long DELAY = 10_000L; // Jamming duration fixed at 10 seconds.

    public static Jammer getInstance() {
        if (instance == null) {
            instance = new Jammer();
        }

        return instance;
    }

    public void init() {
        if (isInit) {
            return;
        }
        isInit = true;
    }

    public boolean isRunning() {
        return this.jammingIsRunning.get();
    }

    private Jammer() {
        isInit = false;
        // At the creation of jobCreator, we defined a Job to stop jamming.
        // This job is called after a certain delay.
        jammingIsRunning = new AtomicBoolean(false);
    }

    /**
     * Start jamming
     * - cbStartJamming called when jamming is started or not. (true or false)
     * - cbJammingDone called when jamming is terminated.
     * @param frequency
     * @param cbStartJamming
     * @param cbJammingDone
     * @return
     */
    public void startJamming(final Activity activity, int frequency, final GollumCallbackGetBoolean cbStartJamming, GollumCallbackGetBoolean cbJammingDone) {
        Logger.d(TAG, "startJamming()");
        Logger.d(TAG, "startJamming(): Pandwarf available ? " + Pandwarf.getInstance().isAvailable(activity));
        if (!Pandwarf.getInstance().isAvailable(activity) || !jammingIsRunning.compareAndSet(false, true)) {
            cbStartJamming.done(false);
            return;
        }
        this.cbJammingDone = cbJammingDone;
        Logger.d(TAG, "startJamming(): jamming can be started");
        GollumDongle.getInstance(activity).rfJammingStart(0, frequency, DATA_RATE, MODULATION, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                Logger.d(TAG, "startJamming(): jamming started");
                cbStartJamming.done(true);
                prepareJob(activity); // prepare the job dedicated to stop the jamming after a certain delay. see field: DELAY
            }
        });
    }

    /**
     * Set jamming duration, after this period we call the job to stop jamming
     */
    private void prepareJob(final Activity activity) {
        Logger.d(TAG, "scheduleEndingJob()");
        Recaller.getInstance().recallMe(JAMMER_JOB_TAG, DELAY, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                // stop jamming.
                stopJamming(activity, false, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {

                    }
                });
            }
        });
    }

    /**
     * Stop jamming, cancel all jobs scheduled.
     * if stopJamming , cbJammingDone is called , this callback restart the specan.
     * Otherwise stopJamming is called by user (or if Bluetooth is disable), this action kill the protection routine.
     *
     * @param stopByUser  indicate if the guardian is interrupted by an external actor (pandwarf disconnection, user action)
     * @param cbStopDone
     */
    public void stopJamming(Activity activity, final boolean stopByUser, final GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopJamming()");
        if (!jammingIsRunning.compareAndSet(true, false)) {
            cbStopDone.done(true);
            return;
        }
        Recaller.getInstance().cancel(JAMMER_JOB_TAG);

        GollumDongle.getInstance(activity).rfJammingStop(0, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                /*
                If stopJamming is called by a Job, stopByUser will be False
                Otherwise True
                */
                if (cbJammingDone != null && !stopByUser) {
                    cbJammingDone.done(true); // this action restart the specan /!\
                    cbJammingDone = null;
                }
                // call cbStopDone when jamming is stopped.
                cbStopDone.done(true);
                Logger.d(TAG, "jamming stopped");
            }
        });
    }
}
