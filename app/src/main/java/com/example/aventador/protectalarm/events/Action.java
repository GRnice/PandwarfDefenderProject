package com.example.aventador.protectalarm.events;

/**
 * Created by Aventador on 21/09/2017.
 */

public enum Action {
    // ========= ACTION ===========
    /**
     * Used by all fragments.
     */
    CONNECT,
    DISCONNECT,

    /**
     * Used by Home fragment
     */
    STOP_CONNECT,

    /**
     * Used by Threshold fragment.
     */
    START_SEARCH_THRESHOLD,
    STOP_SEARCH_THRESHOLD,

    /**
     * Used by Fast fragment.
     */

    START_FAST_PROTECTION_ANALYZER,
    STOP_FAST_PROTECTION_ANALYZER,

    /**
     * Used by Guardian Fragment & Fast fragment.
     */
    START_PROTECTION,
    STOP_PROTECTION,
    START_JAMMING,

    /**
     * Not used.
     */
    UPDATE,
    STOP_JAMMING,
}
