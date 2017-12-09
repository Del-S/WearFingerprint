package cz.uhk.fim.kikm.wearnavigation.activities.scan;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;
import com.qozix.tileview.hotspots.HotSpot;
import com.qozix.tileview.markers.MarkerLayout;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseDataInterface;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseDataLoader;

public class ScanActivity extends BaseActivity implements DatabaseDataInterface<List<Fingerprint>> {

    // Map view
    private TileView mMap;
    // List of the fingerprints loaded and kept for display here
    private List<Fingerprint> mFingerprints;

    // Sizes of the map
    private final int MAP_WIDTH = 3000;
    private final int MAP_HEIGHT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadMap();
        loadFingerprints();
    }

    /**
     * Loads map and tiles of the map. Sets all the required variables.
     */
    private void loadMap() {
        // Find the map view
        mMap = findViewById(R.id.as_view_map);

        // Let the image explode (set scale limits)
        mMap.setScaleLimits( 0, 2 );

        // Size of original image at 100% scale
        mMap.setSize( MAP_WIDTH, MAP_HEIGHT );

        // Map detail levels for zooms
        mMap.addDetailLevel( 1.000f, "tiles/j3np/1000/j3np-%d_%d.png");
        mMap.addDetailLevel( 0.500f, "tiles/j3np/500/j3np-%d_%d.png");
        mMap.addDetailLevel( 0.250f, "tiles/j3np/250/j3np-%d_%d.png");
        mMap.addDetailLevel( 0.125f, "tiles/j3np/125/j3np-%d_%d.png");

        // Set initial scale to have the map zoomed in at first
        mMap.setScale(0.50F);

        // Lets center all markers both horizontally and vertically
        mMap.setMarkerAnchorPoints( -0.5f, -0.5f );

        // Sets listener on tap events for the markers
        mMap.setMarkerTapListener(mMarkerTapListener);

        // Define bound to be able to convert Absolute X to Relative X for clicks
        mMap.defineBounds(0,0,MAP_WIDTH,MAP_HEIGHT);

        // Make the whole map clickable to be able to add new pins (fingerprints)
        HotSpot hotSpot = new HotSpot();
        hotSpot.set(0,0,MAP_WIDTH,MAP_HEIGHT);
        mMap.addHotSpot(hotSpot);
        mMap.setHotSpotTapListener(mHotspotTapListener);

        // Frame the map to the center of the view (does not work in onCreate())
        frameTo( 0.5, 0.5 );

        // Render while panning
        mMap.setShouldRenderWhilePanning( true );

        // Disallow going back to minimum scale while double-taping at maximum scale (for demo purpose)
        mMap.setShouldLoopScale( false );
    }

    /**
     * Initiates loading of the fingerprints
     */
    private void loadFingerprints() {
        DatabaseDataLoader<List<Fingerprint>> loader = new DatabaseDataLoader<>(this);
        loader.execute(DatabaseDataLoader.MODE_FINGERPRINT);
    }

    /**
     * Displays all the fingerprints on the map
     */
    private void displayFingerprints() {
        for (Fingerprint fingerprint : mFingerprints) {
            int x = fingerprint.getX();
            int y = fingerprint.getY();
            double[] point = { x , y };

            ImageView iw = new ImageView(this);
            // TODO: tag can be object so put fingerprint id in there and handle that info as you like.
            iw.setTag(point);
            iw.setId(fingerprint.getDbId());

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(30, 30);
            iw.setLayoutParams(layoutParams);
            iw.setImageResource(R.drawable.green);

            mMap.addMarker(iw, fingerprint.getX(), fingerprint.getY(), null, null);
        }
    }

    /**
     * OnClickListener for Markers.
     * It enables to see more information about the specific fingerprint.
     */
    private MarkerLayout.MarkerTapListener mMarkerTapListener = new MarkerLayout.MarkerTapListener() {
        @Override
        public void onMarkerTap(View v, int x, int y ) {
            double[] position = (double[]) v.getTag();
            Toast.makeText( ScanActivity.this, "You tapped a pin (id = " + v.getId() + ") at " + position[0] + ":" + position[1], Toast.LENGTH_SHORT ).show();
        }
    };

    /**
     * OnClickListener for the HotSpots (whole map)
     * It will enable to add new fingerprints at specific positions.
     */
    private HotSpot.HotSpotTapListener mHotspotTapListener = new HotSpot.HotSpotTapListener() {
        @Override
        public void onHotSpotTap(HotSpot hotSpot, int x, int y) {
            CoordinateTranslater ct = mMap.getCoordinateTranslater();
            int realX = (int) ct.translateAndScaleAbsoluteToRelativeX(x, mMap.getScale());
            int realY = (int) ct.translateAndScaleAbsoluteToRelativeY(y, mMap.getScale());
            Toast.makeText( ScanActivity.this, "You tapped at position: " + realX + ":" + realY, Toast.LENGTH_SHORT ).show();
        }
    };

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

    @Override
    public void allFingerprintsLoaded(List<Fingerprint> result) {
        if(result != null) {
            mFingerprints = result;
            displayFingerprints();
        }
    }

    @Override
    public void loadError() {
        Toast.makeText(this, "There was an error while loading data.", Toast.LENGTH_SHORT).show();
    }
}
