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

    private void endingJob(String tag) {
        Logger.d(TAG, "endingJob()");
        if (callbackMapping.containsKey(tag)) {
            Logger.d(TAG, "endingJob() call callback");
            callbackMapping.get(tag).done(true);
            callbackMapping.remove(tag);
        }
    }

    public void recallMe(final String identifier, long duration, final GollumCallbackGetBoolean cbToRecall) {
        Logger.d(TAG, "recallMe()");
        callbackMapping.put(identifier, cbToRecall);

        new JobRequest.Builder(identifier)
                .setExact(duration) // in n seconds, onRunJob is called.
                .setPersisted(true)
                .build()
                .schedule();
    }

    public void cancel(String tag) {
        jobManager.cancelAllForTag(tag);
    }


}
