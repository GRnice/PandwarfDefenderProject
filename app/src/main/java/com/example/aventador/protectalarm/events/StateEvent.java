package com.example.aventador.protectalarm.events;

import android.os.Bundle;

import java.util.HashMap;

/**
 * Created by Aventador on 21/09/2017.
 */

public class StateEvent extends Event<State> {

    /**
     *
     * @param actionRequested
     * @param parameters
     */
    public StateEvent(State actionRequested, Bundle parameters) {
        super(actionRequested, parameters);
    }

    /**
     *
     * @param actionRequested
     * @param parameter
     */
    public StateEvent(State actionRequested, String parameter) {
        super(actionRequested, parameter);
    }

    /**
     *
     * @param actionRequested
     * @param parameters
     */
    public StateEvent(State actionRequested, HashMap<String, String> parameters) {
        super(actionRequested, parameters);
    }

    /**
     *
     * @return
     */
    public State getState() {
        return getEvent();
    }

}
