package com.example.aventador.protectalarm;

import android.bluetooth.BluetoothAdapter;
import android.graphics.Color;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.tools.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.view.View.GONE;
import static com.example.aventador.protectalarm.tools.Tools.isValidAddressMac;

/**
 * A placeholder fragment containing a simple view.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private Button connectionButton;
    private ProgressBar scanProgressbar;
    private EditText addressMacEditText;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View bodyView = inflater.inflate(R.layout.fragment_home, container, false);

        scanProgressbar = (ProgressBar) bodyView.findViewById(R.id.scan_progressbar);
        scanProgressbar.setIndeterminate(true);
        scanProgressbar.setVisibility(GONE);

        addressMacEditText = (EditText) bodyView.findViewById(R.id.addressMacEditText);
        addressMacEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isValidAddressMac(addressMacEditText.getText().toString())) {
                    connectionButton.setVisibility(View.VISIBLE);
                    addressMacEditText.setTextColor(Color.GREEN);
                } else {
                    connectionButton.setVisibility(View.INVISIBLE);
                    addressMacEditText.setTextColor(Color.RED);
                }
            }
        });

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

    private void startScan() {
        if (BluetoothAdapter.getDefaultAdapter().getState() != BluetoothAdapter.STATE_ON) {
            Toast toast = Toast.makeText(getContext(), "Bluetooth must be started", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            return;
        }
        EventBus.getDefault().postSticky(new ActionEvent(Action.CONNECT, addressMacEditText.getText().toString()));
        scanProgressbar.setVisibility(View.VISIBLE);
        connectionButton.setText("STOP CONNECTION");
        connectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScan();
            }
        });
    }

    private void stopScan() {
        resetFragment();
        EventBus.getDefault().postSticky(new ActionEvent(Action.STOP_CONNECT, ""));
    }

    public void resetFragment() {
        connectionButton.setVisibility(View.INVISIBLE);
        connectionButton.setText("connection");
        connectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });
        if (isValidAddressMac(addressMacEditText.getText().toString())) {
            connectionButton.setVisibility(View.VISIBLE);
        } else {
            connectionButton.setVisibility(View.INVISIBLE);
        }
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
                connectionButton.setText("Disconnect");
                connectionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EventBus.getDefault().postSticky(new ActionEvent(Action.DISCONNECT, ""));
                    }
                });
                break;
            }
            case DISCONNECTED: {
                Logger.d(TAG, "DISCONNECTED");
                resetFragment();
                break;
            }
        }
    }
}
