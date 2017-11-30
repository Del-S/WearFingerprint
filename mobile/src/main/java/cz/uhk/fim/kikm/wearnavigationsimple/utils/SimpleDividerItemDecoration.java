package cz.uhk.fim.kikm.wearnavigationsimple.utils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Used to display dividing line in RecyclerView for devices
 */
public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
    // Divider drawable
    private Drawable mDivider;

    public SimpleDividerItemDecoration(Drawable divider) {
        mDivider = divider;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        // Get dimensions of RecyclerView
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        // Count how many rows is in the RacyclerView
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            // Get View of child on specific position in RecyclerView
            View child = parent.getChildAt(i);

            // Gets layout information of child
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            // Calculate position of the line for teh display
            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();

            // Draw line divider
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        // Add size of divider to the bottom of the child to make it visible
        outRect.set(0, 0, 0, mDivider.getIntrinsicWidth());
    }
}