package com.example.aventador.protectalarm.events;

import android.os.Bundle;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Aventador on 21/09/2017.
 */

/**
 * ActionEvent is used by EventBus. an ActionEvent is broadcast when a action is requested. (CONNECTION, START_PROTECTION, etc...)
 *
 * The difference between {@link ActionEvent} and {@link StateEvent} is that ActionEvent is a request to be executed
 * StateEvent is just an information broadcasted (Frequency changed, pandwarf dongle connected/disconnected, ...)
 */
public class ActionEvent extends Event<Action> {

    /**
     *
     * @param actionRequested
     * @param parameters
     */
    public ActionEvent(Action actionRequested, Bundle parameters) {
        super(actionRequested, parameters);
    }

    /**
     *
     * @param actionRequested
     * @param parameter
     */
    public ActionEvent(Action actionRequested, String parameter) {
        super(actionRequested, parameter);
    }

    /**
     *
     * @param actionRequested
     * @param parameters
     */
    public ActionEvent(Action actionRequested, HashMap<String, String> parameters) {
        super(actionRequested, parameters);
    }

    /**
     *
     * @return
     */
    public Action getActionRequested() {
        return getEvent();
    }

}
