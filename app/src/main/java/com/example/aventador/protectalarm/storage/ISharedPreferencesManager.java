package com.example.aventador.protectalarm.storage;

import android.content.Context;

/**
 * Created by Aventador on 06/11/2017.
 */

public interface ISharedPreferencesManager {
    public String fromSharedPrefsGet(Context context, String key);
    public boolean fromSharedPrefsPut(Context context, String key, String value);
}
