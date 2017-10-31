package com.example.aventador.protectalarm.process.task;

import android.app.Activity;
import android.support.annotation.CallSuper;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;

/**
 * Created by Aventador on 30/10/2017.
 */

class StartJamming extends Task {

    public StartJamming(Activity activity, ActionEvent actionEvent, GollumCallbackGetBoolean cbThreadDone) {
        super(activity, actionEvent,  cbThreadDone);
    }

    @CallSuper
    @Override
    public void run() {
        super.run();
        final String frequency = actionEvent.getParameter(Parameter.FREQUENCY);
        final String dbTolerance = actionEvent.getParameter(Parameter.RSSI_VALUE);
        final int peakTolerance = Integer.valueOf(actionEvent.getParameter(Parameter.PEAK_TOLERANCE));
        final int marginError = Integer.valueOf(actionEvent.getParameter(Parameter.MARGIN_ERROR));
        startJamming(frequency, dbTolerance, peakTolerance, marginError, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean success) {
                cbThreadDone.done(success);
            }
        });
    }
}
