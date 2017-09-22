package com.example.aventador.protectalarm;

import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Event;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.StateEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;

import static android.view.View.GONE;
import static com.example.aventador.protectalarm.events.Action.STOP_CONNECT;

/**
 * A placeholder fragment containing a simple view.
 */
public class HomeFragment extends Fragment {

    private Button fastConnection;
    private ProgressBar scanProgressbar;

    private LinearLayout layoutSelectFrequency;

    private LinearLayout layoutSearchOptimalThreshold;
    private ProgressBar progressBarSearchOptimalThreshold;
    private TextView rssiTextView;
    private Button searchOptimalThresholdButton;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View bodyView = inflater.inflate(R.layout.fragment_home, container, false);
        fastConnection = (Button) bodyView.findViewById(R.id.factConnectionButton);
        fastConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });

        scanProgressbar = (ProgressBar) bodyView.findViewById(R.id.scan_progressbar);
        scanProgressbar.setIndeterminate(true);
        scanProgressbar.setVisibility(GONE);

        // -------------------- //

        layoutSelectFrequency = (LinearLayout) bodyView.findViewById(R.id.frequency_select_layout);
        layoutSelectFrequency.setVisibility(GONE);

        // -------------------- //
        layoutSearchOptimalThreshold = (LinearLayout) bodyView.findViewById(R.id.layout_search_optimal);
        layoutSearchOptimalThreshold.setVisibility(GONE);
        progressBarSearchOptimalThreshold = (ProgressBar) bodyView.findViewById(R.id.progressBarSearchOptimalThreshoold);
        progressBarSearchOptimalThreshold.setVisibility(View.INVISIBLE);
        rssiTextView = (TextView) bodyView.findViewById(R.id.rssiValuetextView);
        searchOptimalThresholdButton = (Button) bodyView.findViewById(R.id.searchOptimalThresholdButton);
        searchOptimalThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearchOptimalThreshold();
            }
        });

        return bodyView;
    }

    private void startSearchOptimalThreshold() {
        progressBarSearchOptimalThreshold.setVisibility(View.VISIBLE);
        searchOptimalThresholdButton.setText("Stop Searching");
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(Parameter.FREQUENCY.toString(), "433000000");
        EventBus.getDefault().postSticky(new ActionEvent(Action.START_SEARCH_THRESHOLD, parameters));
        searchOptimalThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSearchOptimalThreshold();
            }
        });
    }

    private void stopSearchOptimalThreshold() {
        progressBarSearchOptimalThreshold.setVisibility(View.GONE);
        searchOptimalThresholdButton.setText("Search Optimal Threshold");
        EventBus.getDefault().postSticky(new ActionEvent(Action.STOP_SEARCH_THRESHOLD, ""));
        searchOptimalThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearchOptimalThreshold();
            }
        });
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
        EventBus.getDefault().postSticky(new ActionEvent(Action.CONNECT, "EC:5A:4E:57:AD:99"));
        scanProgressbar.setVisibility(View.VISIBLE);
        fastConnection.setText("STOP CONNECTION");
        fastConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScan();
            }
        });
    }

    private void stopScan() {
        scanProgressbar.setVisibility(View.INVISIBLE);
        fastConnection.setText("Fast connection");
        fastConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
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
    @Subscribe
    public void onMessageEvent(StateEvent stateEvent) {
        switch (stateEvent.getState()) {
            case CONNECTED: {
                scanProgressbar.setVisibility(GONE);
                fastConnection.setText("Disconnect");
                layoutSelectFrequency.setVisibility(View.VISIBLE);
                layoutSearchOptimalThreshold.setVisibility(View.VISIBLE);
                fastConnection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EventBus.getDefault().postSticky(new ActionEvent(Action.DISCONNECT, ""));
                    }
                });
                break;
            }
            case DISCONNECTED: {
                layoutSelectFrequency.setVisibility(View.GONE);
                layoutSearchOptimalThreshold.setVisibility(View.GONE);
                fastConnection.setText("Fast connection");

                fastConnection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startScan();
                    }
                });
                break;
            }
            case SEARCH_OPTIMAL_PEAK_DONE:
            {
                progressBarSearchOptimalThreshold.setVisibility(View.GONE);
                searchOptimalThresholdButton.setText("Search Optimal Threshold");
                String rssi = stateEvent.getParameters().getString(Parameter.RSSI_VALUE.toString());
                rssiTextView.setText("Threshold: " + rssi);
                searchOptimalThresholdButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startSearchOptimalThreshold();
                    }
                });
                break;
            }
        }
    }
}
