package com.example.aventador.protectalarm.tools;

import android.content.Context;
import android.support.annotation.NonNull;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Giangrasso on 26/10/2017.
 */

public class Recaller {

    private static final String TAG = "Recaller";
    private static Recaller instance;

    public static final String JAMMER_JOB_TAG = "jammer_job";
    public static final String FAST_PROTECTION_ANALYZER_TAG = "fast_protection_analyze_job";
    private JobManager jobManager;
    private HashMap<String, GollumCallbackGetBoolean> callbackMapping;

    public static Recaller getInstance() {
        if (instance == null) {
            throw new IllegalStateException("you have to give the context at least once, first of all call Recaller.getInstance(Context context)");
        }

        return instance;
    }

    public static Recaller getInstance(Context context) {
        if (instance == null) {
            instance = new Recaller(context);
        }

        return instance;
    }

    /**
     *
     * @param context
     */
    private Recaller(Context context) {
        callbackMapping = new HashMap<>();
        jobManager = JobManager.create(context);
        JobCreator jobCreator = new JobCreator() {
            @Override
            public Job create(final String tag) {
                return new Job() {
                    @NonNull
                    @Override
                    protected Result onRunJob(Params params) {
                        endingJob(tag);
                        return Result.SUCCESS;
                    }
                };
            }
        };

        jobManager.addJobCreator(jobCreator);
    }

    /**
     * Called when a Job or a LittleWait is finished.
     * @param tag tag of the job ended
     */
    private void endingJob(String tag) {
        Logger.d(TAG, "endingJob()");
        if (callbackMapping.containsKey(tag)) {
            Logger.d(TAG, "endingJob() call callback");
            callbackMapping.get(tag).done(true);
            callbackMapping.remove(tag);
        }
    }

    /**
     * Like a timer, the given callback will be called when time is up.
     * @param identifier unique identifier
     * @param duration Time in millis
     * @param cbToRecall Called when time is elapsed
     */
    public void recallMe(final String identifier, long duration, final GollumCallbackGetBoolean cbToRecall) {
        Logger.d(TAG, "recallMe()");

        callbackMapping.put(identifier, cbToRecall);
        if (duration < 2000L) {
            new LittleWait(identifier, duration).start();
        }
        new JobRequest.Builder(identifier)
                .setExact(duration) // in n seconds, onRunJob is called.
                .setPersisted(true)
                .build()
                .schedule();
    }

    /**
     * cancel a recall associated to the given tag.
     * @param tag
     */
    public void cancel(String tag) {
        jobManager.cancelAllForTag(tag);
        if (callbackMapping.containsKey(tag)) {
            callbackMapping.remove(tag);
        }
    }

    private class LittleWait extends Thread {
        private long delay;
        private String tag;

        public LittleWait(String tag, long delay) {
            this.tag = tag;
            this.delay  = delay;
        }
        @Override
        public void run() {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            endingJob(tag);
        }
    }


}
