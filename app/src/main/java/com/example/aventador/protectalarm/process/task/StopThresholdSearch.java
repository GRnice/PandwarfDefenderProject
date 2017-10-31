package com.example.aventador.protectalarm.process.task;

import android.app.Activity;
import android.support.annotation.CallSuper;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.events.ActionEvent;

/**
 * Created by Aventador on 30/10/2017.
 */

class StopThresholdSearch extends Task {

    public StopThresholdSearch(Activity activity, ActionEvent actionEvent, GollumCallbackGetBoolean cbThreadDone) {
        super(activity, actionEvent,  cbThreadDone);
    }

    @CallSuper
    @Override
    public void run() {
        super.run();
        stopThresholdSearch(new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean stopSuccess) {
                cbThreadDone.done(stopSuccess);
            }
        });
    }
}
