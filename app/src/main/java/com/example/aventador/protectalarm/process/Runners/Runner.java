package com.example.aventador.protectalarm.process.Runners;

import android.app.Activity;
import android.support.annotation.Nullable;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Aventador on 21/10/2017.
 */

public abstract class Runner extends Thread {

    private static final String TAG = "Runner";
    protected Activity activity;
    protected int nbScans;
    protected int nbChannels;
    protected int nbSequences;

    protected AtomicBoolean run;

    public Runner(Activity activity, int nbScans, int nbSequences, int nbChannels) {
        this.activity = activity;
        this.nbScans = nbScans;
        this.nbSequences = nbSequences;
        this.nbChannels = nbChannels;
        this.run = new AtomicBoolean(false);
    }

    /**
     *
     * @return
     */
    @Nullable
    protected byte[] scan() {
        byte[] rssi_buffer = new byte[nbChannels];
        int check = GollumDongle.getInstance(activity).rfSpecanGetRssi(rssi_buffer, nbChannels);
        Logger.d(TAG, "check: " + check);
        if (check != nbChannels) {
            return null;
        }
        return rssi_buffer;
    }

    public int getNbSequences() {
        return nbSequences;
    }

    public int getNbScan() {
        return nbScans;
    }

    public synchronized void kill() {
        this.run.set(false);
    }
}
