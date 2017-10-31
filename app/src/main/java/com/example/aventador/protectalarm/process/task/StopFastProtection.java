package com.example.aventador.protectalarm.process.task;

import android.app.Activity;
import android.support.annotation.CallSuper;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.tools.Logger;

/**
 * Created by Aventador on 30/10/2017.
 */

class StopFastProtection extends Task {

    public StopFastProtection(Activity activity, ActionEvent actionEvent, GollumCallbackGetBoolean cbThreadDone) {
        super(activity, actionEvent,  cbThreadDone);
    }

    @CallSuper
    @Override
    public void run() {
        super.run();
        stopFastProtectionAnalyzer(new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean stopSuccess) {
                if (!stopSuccess) {
                    Logger.e(TAG, "impossible to stop fast protection, nota: check if specan is stopped");
                    toastShow("Fast Protection can't be stopped");
                } else {
                    toastShow("Fast Protection is stopped");
                }
                cbThreadDone.done(stopSuccess);
            }
        });
    }
}
