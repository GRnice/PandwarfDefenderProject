package com.example.aventador.protectalarm.process;

/**
 * Created by Aventador on 27/09/2017.
 */

import android.app.Activity;
import android.content.Context;
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
    private JobManager jobManager;
    private AtomicBoolean jammingIsRunning;
    private JobCreator jobCreator;
    private GollumCallbackGetBoolean cbJammingDone;

    private static final int DATA_RATE = 4000;
    private static final int MODULATION = 0x30; // ASK/OOK

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
        jammingIsRunning = new AtomicBoolean(false);
        jobCreator = new JobCreator() {
            @Override
            public Job create(String tag) {
                return new Job() {
                    @NonNull
                    @Override
                    protected Result onRunJob(Params params) {
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

    public boolean startJamming(int frequency, GollumCallbackGetBoolean cbJammingDone) {
        Logger.d(TAG, "startJamming()");
        if (!jammingIsRunning.compareAndSet(false, true)) {
            return false;
        }
        this.cbJammingDone = cbJammingDone;
        Logger.d(TAG, "jamming can be started");
        GollumDongle.getInstance(activity).rfJammingStart(0, frequency, DATA_RATE, MODULATION, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                Logger.d(TAG, "jamming started");
                createEndingJob();
            }
        });
        return true;
    }

    private void createEndingJob() {
        Logger.d(TAG, "createEndingJob()");
        new JobRequest.Builder(JOB_TAG)
                .setExact(10_000L) // jamming duration fixed at 10 seconds.
                .setPersisted(true)
                .build()
                .schedule();
    }

    public void stopJamming(boolean stopByUser, final GollumCallbackGetBoolean cbStopDone) {
        Logger.d(TAG, "stopJamming()");
        if (!jammingIsRunning.compareAndSet(true, false)) {
            cbStopDone.done(false);
            return;
        }
        jobManager.cancelAll();
        if (cbJammingDone != null && !stopByUser) {
            cbJammingDone.done(true);
            cbJammingDone = null;
        }
        GollumDongle.getInstance(activity).rfJammingStop(0, new GollumCallbackGetInteger() {
            @Override
            public void done(int i) {
                cbStopDone.done(true);
                Logger.d(TAG, "jamming stopped");
            }
        });
    }
}
