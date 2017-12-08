package cz.uhk.fim.kikm.wearnavigation.activities.scan;

import android.os.Bundle;

import com.qozix.tileview.TileView;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;

public class ScanActivity extends BaseActivity {

    private TileView mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMap = findViewById(R.id.as_view_map);

        // let the image explode
        mMap.setScaleLimits( 0, 2 );

        // size of original image at 100% mScale
        mMap.setSize( 3000, 3000 );

        // detail levels
        mMap.addDetailLevel( 1.000f, "tiles/j3np/1000/j3np-%d_%d.png");
        mMap.addDetailLevel( 0.500f, "tiles/j3np/500/j3np-%d_%d.png");
        mMap.addDetailLevel( 0.250f, "tiles/j3np/250/j3np-%d_%d.png");
        mMap.addDetailLevel( 0.125f, "tiles/j3np/125/j3np-%d_%d.png");

        // set mScale to 0, but keep scaleToFit true, so it'll be as small as possible but still match the container
        mMap.setScale(0.50F);

        // let's use 0-1 positioning...
        mMap.defineBounds( 0, 0, 1, 1 );

        // frame to center
        frameTo( 0.5, 0.5 );

        // render while panning
        mMap.setShouldRenderWhilePanning( true );

        // disallow going back to minimum scale while double-taping at maximum scale (for demo purpose)
        mMap.setShouldLoopScale( false );

    }

    /**
     * This is a convenience method to scrollToAndCenter after layout (which won't happen if called directly in onCreate
     * see https://github.com/moagrius/TileView/wiki/FAQ
     */
    public void frameTo( final double x, final double y ) {
        mMap.post( new Runnable() {
            @Override
            public void run() {
                mMap.scrollToAndCenter( x, y );
            }
        });
    }

    @Override
    protected void updateUI() {

    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_scan;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.action_show_scan;
    }
}
