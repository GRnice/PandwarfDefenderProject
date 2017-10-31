package com.example.aventador.protectalarm.process.task;

import android.app.Activity;
import android.support.annotation.CallSuper;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.StateEvent;

import org.greenrobot.eventbus.EventBus;

import static com.example.aventador.protectalarm.events.State.SEARCH_OPTIMAL_PEAK_FAIL;

/**
 * Created by Aventador on 30/10/2017.
 */

class StartThresholdSearch extends Task {

    public StartThresholdSearch(Activity activity, ActionEvent actionEvent, GollumCallbackGetBoolean cbThreadDone) {
        super(activity, actionEvent,  cbThreadDone);
    }

    @CallSuper
    @Override
    public void run() {
        super.run();
        String frequency = actionEvent.getParameters().getString(Parameter.FREQUENCY.toString());
        startThresholdSearch(frequency, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean startSuccess) {
                if (!startSuccess) {
                    toastShow("Fail to search threshold");
                    EventBus.getDefault().postSticky(new StateEvent(SEARCH_OPTIMAL_PEAK_FAIL, ""));
                }
                cbThreadDone.done(startSuccess);
            }
        });
    }
}
