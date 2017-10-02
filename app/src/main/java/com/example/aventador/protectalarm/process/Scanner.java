package com.example.aventador.protectalarm.process;

import android.app.Activity;
import android.os.AsyncTask;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.ble.GollumBleManager;
import com.comthings.gollum.api.gollumandroidlib.ble.GollumBleManagerCallbacks;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.callbacks.DongleCallbacks;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

import no.nordicsemi.android.nrftoolbox.scanner.ExtendedBluetoothDevice;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerListener;

/**
 * Created by Aventador on 21/09/2017.
 */

/**
 * This class is dedicated to detected the differents PandwaRF
 * when a targeted pandwaRF is found, it's opened
 */
public class Scanner {
    private static final String TAG = "Scanner";
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
     * Search a specific mac address, when it's found, the dongle associated is open
     * @param activity
     * @param bleAddress the ble address targeted.
     * @param cbDone will be called when a pandwarf will be open.
     */
    public void connect(final Activity activity, final String bleAddress, final GollumCallbackGetBoolean cbDone) {
        if (scanRunning.get()) {
            Logger.e(TAG, "scan is already running");
            return;
        }
        scanRunning.set(true);

        final String bleAddressTargeted = bleAddress.toLowerCase();
        Logger.d(TAG, "start search: ");
        GollumDongle.getInstance(activity).searchDevice(new ScannerListener() {
            @Override
            public void onSignalNewDevice(ExtendedBluetoothDevice extendedBluetoothDevice) {
                Logger.d(TAG, "onSignalNewDevice: " + extendedBluetoothDevice.getAddress());
                String bleAddressFound = extendedBluetoothDevice.getAddress().toLowerCase();
                if (bleAddressFound.equals(bleAddressTargeted)) { // if ble found match with the ble address targeted
                    // open this device.
                    GollumDongle.getInstance(activity).openDevice(extendedBluetoothDevice, true, false, new DongleCallbacks());
                    stopConnect(activity); // stop the scan
                    scanRunning.set(false); // scan not running.
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
        if (scanRunning.compareAndSet(true, false)) {
            GollumDongle.getInstance(activity).stopSearchDevice();
        }
    }
}
