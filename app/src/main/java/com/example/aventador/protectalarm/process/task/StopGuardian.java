package com.example.aventador.protectalarm.process.task;

import android.app.Activity;
import android.support.annotation.CallSuper;
import android.util.Log;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.events.ActionEvent;

/**
 * Created by Aventador on 30/10/2017.
 */

class StopGuardian extends Task {

    public StopGuardian(Activity activity, ActionEvent actionEvent, GollumCallbackGetBoolean cbThreadDone) {
        super(activity, actionEvent,  cbThreadDone);
    }

    @CallSuper
    @Override
    public void run() {
        super.run();
        stopGuardian(new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean stopSuccess) {
                Log.d(TAG, "Pandwarf stopped");
                toastShow("protection stopped");
                cbThreadDone.done(stopSuccess);
            }
        });
    }
}
