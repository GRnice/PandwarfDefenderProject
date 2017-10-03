package com.example.aventador.protectalarm;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.WindowManager;
import android.widget.Toast;

import com.comthings.gollum.api.gollumandroidlib.GollumDongle;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.bluetooth.BluetoothReceiver;
import com.example.aventador.protectalarm.events.Action;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.Event;
import com.example.aventador.protectalarm.events.Parameter;
import com.example.aventador.protectalarm.events.State;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.process.Jammer;
import com.example.aventador.protectalarm.process.Scanner;
import com.example.aventador.protectalarm.process.WatchMan;
import com.example.aventador.protectalarm.tools.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static com.example.aventador.protectalarm.events.State.ATTACK_DETECTED;
import static com.example.aventador.protectalarm.events.State.DISCONNECTED;
import static com.example.aventador.protectalarm.events.State.PROTECTION_FAIL;
import static com.example.aventador.protectalarm.events.State.SEARCH_OPTIMAL_PEAK_FAIL;

public class Main2Activity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final String TAG = "Main2Activity";
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private FloatingActionButton fab;
    private int bluetoothState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.addFragment(new HomeFragment(), "Pairing");
        mSectionsPagerAdapter.addFragment(new ThresholdFragment(), "Threshold Finder");
        mSectionsPagerAdapter.addFragment(new GuardianFragment(), "Protection");

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(mViewPager.getAdapter().getCount());
        mViewPager.addOnPageChangeListener(this);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        bluetoothState = BluetoothAdapter.getDefaultAdapter().getState();
        BluetoothReceiver.getInstance().register(this, new GollumCallbackGetInteger() {
            @Override
            public void done(int state) {
                newStateDetected(state);
            }
        });
        Jammer.getInstance().init(this);
        EventBus.getDefault().register(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.BLUETOOTH},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }

    }

    private void newStateDetected(int state) {
        bluetoothState = state;
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                //Indicates the local Bluetooth adapter is off.
                break;

            case BluetoothAdapter.STATE_TURNING_ON:
                //Indicates the local Bluetooth adapter is turning on. However local clients should wait for STATE_ON before attempting to use the adapter.
                break;

            case BluetoothAdapter.STATE_ON:
                //Indicates the local Bluetooth adapter is on, and ready for use.
                break;

            case BluetoothAdapter.STATE_TURNING_OFF:
                killAllProcess(new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        EventBus.getDefault().postSticky(new StateEvent(DISCONNECTED, ""));
                    }
                });
                //Indicates the local Bluetooth adapter is turning off. Local clients should immediately attempt graceful disconnection of any remote links.
                break;
        }
    }

    /**
     * Kill all process, scan, threshold discovery, protection, jamming
     */
    public void killAllProcess(final GollumCallbackGetBoolean killDone) {
        Scanner.getInstance().stopConnect(this);
        WatchMan.getInstance().stopDiscovery(this, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                WatchMan.getInstance().stopGuardian(Main2Activity.this, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        Jammer.getInstance().stopJamming(true, new GollumCallbackGetBoolean() {
                            @Override
                            public void done(boolean b) {
                                killDone.done(true);
                                GollumDongle.getInstance(Main2Activity.this).closeDevice();
                            }
                        });
                    }
                });
            }
        });
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
        getMenuInflater().inflate(R.menu.menu_main2, menu);
        return true;
    }

    private void startGuardian(final String frequency, final String dbTolerance, final int peakTolerance, final int marginError) {
        WatchMan.getInstance().startGuardian(this, Integer.valueOf(frequency),
                Integer.valueOf(dbTolerance), peakTolerance, marginError, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean startSuccess) {
                if (!startSuccess) {
                    Toast error = Toast.makeText(Main2Activity.this, "Fail to start protection", Toast.LENGTH_SHORT);
                    error.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    error.show();
                    EventBus.getDefault().postSticky(new StateEvent(PROTECTION_FAIL, ""));
                }
            }
        }, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                HashMap<String, String> parametersAttackDetected = new HashMap<>();
                parametersAttackDetected.put(Parameter.DATE.toString(), getCurrentTime());
                EventBus.getDefault().postSticky(new StateEvent(ATTACK_DETECTED, parametersAttackDetected));

                HashMap<String, String> parameters = new HashMap<>();
                parameters.put(Parameter.FREQUENCY.toString(), frequency);
                parameters.put(Parameter.RSSI_VALUE.toString(), dbTolerance);
                parameters.put(Parameter.PEAK_TOLERANCE.toString(), String.valueOf(peakTolerance));
                parameters.put(Parameter.MARGIN_ERROR.toString(), String.valueOf(marginError));

                EventBus.getDefault().postSticky(new ActionEvent(Action.START_JAMMING, parameters));
            }
        });


    }

    /**
     * Used by EventBus
     * Called when a Publisher send a action to be executed.
     * @param actionEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final ActionEvent actionEvent) {
        switch (actionEvent.getActionRequested()) {
            case CONNECT: {
                Scanner scanner = Scanner.getInstance();
                final String bleAddressTarget = actionEvent.getParameters().getString(Action.CONNECT.toString());

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
                killAllProcess(new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        GollumDongle.getInstance(Main2Activity.this).closeDevice();
                        EventBus.getDefault().postSticky(new StateEvent(State.DISCONNECTED, ""));
                    }
                });
                break;
            }
            case START_SEARCH_THRESHOLD: {
                String frequency = actionEvent.getParameters().getString(Parameter.FREQUENCY.toString());
                WatchMan.getInstance().startDiscovery(this, Integer.valueOf(frequency), new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean startSuccess) {
                        if (!startSuccess) {
                            Toast error = Toast.makeText(Main2Activity.this, "Fail to search threshold", Toast.LENGTH_SHORT);
                            error.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                            error.show();

                            EventBus.getDefault().postSticky(new StateEvent(SEARCH_OPTIMAL_PEAK_FAIL, ""));
                        }
                    }
                }, new GollumCallbackGetInteger() {
                    @Override
                    public void done(final int rssi) {
                        Log.d(TAG, "Threshold found");
                        WatchMan.getInstance().stopDiscovery(Main2Activity.this, new GollumCallbackGetBoolean() {
                            @Override
                            public void done(boolean b) {
                                HashMap<String, String> parameter = new HashMap<String, String>();
                                parameter.put(Parameter.RSSI_VALUE.toString(), String.valueOf(rssi));
                                EventBus.getDefault().postSticky(new StateEvent(State.SEARCH_OPTIMAL_PEAK_DONE, parameter));
                            }
                        });
                    }
                });



                break;
            }
            case STOP_SEARCH_THRESHOLD: {
                WatchMan.getInstance().stopDiscovery(this, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {

                    }
                });
                break;
            }
            case START_PROTECTION: {
                final String frequency = actionEvent.getParameter(Parameter.FREQUENCY);
                String dbTolerance =  actionEvent.getParameter(Parameter.RSSI_VALUE);
                int peakTolerance = Integer.valueOf(actionEvent.getParameter(Parameter.PEAK_TOLERANCE));
                int marginError = Integer.valueOf(actionEvent.getParameter(Parameter.MARGIN_ERROR));

                startGuardian(frequency, dbTolerance, peakTolerance, marginError);
                Toast toast = Toast.makeText(this, "protection started\n frequency: " + frequency +
                        "\n db tolerance: " + dbTolerance +
                        "\n peak tolerance: " + peakTolerance +
                        "\n margin error: " + marginError
                        , Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
                break;
            }
            case STOP_PROTECTION: {
                WatchMan.getInstance().stopGuardian(this, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        Jammer.getInstance().stopJamming(true, new GollumCallbackGetBoolean() {
                            @Override
                            public void done(boolean b) {
                                Log.d(TAG, "WatchMan stopped");
                                Toast toast = Toast.makeText(Main2Activity.this, "protection stopped", Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                                toast.show();
                            }
                        });

                    }
                });

                break;
            }
            case START_JAMMING: {
                Toast toastAlert = Toast.makeText(Main2Activity.this, "Attack detected", Toast.LENGTH_SHORT);
                toastAlert.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toastAlert.show();

                Logger.d(TAG, "Attack detected");
                WatchMan.getInstance().stopGuardian(Main2Activity.this, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        Logger.d(TAG, "START_JAMMING, stopGuardian : callback res :" + b);
                        final String frequency = actionEvent.getParameter(Parameter.FREQUENCY);
                        final String dbTolerance = actionEvent.getParameter(Parameter.RSSI_VALUE);
                        final int peakTolerance = Integer.valueOf(actionEvent.getParameter(Parameter.PEAK_TOLERANCE));
                        final int marginError = Integer.valueOf(actionEvent.getParameter(Parameter.MARGIN_ERROR));
                        try {
                            Thread.sleep(1000); // latency ... shit.
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Jammer.getInstance().startJamming(Integer.valueOf(frequency), new GollumCallbackGetBoolean() {
                            @Override
                            public void done(boolean b) {
                                try {
                                    Thread.sleep(1000); // latency ... shit.
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Logger.d(TAG, "START_JAMMING, startJamming : callback res :" + b);
                                startGuardian(frequency, dbTolerance, peakTolerance, marginError);
                            }
                        });
                    }
                });
            }
        }
    }

    /**
     * Used by EventBus
     * Called when a Publisher send a state.
     * @param stateEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
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

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (position == 0) {
            this.fab.show();
        } else {
            this.fab.hide();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public String getCurrentTime() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        String formattedDate = sdf.format(date);
        return formattedDate; // 03/10/2017 14:48:16
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> listOfFragments;
        private ArrayList<String> listOfTitle;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            listOfFragments = new ArrayList<>();
            listOfTitle = new ArrayList<>();
        }

        public void addFragment(Fragment fragment, String title) {
            this.listOfFragments.add(fragment);
            this.listOfTitle.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return this.listOfFragments.get(position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return this.listOfFragments.size();
        }

        @Override
        public String getPageTitle(int position) {
            if (this.listOfFragments.size() > position) {
                return this.listOfTitle.get(position);
            }
            return null;
        }
    }
}
