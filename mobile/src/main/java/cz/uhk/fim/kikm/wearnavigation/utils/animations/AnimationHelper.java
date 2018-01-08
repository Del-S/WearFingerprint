package cz.uhk.fim.kikm.wearnavigation.utils.animations;

import android.app.Activity;

import cz.uhk.fim.kikm.wearnavigation.model.tasks.ScanProgress;

/**
 * Class that will handle animations in the app.
 */
public class AnimationHelper {

    private ScanProgressBarAnimator mScanProgressBarAnimator;   // Scan animator that displays progress

    public AnimationHelper() {
        mScanProgressBarAnimator = new ScanProgressBarAnimator();
    }

    /**
     * Displays ScanProgressBar in specific activity
     *
     * @param activity to show progress in
     * @param scanProgress current progress of the scan
     * @param toVisibility show or hide the scan progress
     * @param duration length of show/hide animation
     */
    public void displayScanStatus(Activity activity, ScanProgress scanProgress, final int toVisibility, int duration) {
        // Trigger animation in specific sub-class
        mScanProgressBarAnimator.displayScanStatus(activity, scanProgress, toVisibility, duration);
    }
}
