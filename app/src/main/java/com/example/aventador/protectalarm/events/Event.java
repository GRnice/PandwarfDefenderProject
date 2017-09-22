package com.example.aventador.protectalarm.events;

import android.os.Bundle;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Aventador on 21/09/2017.
 */

public abstract class Event<T> {
    private T event;
    private Bundle parameters;

    /**
     *
     * @param actionRequested
     * @param parameters
     */
    public Event(T actionRequested, Bundle parameters) {
        this.event = actionRequested;
        this.parameters = parameters;
    }

    /**
     *
     * @param actionRequested
     * @param parameter
     */
    public Event(T actionRequested, String parameter) {
        this.event = actionRequested;
        this.parameters = new Bundle();
        this.parameters.putString(actionRequested.toString(), parameter);
    }

    /**
     *
     * @param actionRequested
     * @param parameters
     */
    public Event(T actionRequested, HashMap<String, String> parameters) {
        Bundle bundle = new Bundle();
        Iterator<String> keys = parameters.keySet().iterator();
        String key;
        while(keys.hasNext()) {
            key = keys.next();
            bundle.putString(key, parameters.get(key));
        }
        this.event = actionRequested;
        this.parameters = bundle;
    }

    /**
     *
     * @return
     */
    protected final T getEvent() {
        return event;
    }

    /**
     *
     * @return
     */
    public final Bundle getParameters() {
        return parameters;
    }
}
