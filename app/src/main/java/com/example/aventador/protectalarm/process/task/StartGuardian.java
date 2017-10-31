package com.example.aventador.protectalarm.process.task;


import android.app.Activity;
import android.support.annotation.CallSuper;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.tools.Logger;

import org.greenrobot.eventbus.EventBus;

import static com.example.aventador.protectalarm.events.State.PROTECTION_FAIL;

/**
 * Created by Aventador on 30/10/2017.
 */

class StartGuardian extends Task {

    public StartGuardian(Activity activity, ActionEvent actionEvent, GollumCallbackGetBoolean cbThreadDone) {
        super(activity, actionEvent,  cbThreadDone);
    }

    @CallSuper
    @Override
    public void run() {
        super.run();
        final String frequency = actionEvent.getParameter(Parameter.FREQUENCY);
        final String dbTolerance =  actionEvent.getParameter(Parameter.RSSI_VALUE);
        final int peakTolerance = Integer.valueOf(actionEvent.getParameter(Parameter.PEAK_TOLERANCE));
        final int marginError = Integer.valueOf(actionEvent.getParameter(Parameter.MARGIN_ERROR));
        startGuardian(frequency, dbTolerance, peakTolerance, marginError, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean startSuccess) {
                if (!startSuccess) {
                    Logger.e(TAG, "guardian not started");
                    toastShow("Fail to start protection");
                    EventBus.getDefault().postSticky(new StateEvent(PROTECTION_FAIL, ""));
                } else {
                    Logger.d(TAG, "guardian started");
                    String message = "protection started\n frequency: " + frequency +
                            "\n db tolerance: " + dbTolerance +
                            "\n peak tolerance: " + peakTolerance +
                            "\n margin error: " + marginError;
                    toastShow(message);

                }
                cbThreadDone.done(startSuccess);
            }
        });
    }
}
