package com.example.aventador.protectalarm.process.task;

import android.app.Activity;
import android.support.annotation.CallSuper;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.tools.Logger;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Aventador on 30/10/2017.
 */

class StartFastProtection extends Task {

    public StartFastProtection(Activity activity, ActionEvent actionEvent, GollumCallbackGetBoolean cbThreadDone) {
        super(activity, actionEvent,  cbThreadDone);
    }

    @CallSuper
    @Override
    public void run() {
        super.run();
        String frequency = actionEvent.getParameters().getString(Parameter.FREQUENCY.toString());
        startFastProtectionAnalyser(frequency, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean startSuccess) {
                if (!startSuccess) {
                    Logger.e(TAG, "startFastProtectionAnalyser: fast protection analyzer not started, FAIL");
                    toastShow("Fast protection analyzer not started");
                    EventBus.getDefault().postSticky(new StateEvent(com.example.aventador.protectalarm.events.State.FAST_PROTECTION_ANALYZER_FAIL, ""));
                } else {
                    toastShow("Fast protection analyzer started, will take 1 min");
                }
                cbThreadDone.done(startSuccess);
            }
        });
    }
}
