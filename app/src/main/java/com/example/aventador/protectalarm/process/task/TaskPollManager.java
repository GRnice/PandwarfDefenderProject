package com.example.aventador.protectalarm.process.task;

import android.app.Activity;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.tools.Logger;


import java.util.ArrayDeque;
import java.util.Queue;


/**
 * Created by Giangrasso on 29/10/2017.
 *
 * Task PollManager is used to manage repetitive clicks.
 * Instead of call Pandwarf immediatly after a request, the request is stored in the queue of TaskPollManager.
 * So we make sure every request is executed without interferences from the user.
 */
public class TaskPollManager {
    private final Queue<ActionEvent> pollActions; // contains all user' requests
    private Activity activity;
    private Task currentTask;
    private static final String TAG = "TaskPollManager";

    // cbThreadDone is a callback called when a Task is finished.
    private GollumCallbackGetBoolean cbThreadDone = new GollumCallbackGetBoolean() {
        @Override
        public void done(boolean b) {
            endExec();
        }
    };

    private static TaskPollManager instance;
    public static TaskPollManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("getInstance(Activity) shall be called first");
        }

        return instance;
    }

    public static TaskPollManager getInstance(Activity activity) {
        if (instance == null) {
            instance = new TaskPollManager(activity);
        }

        return instance;
    }

    /**
     *
     * @param activity
     */
    private TaskPollManager(Activity activity) {
        this.activity = activity;
        pollActions = new ArrayDeque<>();
    }

    public final synchronized boolean put(ActionEvent actionEvent) {
        Logger.d(TAG, "put()");
        if (!pollActions.add(actionEvent)) {
            return false; // fail
        }
        if (pollActions.size() == 1) {
            Logger.d(TAG, "one element in the poll, otherwise currentTask is running and will call later endExec");
            execNext();
        }
        return true;
    }

    /**
     * Execute a new task stored in the poll.
     */
    private synchronized void execNext() {
        Logger.d(TAG, "execNext()");
        if (pollActions.size() == 0) {
            Logger.d(TAG, "no task in the poll");
            return;
        }
        ActionEvent actionEvent = pollActions.peek();
        Logger.d(TAG, "execNext: actionEvent is: " + actionEvent.getActionRequested());
        switch (actionEvent.getActionRequested()) {
            case START_FAST_PROTECTION_ANALYZER: {
                currentTask = new StartFastProtection(activity, actionEvent, cbThreadDone);
                break;
            }

            case START_SEARCH_THRESHOLD: {
                currentTask = new StartThresholdSearch(activity, actionEvent, cbThreadDone);
                break;
            }

            case START_PROTECTION: {
                currentTask = new StartGuardian(activity, actionEvent, cbThreadDone);
                break;
            }

            case START_JAMMING: {
                currentTask = new StartJamming(activity, actionEvent, cbThreadDone);
                break;
            }

            case STOP_FAST_PROTECTION_ANALYZER: {
                currentTask = new StopFastProtection(activity, actionEvent, cbThreadDone);
                break;
            }

            case STOP_SEARCH_THRESHOLD: {
                currentTask = new StopThresholdSearch(activity, actionEvent, cbThreadDone);
                break;
            }

            case STOP_PROTECTION: {
                currentTask = new StopGuardian(activity, actionEvent, cbThreadDone);
                break;
            }
            default: {
                // Case not supported
                endExec();
                return;
            }
        }
        currentTask.start();

    }

    /**
     * Called when a task is done.
     */
    private synchronized void endExec() {
        Logger.d(TAG, "endExec()");
        pollActions.poll();
        execNext();
    }

}
