package cz.uhk.fim.kikm.wearnavigation.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cz.uhk.fim.kikm.wearnavigation.R;

public class AnimationHelper {

    private final static String TAG = "AnimationHelper";

    public static void displayScanStatus(Activity activity, final int toVisibility, int duration) {
        View scanStatusView = activity.findViewById(R.id.scan_progress_overlay);       // Load scan progress overlay from activity

        // If the overlay does not exist then then inflate it with layout
        if(scanStatusView == null) {
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
                // Only if bottom menu was found
                if(bottomMenu != null) {
                    // Connect bottom of scanStatusView with top of Bottom menu
                    set.connect(scanStatusView.getId(), ConstraintSet.BOTTOM, bottomMenu.getId(), ConstraintSet.TOP, 20);
                } else {
                    // Connect bottom of scanStatusView with parent bottom
                    set.connect(scanStatusView.getId(), ConstraintSet.BOTTOM, layout.getId(), ConstraintSet.BOTTOM, 20);
                }
                // Center scanStatusView in layout
                set.connect(scanStatusView.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START);
                set.connect(scanStatusView.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END);
                // Apply new constraint into the layout
                set.applyTo(layout);
            } catch (Exception e) {
                Log.e(TAG, "Could not set constraints: ", e);
                // Add the scanStatusView into the viewGroup to at least show something
                viewGroup.addView(scanStatusView);
            }
        }

        final View view = scanStatusView;      // Only for the final part so it can be used in setListener
        // Do the animation only if the visibility should change
        if(toVisibility != view.getVisibility()) {
            view.setVisibility(View.VISIBLE);

            // Show or hide the overlay
            boolean show = toVisibility == View.VISIBLE;
            if (show) {
                view.setAlpha(0);
            }
            view.bringToFront();        // So the view is not under different view we bring it to front
            view.animate()              // Display this view with animation
                    .setDuration(duration)
                    .alpha(show ? 100 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            view.setVisibility(toVisibility);
                        }
                    });
        } else {
            // If not than do nothing
            view.setVisibility(toVisibility);
        }
    }

}
