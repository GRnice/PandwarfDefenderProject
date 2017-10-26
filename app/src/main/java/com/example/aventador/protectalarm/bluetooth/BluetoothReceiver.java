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
 * Created by Giangrasso on 29/09/2017.
 */

/**
 * BluetoothReceiver is able to register/unregister the app to the bluetooth events.
 *
 * When an event is catched thanks to the BroadcastReceiver @see: mReceiver
 * the callback "cbNewState" is called with the state of the bluetooth event.
 * this callback is given by the main activity.
 */
public class BluetoothReceiver {
    private static BluetoothReceiver instance;
    private AtomicBoolean register; // manages multi-threading.
    private GollumCallbackGetInteger cbNewState; // callback given by main activity.

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) && cbNewState != null) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                cbNewState.done(state); // the main activity will be notified.
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
     * Register the app to the bluetooth events.
     * If the register is already done -> return false.
     * @param context
     * @param cbNewState
     * @return
     */
    public boolean register(@NonNull Context context, @Nullable GollumCallbackGetInteger cbNewState) {
        if (register.compareAndSet(false, true)) { // set register to true, if the current value is false.
            if (cbNewState != null) {
                this.cbNewState = cbNewState;
            }
            context.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            return true;
        }

        return false;
    }

    /**
     * Unregister the app to the bluetooth events.
     * If the unregister is already done -> return false.
     *
     * @param context
     * @return
     */
    public boolean unregister(@NonNull Context context) {
        if (register.compareAndSet(true, false)) { // set register to false, if the current value is true.
            context.unregisterReceiver(mReceiver);
            return true;
        }
        return false;
    }
}
