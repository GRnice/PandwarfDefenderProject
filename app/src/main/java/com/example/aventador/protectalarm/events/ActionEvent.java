package com.example.aventador.protectalarm.events;

import android.os.Bundle;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Aventador on 21/09/2017.
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
