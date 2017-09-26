package com.example.aventador.protectalarm.tools;

import android.util.Log;

/**
 * Created by Aventador on 26/09/2017.
 */

public class Logger {
    public static void d(String title, String body) {
        Log.d(title, body);
    }

    public static void e(String title, String body) {
        Log.e(title, body);
    }

    public static void w(String title, String body) {
        Log.w(title, body);
    }
    public static void v(String title, String body) {
        Log.v(title, body);
    }
    public static void i(String title, String body) {
        Log.i(title, body);
    }

}
