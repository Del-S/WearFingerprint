package cz.uhk.fim.kikm.wearnavigation.utils.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.FingerprintScanner;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.ScanProgress;
import cz.uhk.fim.kikm.wearnavigation.utils.ProgressBarAnimation;

/**
 * Animator that displays ScanProgress in specific activity
 */
class ScanProgressBarAnimator {

    private static final String TAG = "ScanProgressBar";

    // Display widgets
    private ProgressBar mProgressBar;  // Progress bar of the scan
    private TextView mStatus;          // Scan status text
    private TextView mBleCount;        // Count of found Bluetooth devices
    private TextView mWifiCount;       // Count of found Wireless devices
    private TextView mCellCount;       // Count of found Cellular devices

    private ProgressBarAnimation mProgressAnimation;    // ProgressBar animation class

    /**
     * Displays scan status bar with progress of the scan. Animation is handled via alpha change of the view.
     * - If view does not exist in specific activity it is created
     * - If the view is shown updates status
     *
     * @param activity to display scan progress in
     * @param scanProgress holding scan progress data
     * @param toVisibility show or hide progress
     * @param duration show/hide animation length
     */
    void displayScanStatus(Activity activity, ScanProgress scanProgress, final int toVisibility, int duration) {
        final View view = loadScanStatusView(activity);     // Load or create scan view

        // Show or hide animation only if the visibility should change
        if (toVisibility != view.getVisibility()) {
            view.setVisibility(View.VISIBLE);   // Display view in the activity so it calculates height and width

            // Check if we should display or hide the view
            boolean show = toVisibility == View.VISIBLE;
            if (show) {
                view.setAlpha(0);
            }
            view.bringToFront();        // So the view is not under different view we bring it to front
            view.setElevation(5);       // Set elevation to create shadows fro this view
            view.animate()              // Display this view with animation
                    .setDuration(duration)
                    .alpha(show ? 100 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // Reset widgets when this view was just hidden
                            if(toVisibility == View.GONE) {
                                resetScanWidgets();
                            }
                            view.setVisibility(toVisibility);   // At the end of the animation we set proper visibility
                        }
                    });
        } else {
            // If visibility was not changed then do nothing
            view.setVisibility(toVisibility);
        }

        // Sets progress texts to the view
        setScanProgressTexts(activity, view, scanProgress);
    }

    /**
     * Tries to find view in specified activity and return it.
     * - If the view does not exist it is created and constraint to the bottom of the screen.
     *
     * @param activity to find progress bard overlay
     * @return progress bar view
     */
    private View loadScanStatusView(Activity activity) {
        // Load scan progress overlay from activity
        View scanStatusView = activity.findViewById(R.id.scan_progress_overlay);

        // If the overlay does not exist then then inflate it with layout
        if (scanStatusView == null) {
            // Loads parent view added in setContentView() method
            ViewGroup viewGroup = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            scanStatusView = LayoutInflater.from(activity).inflate(R.layout.scan_progress_overlay, null);  // Inflate progress view
            scanStatusView.setVisibility(View.GONE);       // Hide created view

            try {
                // Initiate constraint variables
                ConstraintLayout layout = (ConstraintLayout) viewGroup;         // Cast ViewGroup into ConstraintLayout
                ConstraintSet set = new ConstraintSet();                        // Create new instance of ConstraintSet
                BottomNavigationView bottomMenu = layout.findViewById(R.id.bottom_menu);    // Find bottom menu in the layout

                // Add the scanStatusView into the layout
                viewGroup.addView(scanStatusView);

                // Set constraints to the scanStatusView
                set.clone(layout);          // Clone constraint from ConstraintLayout
                if (bottomMenu != null) {   // Only if bottom menu was found
                    // Connect bottom of scanStatusView with top of Bottom menu
                    set.connect(scanStatusView.getId(), ConstraintSet.BOTTOM, bottomMenu.getId(), ConstraintSet.TOP, 20);
                } else {
                    // Connect bottom of scanStatusView with parent bottom
                    set.connect(scanStatusView.getId(), ConstraintSet.BOTTOM, layout.getId(), ConstraintSet.BOTTOM, 20);
                }

                // Center scanStatusView in layout
                set.connect(scanStatusView.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START);
                set.connect(scanStatusView.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END);
                set.applyTo(layout);    // Apply new constraint into the layout
            } catch (Exception e) {
                Log.e(TAG, "Could not set constraints: ", e);
                // Add the scanStatusView into the viewGroup to at least show something
                viewGroup.addView(scanStatusView);
            }

            loadScanWidgets(scanStatusView);    // Load display widgets
        }

        return scanStatusView;  // Return progress bar view
    }

    /**
     * Finds widget views in the scan progress bar view.
     * Used to display scan progress data. Also needed to animate progress bar.
     *
     * @param view progress bar view
     */
    private void loadScanWidgets(View view) {
        // Load widgets from the view
        mProgressBar = view.findViewById(R.id.spo_progress);        // Load progress bar widget
        mStatus = view.findViewById(R.id.spo_status);               // Load text status information widget
        mBleCount = view.findViewById(R.id.spo_bluetooth_count);    // Load bluetooth entries count widget
        mWifiCount = view.findViewById(R.id.spo_wireless_count);    // Load wireless entries count widget
        mCellCount = view.findViewById(R.id.spo_cellular_count);    // Load cellular entries count widget

        // Initiate progress bar animation
        mProgressAnimation = new ProgressBarAnimation(mProgressBar, 1000);
    }

    /**
     * Resets scan widgets for next scan.
     */
    private void resetScanWidgets() {
        if(areScanWidgetsLoaded()) {
            // Reset values to default
            mStatus.setText(R.string.spo_status_creating);
            mProgressBar.setMax(100);
            mProgressBar.setProgress(0);
            mBleCount.setText(String.valueOf(0));
            mWifiCount.setText(String.valueOf(0));
            mCellCount.setText(String.valueOf(0));
        }
    }

    /**
     * Checks if the scan widgets are loaded.
     * Used to prevent crashes in setScanProgressTexts().
     *
     * @return true/false widgets loaded
     */
    private boolean areScanWidgetsLoaded() {
        return ( mProgressBar != null &&
                mStatus != null &&
                mBleCount != null &&
                mWifiCount != null &&
                mCellCount != null &&
                mProgressAnimation != null );
    }

    /**
     * Sets scan variables to the view and animates change in ProgressBar.
     *
     * @param activity to display scan progress in
     * @param view progress bar view
     * @param scanProgress current scan progress
     */
    private void setScanProgressTexts(Activity activity, View view, ScanProgress scanProgress) {
        // Try to load widgets if they are not loaded just yet
        if(view != null && !areScanWidgetsLoaded()) {
            loadScanWidgets(view);
        }

        // Change status texts only if the view is displayed
        if (view != null
                && view.getVisibility() != View.GONE
                && scanProgress != null
                && areScanWidgetsLoaded()) {

            // Set the data into the widgets
            mProgressBar.setMax(scanProgress.getScanLength());              // Sets the max value into progress bar
            mProgressAnimation.setProgress(scanProgress.getCurrentTime());  // Set current value into progress bar
            mStatus.setText(scanProgress.getStateString());                 // Sets status text to inform user
            mBleCount.setText(String.valueOf(scanProgress.getBeaconCount()));      // Sets count of the bluetooth le device entries
            mWifiCount.setText(String.valueOf(scanProgress.getWirelessCount()));   // Sets count of wireless devices entries
            mCellCount.setText(String.valueOf(scanProgress.getCellularCount()));   // Sets count of cellular tower entries

            // Hide this view after completion (5 seconds)
            if (scanProgress.getState() == FingerprintScanner.TASK_STATE_DONE) {
                Handler hideHandler = new Handler();
                hideHandler.postDelayed(() -> {
                    if (activity != null) {
                        // Hide view after 3 seconds
                        displayScanStatus(activity, null, View.GONE, 1000);
                    }
                }, 3000);
            }
        }
    }

}