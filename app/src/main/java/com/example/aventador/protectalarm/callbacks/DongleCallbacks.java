package com.example.aventador.protectalarm.callbacks;

import com.comthings.gollum.api.gollumandroidlib.ble.GollumBleManagerCallbacks;
import com.example.aventador.protectalarm.events.State;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.tools.Logger;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Giangrasso on 21/09/2017.
 */

/**
 * Receives differents signals from the PandwaRF.
 * the only signal processed is onDeviceReady.
 */
public class DongleCallbacks implements GollumBleManagerCallbacks {
    private final static String TAG = "DongleCallbacks";

    @Override
    public void onDeviceReady() {
        Logger.d(TAG, "onDeviceReady");
        EventBus.getDefault().postSticky(new StateEvent(State.CONNECTED, "")); // Send an event "CONNECTED"
        // Main activity and all fragments catch this event. /!\ /!\
    }

    @Override
    public void onDeviceNameValueReceived(String s) {

    }

    @Override
    public void onApparenceValueReceived(int i) {

    }

    @Override
    public void onConnectionParamsValueReceived(int i, int i1, int i2, int i3) {

    }

    @Override
    public void onManufacturerNameReceived(String s) {

    }

    @Override
    public void onModelNumberReceived(String s) {

    }

    @Override
    public void onSerialNumberReceived(String s) {

    }

    @Override
    public void onHardwareRevisionReceived(String s) {

    }

    @Override
    public void onSoftwareRevisionReceived(String s) {

    }

    @Override
    public void onFirmwareRevisionReceived(String s) {

    }

    @Override
    public void onSoftwareVersionReceived(String s) {

    }

    @Override
    public void onFirmwareVersionReceived(String s) {

    }

    @Override
    public void onRxValueReceived(byte[] bytes) {

    }

    @Override
    public void onLoopBackModeReceived(boolean b) {

    }

    @Override
    public void onButtonPushedReceived(int i) {

    }

    @Override
    public void onBusConfigReceived(String s) {

    }

    @Override
    public void onBusConfigUsbAllowed(Boolean aBoolean) {

    }

    @Override
    public void onButtonPushedNotificationStatusReceived(Boolean aBoolean) {

    }

    @Override
    public void onBleErrorReceived(int i, int i1, String s) {

    }

    @Override
    public void onBatteryNotificationStatusReceived(Boolean aBoolean) {

    }

    @Override
    public void onDeviceConnecting() {

    }

    @Override
    public void onDeviceConnected() {
    }

    @Override
    public void onDeviceDisconnecting() {
    }

    @Override
    public void onDeviceDisconnected() {

    }

    @Override
    public void onLinklossOccur() {

    }

    @Override
    public void onServicesDiscovered(boolean b) {

    }



    @Override
    public void onBatteryValueReceived(int i) {

    }

    @Override
    public void onRssiValueReceived(int i) {

    }

    @Override
    public void onBondingRequired() {

    }

    @Override
    public void onBonded() {

    }

    @Override
    public void onError(String s, int i) {

    }

    @Override
    public void onDeviceNotSupported() {

    }
}
