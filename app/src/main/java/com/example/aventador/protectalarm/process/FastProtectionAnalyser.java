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
 *
 * FastProtectionAnalyser:
 * it calculates the best value in decibel automatically
 *
 * Process:
 * First of all we start at DECREASE_DB mode, as long as an attack is not detected we DECREASE "currentDbTolerance"
 * When an attack is detected. we reverse the process by incrementing weakly "currentDbTolerance", it's the INCREASE_DB mode.
 * Finally when we don't detect an attack, we cans suppose have found the good db tolerance
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
 *      But it's a limit, so to prevent a potentiel false alarm we go from -84dB to -82dB   /!\ IT'S ARBITRARY BUT but it does the job ... /!\
 *
 */
class FastProtectionAnalyser {

    private static final String TAG = "FastProtectionAnalyser";

    private GollumCallbackGetConfiguration cbConfigFound;
    private Configuration configuration;
    private int nbSequences;
    private int nbScans;

    private int nbChannels;
    private Activity activity;

    private final int DB_STEP_DECREASE = -10;
    private final int DB_STEP_INCREASE = 2;

    private final int DB_TOLERANCE_MINIMUM = -120; // -115db min
    private final int DB_TOLERANCE_MAXIMUM = -10; // 10db max
    private final int PEAK_TOLERANCE = 40; // 40%
    private final int MARGIN_ERROR = 10; // 10%

    private GuardianThread guardianThread;
    private int currentDbTolerance;
    private int decibelStep;
    private AnalyzerStatus status;
    private AtomicBoolean run;

    private GollumCallbackGetBoolean cbForceStopDone = null; // null by default. If cbForceStopDone is not null the analyzer is force stopped.

    /**
     *
     * @param activity
     * @param frequency
     * @param nbScans
     * @param nbSequences
     * @param nbChannels nb channels required, for Specan
     * @param cbConfigurationFound called when FastProtectionAnalyser has found the optimal configuration. {@link Configuration}
     */
    public FastProtectionAnalyser(Activity activity, int frequency, int nbScans, int nbSequences, int nbChannels, GollumCallbackGetConfiguration cbConfigurationFound) {
        this.activity = activity;
        this.nbScans = nbScans;
        this.nbSequences = nbSequences;
        this.nbChannels = nbChannels;
        this.cbConfigFound = cbConfigurationFound;

        run = new AtomicBoolean(false);

        this.configuration = new Configuration(frequency, DB_TOLERANCE_MINIMUM, PEAK_TOLERANCE, MARGIN_ERROR);
    }

    /**
     * start the analysis process.
     * - in DECREASE_DB mode
     */
    public void start() {
        if (!run.compareAndSet(false, true)) {
            return; // one call of run() expected !!!!
        }
        currentDbTolerance = DB_TOLERANCE_MAXIMUM;
        status = DECREASE_DB;
        decibelStep = DB_STEP_DECREASE;
        cbForceStopDone = null;
        startGuardian();
    }

    /**
     * startGuardian create a GuardianThread with a fixed currentDbTolerance
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

    /**
     * Indicate if the analyzer shall be stopped
     * @return
     */
    private boolean forceStopProcess() {
        return cbForceStopDone != null;
    }

    /**
     * @param cbForceStopDone Called when the FastProtectionAnalyser process is stopped.
     */
    public void stop(GollumCallbackGetBoolean cbForceStopDone) {
        Logger.d(TAG, "stop()");
        if (!run.get()) {
            return;
        }

        this.cbForceStopDone = cbForceStopDone;
    }

    /**
     * Called when a guardian thread as not found an brute force attack (in this case a false positive)
     */
    private void guardianIsDone() {
        Logger.d(TAG, "guardian done");
        if (forceStopProcess()) {
            /*
            Check if the analyzer shall be force stopped by user
             */
            Logger.d(TAG, "guardianIsDone : forceStopProcess");
            run.set(false); // the analyzer doesn't work.
            cbForceStopDone.done(true); // force stop is done.
            return;
        }
        if (status == INCREASE_DB) {
            /*
            If we are in INCREASE_DB, we must break the process because we know the good db tolerance.
            recall:
                * First of all we start at DECREASE_DB mode, as long as an attack is not detected we DECREASE "currentDbTolerance"
                * When an attack is detected. we reverse the process by incrementing weakly "currentDbTolerance", it's the INCREASE_DB mode.
                * Finally when we don't detect an attack, we cans suppose have found the good db tolerance
                * But it's a limit, so to prevent a potentiel false alarm we add 2 to the currentDbTolerance   /!\ IT'S ARBITRARY BUT but it does the job ... /!\
             */
            Logger.d(TAG, "guardianIsDone: status == INCREASE_DB: End of process");
            Logger.d(TAG, "guardianIsDone: db tolerance: " + getCurrentDbTolerance());
            configuration.setDbTolerance(getCurrentDbTolerance() + DB_STEP_INCREASE);
            run.set(false);
            cbConfigFound.done(true, configuration);
            return;
        } else {
            /*
            Otherwise, we add decibelStep to currentDbTolerance
            regardless of the mode DECREASE/INCREASE_DB

            and we restart the guardian
             */
            currentDbTolerance += decibelStep;
            startGuardian();
        }


    }

    /**
     * Called when an brute force attack is detected (in this case it's a false positive)
     */
    private void attackDetected() {
        Logger.d(TAG, "attack detected");
        guardianThread.kill(); // we kill the guardian.
        if (forceStopProcess()) {
            /*
            Check if the analyzer shall be force stopped by user

            if true:    stop the analyzer.
                        call cbForceStopDone
                        and cancel all pending jobs associated to this process
                        {@link com.example.aventador.protectalarm.tools.Recaller.FAST_PROTECTION_ANALYZER_TAG}
             */
            Logger.d(TAG, "attackDetected : forceStopProcess");
            run.set(false);
            cbForceStopDone.done(true);
            Recaller.getInstance().cancel(FAST_PROTECTION_ANALYZER_TAG);
            return;
        }
        if (status == DECREASE_DB) {
            /*
            If an attack is detected in DECREASE_DB mode, we toogle to INCREASE_DB mode
            in order to refine the db tolerance value.
            as long as we detect a brute force attack, we INCREASE the "currentDbTolerance'
             */
            Logger.d(TAG, "attackDetected: reverse process, decibelStep fixed to 2");
            decibelStep = DB_STEP_INCREASE;
            status = INCREASE_DB;
        }

        checkThreadLater(); // this routine check if guardianThread is dead.

    }



    /**
     * Use Recaller to give the guardian thread time to stop
     * When the callback is called, check if the thread is dead.
     * If it's not dead, call again Recaller.recallMe().
     */
    private void checkThreadLater() {
        if (forceStopProcess()) {
            cbForceStopDone.done(true);
            return;
        }
        Recaller.getInstance().recallMe(FAST_PROTECTION_ANALYZER_TAG, 1000L, new GollumCallbackGetBoolean() {
            @Override
            public void done(boolean b) {
                /*
                Called each 2seconds, and while the guardian thread is not dead.
                 */
                if (guardianThread.isAlive()) {
                    Logger.d(TAG, "guardianThread is always alive");
                    checkThreadLater(); // retry and check for the next time
                } else {
                    Logger.d(TAG, "guardianThread is dead");
                    /*
                    Otherwise, we add decibelStep to currentDbTolerance
                    regardless of the mode DECREASE/INCREASE_DB

                    and we restart the guardian
                    */
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
        /*
        States of the FastProtectionAnalyzer process.
         */
        INCREASE_DB, // --> currentDbTolerance will raise up
        DECREASE_DB; // --> currentDbTolerance will decrease
    }
}
