package com.example.aventador.protectalarm.callbacks;

import com.example.aventador.protectalarm.storage.Configuration;

/**
 * Created by Giangrasso on 26/10/2017.
 */

public interface GollumCallbackGetConfiguration {
    public void done(boolean success, Configuration configuration);
}
