package com.example.aventador.protectalarm.customViews;

/**
 * Created by Aventador on 03/10/2017.
 */

public class HistoryLog {
    public enum WARNING_LEVEL {
        HIGH,
        MEDIUM,
        LOW;
    }

    private WARNING_LEVEL warningLevel;
    private String message;

    public HistoryLog(WARNING_LEVEL warningLevel, String message) {
        this.warningLevel = warningLevel;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public WARNING_LEVEL getWarningLevel() {
        return warningLevel;
    }
}
