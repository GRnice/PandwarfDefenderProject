package com.example.aventador.protectalarm.customViews;

/**
 * Created by Giangrasso on 03/10/2017.
 */

/**
 * HistoryLog is a message containing date, message and a warning level.
 * It used by Guardian fragment when a event must be added to the list of logs.
 */
public class HistoryLog {
    public enum WARNING_LEVEL {
        HIGH,
        MEDIUM,
        LOW;
    }

    private WARNING_LEVEL warningLevel;
    private String message;
    private String date;

    public HistoryLog(WARNING_LEVEL warningLevel, String date, String message) {
        this.warningLevel = warningLevel;
        this.message = message;
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public WARNING_LEVEL getWarningLevel() {
        return warningLevel;
    }

    public String getDate() {
        return date;
    }
}
