package com.example.aventador.protectalarm;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetInteger;
import com.example.aventador.protectalarm.bluetooth.BluetoothReceiver;
import com.example.aventador.protectalarm.events.ActionEvent;
import com.example.aventador.protectalarm.events.StateEvent;
import com.example.aventador.protectalarm.fragments.settings.SettingsFragment;
import com.example.aventador.protectalarm.process.Pandwarf;
import com.example.aventador.protectalarm.process.task.TaskPollManager;
import com.example.aventador.protectalarm.tools.Logger;
import com.example.aventador.protectalarm.tools.Recaller;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import static com.example.aventador.protectalarm.events.State.DISCONNECTED;

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
    private View settingsFragmentLayout;
    private FloatingActionButton fab;
    private View appBarLayout;
    private int bluetoothState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        appBarLayout = findViewById(R.id.appbar);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.addFragment(new HomeFragment(), "Pairing");
        mSectionsPagerAdapter.addFragment(new ThresholdFragment(), "Signal measurement");
        mSectionsPagerAdapter.addFragment(new GuardianFragment(), "Protection");

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(mViewPager.getAdapter().getCount());
        mViewPager.addOnPageChangeListener(this);

        settingsFragmentLayout = findViewById(R.id.settings_fragment_layout);

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
        EventBus.getDefault().register(this);
        Recaller.getInstance(this); // init Recaller.
        TaskPollManager.getInstance(this); // TaskPollManager.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    },
                    REQUEST_CODE_ASK_PERMISSIONS);
        }

    }
    @Override
    @CallSuper
    public void onDestroy() {
        super.onDestroy();
        BluetoothReceiver.getInstance().unregister(this);
        EventBus.getDefault().unregister(this);
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
                if (!Pandwarf.getInstance().isConnected()) {
                    return;
                }
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
     * Kill all process, scan, threshold discovery, protection, jamming, fast protection analyzer
     */
    public void killAllProcess(final GollumCallbackGetBoolean killDone) {
        Pandwarf.getInstance().stopFastProtectionAnalyzer(this, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                Pandwarf.getInstance().stopDiscovery(Main2Activity.this, new GollumCallbackGetBoolean() {
                    @Override
                    public void done(boolean b) {
                        Pandwarf.getInstance().stopGuardian(Main2Activity.this, new GollumCallbackGetBoolean() {
                            @Override
                            public void done(boolean b) {
                                Pandwarf.getInstance().stopJamming(Main2Activity.this, true, new GollumCallbackGetBoolean() {
                                    @Override
                                    public void done(boolean b) {
                                        killDone.done(true);
                                        Pandwarf.getInstance().close(Main2Activity.this);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main2, menu);
        return true;
    }

    public void closeSettings() {
        appBarLayout.setVisibility(View.VISIBLE);
        mViewPager.setVisibility(View.VISIBLE);
        settingsFragmentLayout.setVisibility(View.GONE);
    }

    /**
     * Used by EventBus
     * Called when a Publisher send a action to be executed.
     * @param actionEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final ActionEvent actionEvent) {
        switch (actionEvent.getActionRequested()) {
            case START_FAST_PROTECTION_ANALYZER:
            case STOP_FAST_PROTECTION_ANALYZER:
            case START_SEARCH_THRESHOLD:
            case STOP_SEARCH_THRESHOLD:
            case START_PROTECTION:
            case STOP_PROTECTION: {
                TaskPollManager.getInstance().put(actionEvent);
                break;
            }
            case START_JAMMING: {
                toastShow("Attack detected");

                Logger.d(TAG, "Attack detected");
                TaskPollManager.getInstance().put(actionEvent);
                break;
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
                Pandwarf.getInstance().setConnected(true);
                toastShow("Device Opened...");
                break;
            }
            case DISCONNECTED: {
                toastShow("Closing device...");
                Pandwarf.getInstance().setConnected(false);
                break;
            }
        }
    }



    private void toastShow(String message) {
        Toast toast = Toast.makeText(Main2Activity.this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            appBarLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            settingsFragmentLayout.setVisibility(View.VISIBLE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        this.fab.hide();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

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
