package com.example.aventador.protectalarm.events;

import android.os.Bundle;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Aventador on 21/09/2017.
 */

/**
 * Event, used by EventBus. this abstract class must be extends by a concret Event
 * an Event has a subject represented by the class T
 * the subject is following by a set of Parameters, see: {@link Parameter}
 * example : see: {@link ActionEvent}
 * @param <T>
 */
public abstract class Event<T> {
    private T event;
    private Bundle parameters;

    /**
     *
     * @param event
     * @param parameters
     */
    public Event(T event, Bundle parameters) {
        this.event = event;
        this.parameters = parameters;
    }

    /**
     *
     * @param event
     * @param parameter
     */
    public Event(T event, String parameter) {
        this.event = event;
        this.parameters = new Bundle();
        this.parameters.putString(event.toString(), parameter);
    }

    /**
     *
     * @param event
     * @param parameters
     */
    public Event(T event, HashMap<String, String> parameters) {
        Bundle bundle = new Bundle();
        Iterator<String> keys = parameters.keySet().iterator();
        String key;
        while(keys.hasNext()) {
            key = keys.next();
            bundle.putString(key, parameters.get(key));
        }
        this.event = event;
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
