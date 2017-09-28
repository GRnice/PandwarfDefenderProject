package com.example.aventador.protectalarm.events;

import android.os.Bundle;

import java.util.HashMap;

/**
 * Created by Aventador on 21/09/2017.
 */

/**
 *  StateEvent is used by EventBus. an StateEvent is broadcast when an action is complete. (ATTACK_DETECTED, CONNECTED, SEARCH_OPTIMAL_PEAK_DONE, etc...)
 *
 * The difference between {@link ActionEvent} and {@link StateEvent} is that ActionEvent is a request to be executed
 * StateEvent is just an information broadcasted (Frequency changed, pandwarf dongle connected/disconnected, ...)
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
