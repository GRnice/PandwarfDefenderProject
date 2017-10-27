package com.example.aventador.protectalarm.events;

/**
 * Created by Giangrasso on 21/09/2017.
 */

public enum State {
    CONNECTED,
    DISCONNECTED,

    SEARCH_OPTIMAL_PEAK_DONE,
    SEARCH_OPTIMAL_PEAK_FAIL,

    FAST_PROTECTION_ANALYZER_DONE,
    FAST_PROTECTION_ANALYZER_FAIL,

    PROTECTION_FAIL,

    FREQUENCY_SELECTED,
    DB_TOLERANCE_SELECTED,

    ATTACK_DETECTED;
}
