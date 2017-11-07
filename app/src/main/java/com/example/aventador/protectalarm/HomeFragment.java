package com.example.aventador.protectalarm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.callbacks.DongleCallbacks;
import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.State;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.process.Pandwarf;
import com.example.aventador.protectalarm.tools.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import no.nordicsemi.android.nrftoolbox.scanner.ExtendedBluetoothDevice;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerListener;

import static android.view.View.GONE;
import static com.example.aventador.protectalarm.tools.Tools.isValidAddressMac;

/**
 * A placeholder fragment containing a simple view.
 */

/**
 * HomeFragment allows the user to select the pandwarf mac address to connect
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private Button connectionButton;
    private AtomicBoolean scanIsRunning;
    private AtomicBoolean connexionInProgress;
    private ProgressBar scanProgressbar;
    private ListView listView;
    private TextView pandwarfConnectedTextView;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View bodyView = inflater.inflate(R.layout.fragment_home, container, false);

        scanProgressbar = (ProgressBar) bodyView.findViewById(R.id.scan_progressbar);
        scanProgressbar.setIndeterminate(true);
        scanProgressbar.setVisibility(GONE);
        pandwarfConnectedTextView = (TextView) bodyView.findViewById(R.id.pandwarf_connected_text_view);
        listView = (ListView) bodyView.findViewById(R.id.pandwarf_list_view);
        listView.setAdapter(new ListAdapterCustom(getContext(), 0, new ArrayList<ExtendedBluetoothDevice>()));
        scanIsRunning = new AtomicBoolean(false);
        connexionInProgress = new AtomicBoolean(false);
        connectionButton = (Button) bodyView.findViewById(R.id.connection_button);
        resetFragment();

        return bodyView;
    }

    @Override
    @Subscribe
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    @CallSuper
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void connect(ExtendedBluetoothDevice extendedBluetoothDevice) {
        if (!Pandwarf.getInstance().isConnected() && connexionInProgress.compareAndSet(false, true)) {
            GollumDongle.getInstance(getActivity()).openDevice(extendedBluetoothDevice, true, false, new DongleCallbacks());
        }
    }

    public void disconnect() {
        killAllProcess(new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                resetFragment();
                Pandwarf.getInstance().close(getActivity());
                EventBus.getDefault().postSticky(new StateEvent(State.DISCONNECTED, ""));
            }
        });
    }

    /**
     * Reset the progress bar and connectionButton.
     * stop the scan process.
     */
    private void stopScan() {
        if (scanIsRunning.compareAndSet(true, false)) {
            resetFragment();
            GollumDongle.getInstance(getActivity()).stopSearchDevice();
        }
    }

    /**
     * Search all pandwarf in the environment.
     * Start scan process.
     */
    private void startScan() {

        if (BluetoothAdapter.getDefaultAdapter().getState() != BluetoothAdapter.STATE_ON) {
            Toast toast = Toast.makeText(getContext(), "Bluetooth must be started", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            return;
        }

        scanProgressbar.setVisibility(View.VISIBLE);
        connectionButton.setText(getString(R.string.stop_scan_pandwarf));
        connectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScan();
            }
        });


        if (!scanIsRunning.compareAndSet(false, true)) {
            Logger.e(TAG, "scan is already running");
            return;
        }

        Logger.d(TAG, "start search: ");
        GollumDongle.getInstance(getActivity()).searchDevice(new ScannerListener() {
            @Override
            public void onSignalNewDevice(ExtendedBluetoothDevice extendedBluetoothDevice) {
                Logger.d(TAG, "onSignalNewDevice: " + extendedBluetoothDevice.getAddress());
                ListAdapterCustom listAdapterCustom = ((ListAdapterCustom) listView.getAdapter());
                listAdapterCustom.add(extendedBluetoothDevice);
                listAdapterCustom.notifyDataSetChanged();
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
     * Reset the progress bar and connectionButton.
     */
    public void resetFragment() {
        connectionButton.setText(getString(R.string.start_scan_pandwarf));
        scanProgressbar.setVisibility(View.GONE);
        connectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });
    }

    /**
     * Kill all process, scan, threshold discovery, protection, jamming, fast protection analyzer
     * /!\ Duplicated in Main2Activity /!\
     */
    public void killAllProcess(final GollumCallbackGetBoolean killDone) {
        Pandwarf.getInstance().stopFastProtectionAnalyzer(getActivity(), new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                Pandwarf.getInstance().stopDiscovery(getActivity(), new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        Pandwarf.getInstance().stopGuardian(getActivity(), new GollumCallbackGetBoolean() {
                            @Override
                            public void done(boolean b) {
                                Pandwarf.getInstance().stopJamming(getActivity(), true, new GollumCallbackGetBoolean() {
                                    @Override
                                    public void done(boolean b) {
                                        killDone.done(true);
                                        Pandwarf.getInstance().close(getActivity());
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

    }

    /**
     * Used by EventBus
     * Called when a Publisher send a action to be executed.
     * @param actionEvent
     */
    @Subscribe
    public void onMessageEvent(ActionEvent actionEvent) {
    }

    /**
     * Used by EventBus
     * Called when a Publisher send a state.
     * @param stateEvent
     */
    @Subscribe(threadMode =  ThreadMode.MAIN)
    public void onMessageEvent(StateEvent stateEvent) {
        Logger.d(TAG, "onMessageEvent: State Event: " + stateEvent.getState());
        switch (stateEvent.getState()) {
            case CONNECTED: {
                Logger.d(TAG, "CONNECTED");
                scanProgressbar.setVisibility(GONE);
                connexionInProgress.set(false);
                pandwarfConnectedTextView.setText("Pandwarf: " + GollumDongle.getInstance(getActivity()).getCurrentBleDeviceMacAddress());
                stopScan();
                connectionButton.setText(getString(R.string.disconnect_pandwarf));
                connectionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        disconnect();
                    }
                });
                break;
            }
            case DISCONNECTED: {
                Logger.d(TAG, "DISCONNECTED");
                pandwarfConnectedTextView.setText("Pandwarf:");
                resetFragment();
                break;
            }
        }
    }

    private class ListAdapterCustom extends ArrayAdapter<ExtendedBluetoothDevice> {

        private List<ExtendedBluetoothDevice> extendedBluetoothDevices;
        public ListAdapterCustom(@NonNull Context context, int resource) {
            super(context, resource);
        }

        public ListAdapterCustom(@NonNull Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
            extendedBluetoothDevices = new ArrayList<>();
        }

        public ListAdapterCustom(@NonNull Context context, int resource, @NonNull ExtendedBluetoothDevice[] objects) {
            super(context, resource, objects);
            extendedBluetoothDevices = Arrays.asList(objects);
        }

        public ListAdapterCustom(@NonNull Context context, int resource, int textViewResourceId, @NonNull ExtendedBluetoothDevice[] objects) {
            super(context, resource, textViewResourceId, objects);
            extendedBluetoothDevices = Arrays.asList(objects);
        }

        public ListAdapterCustom(@NonNull Context context, int resource, @NonNull List<ExtendedBluetoothDevice> objects) {
            super(context, resource, objects);
            extendedBluetoothDevices = objects;

        }

        public ListAdapterCustom(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<ExtendedBluetoothDevice> objects) {
            super(context, resource, textViewResourceId, objects);
            extendedBluetoothDevices = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.row_connection_listview, parent, false);;
            } else {
                view = convertView;
            }

            final ExtendedBluetoothDevice extendedBluetoothDevice = getItem(position);
            if (extendedBluetoothDevice == null) {
                return view;
            }
            TextView bleAddressTextView = (TextView) view.findViewById(R.id.ble_address_text_view);
            bleAddressTextView.setText("Pandwarf: " + extendedBluetoothDevice.getAddress().toUpperCase());
            bleAddressTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    connect(extendedBluetoothDevice);
                }
            });

            return view;
        }
    }
}
