package com.example.aventador.protectalarm.process;

import android.app.Activity;
import android.os.AsyncTask;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.ble.GollumBleManager;
import com.comthings.gollum.api.gollumandroidlib.ble.GollumBleManagerCallbacks;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.callbacks.DongleCallbacks;

import java.util.concurrent.atomic.AtomicBoolean;

import no.nordicsemi.android.nrftoolbox.scanner.ExtendedBluetoothDevice;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerListener;

/**
 * Created by Aventador on 21/09/2017.
 */

public class Scanner {
    private static Scanner instance;
    private AtomicBoolean scanRunning;

    public static Scanner getInstance() {
        if (instance == null) {
            instance = new Scanner();
        }
        return instance;
    }

    private Scanner() {
        scanRunning = new AtomicBoolean(false);
    }

    /**
     *
     * @param activity
     * @param bleAddress
     * @param cbDone
     */
    public void connect(final Activity activity, final String bleAddress, final GollumCallbackGetBoolean cbDone) {
        if (scanRunning.get()) {
            return;
        }
        scanRunning.set(true);

        GollumDongle.getInstance(activity).searchDevice(new ScannerListener() {
            @Override
            public void onSignalNewDevice(ExtendedBluetoothDevice extendedBluetoothDevice) {
                if (extendedBluetoothDevice.getAddress().toLowerCase().equals(bleAddress.toLowerCase())) {
                    GollumDongle.getInstance(activity).openDevice(extendedBluetoothDevice, true, false, new DongleCallbacks());
                    stopConnect(activity);
                    scanRunning.set(false);
                    cbDone.done(true);
                }
            }

            @Override
            public void onSignalUpdateDevice(ExtendedBluetoothDevice extendedBluetoothDevice) {

            }

            @Override
            public void onSignalEndScan(Exception e) {

            }
        });
    }

    /**
     *
     * @param activity
     */
    public void stopConnect(final Activity activity) {
        if (scanRunning.get()) {
            GollumDongle.getInstance(activity).stopSearchDevice();
        }
    }
}
