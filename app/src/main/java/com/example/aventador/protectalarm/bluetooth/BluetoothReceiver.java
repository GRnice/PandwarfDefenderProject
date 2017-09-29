package com.example.aventador.protectalarm.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Aventador on 29/09/2017.
 */

public class BluetoothReceiver {
    private static BluetoothReceiver instance;
    private AtomicBoolean register;
    private GollumCallbackGetInteger cbNewState;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) && cbNewState != null) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                cbNewState.done(state);
            }
        }
    };

    public static BluetoothReceiver getInstance() {
        if (instance == null) {
            instance = new BluetoothReceiver();
        }

        return instance;
    }

    private BluetoothReceiver() {
        register = new AtomicBoolean(false);
    }

    /**
     *
     * @param context
     * @param cbNewState
     * @return
     */
    public boolean register(@NonNull Context context, @Nullable GollumCallbackGetInteger cbNewState) {
        if (register.compareAndSet(false, true)) {
            if (cbNewState != null) {
                this.cbNewState = cbNewState;
            }
            context.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            return true;
        }

        return false;
    }

    /**
     *
     * @param context
     * @return
     */
    public boolean unregister(@NonNull Context context) {
        if (register.compareAndSet(true, false)) {
            context.unregisterReceiver(mReceiver);
            return true;
        }
        return false;
    }
}
