package cz.uhk.fim.kikm.wearnavigation.utils;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

/**
 * Updates progress bar with animation.
 */
public class ProgressBarAnimation extends Animation {
    private ProgressBar mProgressBar;   // To update progress in
    private int mTo;                    // Update to specific value
    private int mFrom;                  // From specific value
    private long mStepDuration;         // Length for animation

    public ProgressBarAnimation(ProgressBar progressBar, long stepDuration) {
        super();
        // Set instance variables
        mProgressBar = progressBar;
        mStepDuration = stepDuration;
    }

    /**
     * Update progress of progress bar with animation.
     *
     * @param progress current progress (to)
     */
    public void setProgress(int progress) {
        // Don't go under 0 progress
        if (progress < 0) {
            progress = 0;
        }

        // Don't go over max progress
        if (progress > mProgressBar.getMax()) {
            progress = mProgressBar.getMax();
        }

        mTo = progress;                         // Progress to go to
        mFrom = mProgressBar.getProgress();     // Get current progress
        setDuration(mStepDuration);             // Sets animation duration
        mProgressBar.startAnimation(this);      // Start update animation
    }

    /**
     * Handles animation transformations. Sets progress based on calculated value.
     *
     * @param interpolatedTime The value of the normalized time (0.0 to 1.0)
     *        after it has been run through the interpolation function.
     * @param t The Transformation object to fill in with the current
     *        transforms.
     */
    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        // Update progress value
        float value = mFrom + (mTo - mFrom) * interpolatedTime; // Calculate progress by interpolatedTime
        mProgressBar.setProgress((int) value);                  // Set progress value
    }
}
