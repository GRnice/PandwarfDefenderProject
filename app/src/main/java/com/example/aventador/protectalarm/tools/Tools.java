package com.example.aventador.protectalarm.tools;

/**
 * Created by Aventador on 26/09/2017.
 */

public class Tools {
    private static final String TAG = "Tools";
    public static boolean isValidFrequency(String frequency) {
        try {
            int freq = Integer.parseInt(frequency);
            Logger.d(TAG, "" + freq);
            if (freq > 300000000 && freq < 910000000) {
                return true;
            }
            return false;
        }catch (NumberFormatException e) {
            return false;
        }
    }
}
