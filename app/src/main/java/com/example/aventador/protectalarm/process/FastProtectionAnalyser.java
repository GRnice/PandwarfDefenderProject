package com.example.aventador.protectalarm.process;

import android.app.Activity;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetBoolean;
import com.example.aventador.protectalarm.callbacks.GollumCallbackGetConfiguration;
import com.example.aventador.protectalarm.process.Runners.GuardianThread;
import com.example.aventador.protectalarm.storage.Configuration;
import com.example.aventador.protectalarm.tools.Logger;
import com.example.aventador.protectalarm.tools.Recaller;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.aventador.protectalarm.process.FastProtectionAnalyser.AnalyzerStatus.DECREASE_DB;
import static com.example.aventador.protectalarm.process.FastProtectionAnalyser.AnalyzerStatus.INCREASE_DB;
import static com.example.aventador.protectalarm.tools.Recaller.FAST_PROTECTION_ANALYZER_TAG;

/**
 * Created by Giangrasso on 26/10/2017.
 */

/**
 * Visibility fixed at Package-private!
 */
class FastProtectionAnalyser {

    private static final String TAG = "FastProtectionAnalyser";

    private GollumCallbackGetConfiguration cbConfigFound;
    private Configuration configuration;
    private int nbSequences;
    private int nbScans;

    private int nbChannels;
    private Activity activity;
    private final int DB_TOLERANCE_MINIMUM = -115; // -115db min
    private final int DB_TOLERANCE_MAXIMUM = -10; // 10db max
    private final int PEAK_TOLERANCE = 40; // 40%
    private final int MARGIN_ERROR = 10; // 10%

    private GuardianThread guardianThread;
    private int currentDbTolerance;
    private int decibelStep;
    private AnalyzerStatus status;
    private AtomicBoolean run;

    private GollumCallbackGetBoolean cbForceStopDone = null; // null by default. If cbForceStopDone is not null the analyzer is force stopped.

    public FastProtectionAnalyser(Activity activity, int frequency, int nbScans, int nbSequences, int nbChannels, GollumCallbackGetConfiguration cbConfigurationFound) {
        this.activity = activity;
        this.nbScans = nbScans;
        this.nbSequences = nbSequences;
        this.nbChannels = nbChannels;
        this.cbConfigFound = cbConfigurationFound;

        run = new AtomicBoolean(false);

        this.configuration = new Configuration(frequency, DB_TOLERANCE_MINIMUM, PEAK_TOLERANCE, MARGIN_ERROR);
    }

    public void start() {
        if (!run.compareAndSet(false, true)) {
            return; // one call of run() expected !!!!
        }
        currentDbTolerance = DB_TOLERANCE_MAXIMUM;
        status = DECREASE_DB;
        decibelStep = -10;
        cbForceStopDone = null;
        startGuardian();
    }

    /**
     * Process:
     *      * First of all, we start at DECREASE_DB, as long as an attack is not detected we INCREASE "currentDbTolerance"
     *      * When an attack is detected we toogle to INCREASE_DB, that is, we reverse the process by decrementing weakly "currentDbTolerance"
     *      * Finally when we don't detect an attack, we cans suppose have found the good db tolerance
     *
     *      Example:
     *      We start at -10dB
     *      -10dB -> no Attack detected.
     *      ...
     *      -90dB -> We detect an attack at -90dB !
     *      we reverse at this time we reverse the process
     *      -88dB -> We detect an attack
     *      -86dB -> Again
     *      -84dB -> No attack detected. -84dB is a good value!!
     *      But it's a limit, so to prevent a potentiel false alarm we go from -84dB to -82dB   /!\ IT'S ARBITRARY /!\
     *
     */
    private void startGuardian() {
        Logger.d(TAG, "startGuardian()");
        Logger.d(TAG, "startGuardian(): currentDbTolerance: " + getCurrentDbTolerance());
        guardianThread = new GuardianThread(activity, getNbSequences(), getNbScans(), getNbChannels(), getCurrentDbTolerance(), PEAK_TOLERANCE, MARGIN_ERROR, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                attackDetected();
            }
        }, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                guardianIsDone();
            }
        });

        guardianThread.start(false);
    }

    private boolean forceStopProcess() {
        return cbForceStopDone != null;
    }

    public void stop(GollumCallbackGetBoolean cbForceStopDone) {
        Logger.d(TAG, "stop()");
        if (!run.get()) {
            return;
        }

        this.cbForceStopDone = cbForceStopDone;
    }

    private void guardianIsDone() {
        Logger.d(TAG, "guardian done");
        if (forceStopProcess()) {
            Logger.d(TAG, "guardianIsDone : forceStopProcess");
            run.set(false);
            cbForceStopDone.done(true);
            return;
        }
        if (status == INCREASE_DB) {
            /*
            If we are in INCREASE_DB, we must break the process because we know the good db tolerance.
            recall:
                * First of all we start at DECREASE_DB, as long as an attack is not detected we INCREASE "currentDbTolerance"
                * When an attack is detected. we reverse the process by incrementing weakly "currentDbTolerance"
                * Finally when we don't detect an attack, we cans suppose have found the good db tolerance
             */
            Logger.d(TAG, "guardianIsDone: status == INCREASE_DB: End of process");
            Logger.d(TAG, "guardianIsDone: db tolerance: " + getCurrentDbTolerance());
            configuration.setDbTolerance(getCurrentDbTolerance() + 2);
            cbConfigFound.done(true, configuration);
            return;
        }

        currentDbTolerance += decibelStep;
        startGuardian();
    }


    private void attackDetected() {
        Logger.d(TAG, "attack detected");
        guardianThread.kill();
        if (forceStopProcess()) {
            Logger.d(TAG, "attackDetected : forceStopProcess");
            run.set(false);
            cbForceStopDone.done(true);
            Recaller.getInstance().cancel(FAST_PROTECTION_ANALYZER_TAG);
            return;
        }
        if (status == DECREASE_DB) {
            Logger.d(TAG, "attackDetected: reverse process, decibelStep fixed to 2");
            decibelStep = 2;
            status = INCREASE_DB;
        }

        checkThreadLater();

    }



    /**
     * Use Recaller to give the thread time to stop
     * When the callback is called, check if the thread is dead.
     * If it's not dead, call again Recaller.recallMe().
     */
    private void checkThreadLater() {
        if (forceStopProcess()) {
            cbForceStopDone.done(true);
            return;
        }
        Recaller.getInstance().recallMe(FAST_PROTECTION_ANALYZER_TAG, 2000L, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                if (guardianThread.isAlive()) {
                    Logger.d(TAG, "guardianThread is always alive");
                    checkThreadLater();
                } else {
                    Logger.d(TAG, "guardianThread is dead");
                    currentDbTolerance += decibelStep;
                    startGuardian();
                }
            }
        });
    }

    public int getCurrentDbTolerance() {
        return currentDbTolerance;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public int getNbSequences() {
        return nbSequences;
    }

    public int getNbScans() {
        return nbScans;
    }

    public int getNbChannels() {
        return nbChannels;
    }

    public int getDecibelStep() {
        return decibelStep;
    }

    enum AnalyzerStatus {
        INCREASE_DB,
        DECREASE_DB;
    }
}
