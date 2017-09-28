package com.example.aventador.protectalarm.events;

/**
 * Created by Aventador on 21/09/2017.
 */

public enum State {
    CONNECTED,
    DISCONNECTED,

    SEARCH_OPTIMAL_PEAK_DONE,
    SEARCH_OPTIMAL_PEAK_FAIL,

    PROTECTION_FAIL,

    FREQUENCY_SELECTED,
    DB_TOLERANCE_SELECTED,

    ATTACK_DETECTED;
}
