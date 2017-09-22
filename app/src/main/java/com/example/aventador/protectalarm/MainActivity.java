package com.example.aventador.protectalarm;

import android.*;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.State;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.process.Scanner;
import com.example.aventador.protectalarm.process.ThresholdFinder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        EventBus.getDefault().register(this);
    }

    @Override
    @CallSuper
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Used by EventBus
     * Called when a Publisher send a action to be executed.
     * @param actionEvent
     */
    @Subscribe
    public void onMessageEvent(ActionEvent actionEvent) {
        switch (actionEvent.getActionRequested()) {
            case CONNECT: {
                Scanner scanner = Scanner.getInstance();
                final String bleAddressTarget = actionEvent.getParameters().getString(Action.CONNECT.toString());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.BLUETOOTH},
                            REQUEST_CODE_ASK_PERMISSIONS);
                }

                scanner.connect(this, bleAddressTarget, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        EventBus.getDefault().postSticky(new StateEvent(State.CONNECTED, bleAddressTarget));
                    }
                });
                break;
            }
            case STOP_CONNECT: {
                Scanner.getInstance().stopConnect(this);
                break;
            }
            case DISCONNECT: {
                GollumDongle.getInstance(this).closeDevice();
                EventBus.getDefault().postSticky(new StateEvent(State.DISCONNECTED, ""));
                break;
            }
            case START_SEARCH_THRESHOLD: {
                String frequency = actionEvent.getParameters().getString(Parameter.FREQUENCY.toString());
                ThresholdFinder.getInstance().find(this, frequency, new GollumCallbackGetInteger() {
                    @Override
                    public void done(int rssi) {
                        HashMap<String, String> parameter = new HashMap<String, String>();
                        parameter.put(Parameter.RSSI_VALUE.toString(), String.valueOf(rssi));
                        EventBus.getDefault().postSticky(new StateEvent(State.SEARCH_OPTIMAL_PEAK_DONE, parameter));
                    }
                });
                break;
            }
            case STOP_SEARCH_THRESHOLD: {
                ThresholdFinder.getInstance().stopSpecan(this);
                break;
            }
        }
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
                Toast toast = Toast.makeText(this, "Opening device...", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
                break;
            }
            case DISCONNECTED: {
                Toast toast = Toast.makeText(this, "Closing device...", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
