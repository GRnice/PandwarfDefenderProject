package com.example.aventador.protectalarm.storage;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Since;

/**
 * Created by Aventador on 11/10/2017.
 */

public class Configuration {
    @Expose
    @SerializedName("db_tolerance")
    private int dbTolerance = 0;

    @Expose
    @SerializedName("frequency")
    private int frequency = 0;

    @Expose
    @SerializedName("peak_tolerance")
    private int peakTolerance = 0;

    @Expose
    @SerializedName("margin_error")
    private int marginError = 0;

    public Configuration() {

    }

    public Configuration(int dbTolerance, int frequency, int peakTolerance, int marginError) {
        this.dbTolerance = dbTolerance;
        this.frequency = frequency;
        this.peakTolerance = peakTolerance;
        this.marginError = marginError;
    }

    public int getDbTolerance() {
        return dbTolerance;
    }

    public void setDbTolerance(int dbTolerance) {
        this.dbTolerance = dbTolerance;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getPeakTolerance() {
        return peakTolerance;
    }

    public void setPeakTolerance(int peakTolerance) {
        this.peakTolerance = peakTolerance;
    }

    public int getMarginError() {
        return marginError;
    }

    public void setMarginError(int marginError) {
        this.marginError = marginError;
    }

    @Override
    public String toString() {
        return "Configuration: " +
                "{" +
                "frequency: " + frequency + " ," +
                "dbTolerance: " + dbTolerance + " ," +
                "peak tolerance: " + peakTolerance + " ," +
                "margin error: " + marginError +
                "}";
    }
}
