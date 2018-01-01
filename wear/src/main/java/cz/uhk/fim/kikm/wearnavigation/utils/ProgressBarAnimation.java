package cz.uhk.fim.kikm.wearnavigation.utils;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

public class ProgressBarAnimation extends Animation {
    private ProgressBar mProgressBar;
    private int mTo;
    private int mFrom;
    private long mStepDuration;

    public ProgressBarAnimation(ProgressBar progressBar, long stepDuration) {
        super();
        mProgressBar = progressBar;
        mStepDuration = stepDuration;
    }


    public void setProgress(int progress) {
        if (progress < 0) {
            progress = 0;
        }

        if (progress > mProgressBar.getMax()) {
            progress = mProgressBar.getMax();
        }

        mTo = progress;

        mFrom = mProgressBar.getProgress();
        setDuration(mStepDuration);
        mProgressBar.startAnimation(this);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float value = mFrom + (mTo - mFrom) * interpolatedTime;
        mProgressBar.setProgress((int) value);
    }
}
