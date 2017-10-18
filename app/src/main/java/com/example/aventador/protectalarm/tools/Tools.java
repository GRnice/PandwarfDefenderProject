package com.example.aventador.protectalarm.tools;

import com.example.aventador.protectalarm.storage.Configuration;

/**
 * Created by Aventador on 26/09/2017.
 */

public class Tools {
    private static final String TAG = "Tools";
    public static final boolean isValidFrequency(String frequency) {
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

    public static final boolean isValidDb(String db) {
        try {
            int freq = Integer.parseInt(db);
            Logger.d(TAG, "" + freq);
            if (freq >= -120 && freq <= 0) {
                return true;
            }
            return false;
        }catch (NumberFormatException e) {
            return false;
        }
    }

    public static final boolean isValidAddressMac(String addressMac) {
        if (addressMac.length() != 17) {
            Logger.e(TAG, "size addressMac is wrong");
            return false;
        }
        String[] addressSplitted = addressMac.split(":");
        if (addressSplitted.length != 6) {
            Logger.e(TAG, "splitting of addressMac is wrong");
            return false;
        }
        for (int i = 0; i < addressSplitted.length; i++) {
            Logger.d(TAG, "addressSplitted[" + i + "] = " + addressSplitted[i]);
            if (!containHexaCaracters(addressSplitted[i])) {
                return false;
            }
        }

        return true;
    }

    public static final boolean containHexaCaracters(String supposedHexa) {
        supposedHexa = supposedHexa.toUpperCase();
        int codeAscii;
        for (int i = 0; i < supposedHexa.length(); i++) {
            codeAscii = supposedHexa.charAt(i) - '0';
            Logger.d(TAG, "code: codeAscii: " + codeAscii + " char: " + supposedHexa.charAt(i));
            if (codeAscii > 9 || codeAscii < 0) { // check if it's not a number

            } else {
                continue;
            }

            codeAscii = supposedHexa.charAt(i) - 'A'; // check if it's not a letter in [A - F]
            Logger.d(TAG, "code: codeAscii: " + codeAscii + " char: " + supposedHexa.charAt(i));
            if (codeAscii > 5 || codeAscii < 0) {

            } else {
                continue;
            }

            return false;
        }
        return true; // empty is not HEXA
    }

}
