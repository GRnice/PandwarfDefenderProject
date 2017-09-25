package com.example.aventador.protectalarm;

import android.*;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.WindowManager;
import android.widget.TextView;
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
import com.example.aventador.protectalarm.process.WatchMan;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;

public class Main2Activity extends AppCompatActivity {

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

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

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
        getMenuInflater().inflate(R.menu.menu_main2, menu);
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
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.BLUETOOTH},
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
                        Log.d(TAG, "Threshold found");
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
            case START_PROTECTION: {
                String frequency = actionEvent.getParameters().getString(Parameter.FREQUENCY.toString());
                String dbTolerance =  actionEvent.getParameters().getString(Parameter.RSSI_VALUE.toString());
                WatchMan.getInstance().start(this, Integer.valueOf(frequency), Integer.valueOf(dbTolerance), new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        Log.d(TAG, "Attack detected");
                    }
                });
                Toast toast = Toast.makeText(this, "protection started\n frequency: " + frequency + ", db tolerance: " + dbTolerance, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
                break;
            }
            case STOP_PROTECTION: {
                WatchMan.getInstance().stop(this);
                Log.d(TAG, "WatchMan stopped");
                Toast toast = Toast.makeText(this, "protection stopped", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
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
