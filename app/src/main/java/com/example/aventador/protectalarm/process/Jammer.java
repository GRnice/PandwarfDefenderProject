package com.example.aventador.protectalarm.process;

/**
 * Created by Aventador on 27/09/2017.
 */

import android.app.Activity;
import android.support.annotation.NonNull;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Jammer is dedicated to apply a jamming.
 * It's used only when a brute force attack is detected.
 * /!\ IT MUSTN'T BE USED FOR OTHER THINGS /!\
 * you are warned...
 */
public class Jammer {
    private static final String TAG = "Jammer";
    private static final String JOB_TAG = "jamming_tag";
    private static Jammer instance;
    private boolean isInit;
    private Activity activity;
    private AtomicBoolean jammingIsRunning; // indicate if jamming is on run.

    private JobManager jobManager; // used to create a Job to stop the jamming after a certain delay
    private JobCreator jobCreator; // used to create a Job to stop the jamming after a certain delay

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

    public void init(Activity activity) {
        if (isInit) {
            return;
        }
        isInit = true;
        this.activity = activity;
        jobManager = JobManager.create(activity);
        jobManager.addJobCreator(jobCreator);
    }

    private Jammer() {
        isInit = false;
        // At the creation of jobCreator, we defined a Job to stop jamming.
        // This job is called after a certain delay.
        jammingIsRunning = new AtomicBoolean(false);

        jobCreator = new JobCreator() {
            @Override
            public Job create(String tag) {
                // Job to stop jamming is also defined
                return new Job() {
                    @NonNull
                    @Override
                    protected Result onRunJob(Params params) {
                        // stop jamming.
                        stopJamming(false, new GollumCallbackGetBoolean() {
                            @Override
                            public void done(boolean b) {

                            }
                        });
                        return Result.SUCCESS;
                    }
                };
            }
        };
    }

    /**
     *
     * @param frequency
     * @param cbStartJamming
     * @param cbJammingDone
     * @return
     */
    public void startJamming(int frequency, final GollumCallbackGetBoolean cbStartJamming, GollumCallbackGetBoolean cbJammingDone) {
        Logger.d(TAG, "startJamming()");
        if (!Pandwarf.getInstance().isAvailableForNewStart(activity) || !jammingIsRunning.compareAndSet(false, true)) {
            cbStartJamming.done(false);
        }
        this.cbJammingDone = cbJammingDone;
        Logger.d(TAG, "jamming can be started");
        GollumDongle.getInstance(activity).rfJammingStart(0, frequency, DATA_RATE, MODULATION, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                Logger.d(TAG, "jamming started");
                cbStartJamming.done(true);
                prepareJob(); // prepare the job dedicated to stop the jamming after a certain delay. see field: DELAY
            }
        });
    }

    /**
     * Set jamming duration, after this period we call the job to stop jamming
     */
    private void prepareJob() {
        Logger.d(TAG, "scheduleEndingJob()");
        new JobRequest.Builder(JOB_TAG)
                .setExact(DELAY) // jamming duration fixed at 10 seconds.
                .setPersisted(true)
                .build()
                .schedule();
    }

    /**
     * Stop jamming, cancel all jobs scheduled.
     * @param stopByUser
     * @param cbStopDone
     */
    public void stopJamming(boolean stopByUser, final GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopJamming()");
        if (!jammingIsRunning.compareAndSet(true, false)) {
            cbStopDone.done(false);
            return;
        }
        jobManager.cancelAll(); // kill all jobs scheduled.
        /*
        If stopJamming is called by a Job, stopByUser will be False
        Otherwise True
         */
        if (cbJammingDone != null && !stopByUser) {
            cbJammingDone.done(true); // this action restart the specan /!\
            cbJammingDone = null;
        }
        GollumDongle.getInstance(activity).rfJammingStop(0, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                // call cbStopDone when jamming is stopped.
                cbStopDone.done(true);
                Logger.d(TAG, "jamming stopped");
            }
        });
    }
}
