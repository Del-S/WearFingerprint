package cz.uhk.fim.kikm.wearnavigation.activities.scan;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;
import com.qozix.tileview.hotspots.HotSpot;
import com.qozix.tileview.markers.MarkerLayout;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.LocationEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseDataInterface;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseDataLoader;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.FingerprintScanner;

/**
 * Displays map with fingerprints and enables different actions with them.
 * - See information about selected fingerprint
 * - Create a new fingerprint
 * - Delete fingerprint
 */
public class ScanActivity extends BaseActivity implements DatabaseDataInterface<List<Fingerprint>> {

    // Map view
    private TileView mMap;
    // List of the fingerprints loaded and kept for display here
    private List<Fingerprint> mFingerprints;
    // Disables HotSpot click if marker was clicked
    private boolean mMarkerClicked = false;

    public static final int JOB_ID = 1;

    // Sizes of the map
    private final int MAP_WIDTH = 3000;
    private final int MAP_HEIGHT = 3000;

    private JobScheduler jobScheduler;
    private JobInfo.Builder jobBuilder;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        jobScheduler = (JobScheduler) getSystemService( Context.JOB_SCHEDULER_SERVICE );
        jobBuilder = new JobInfo.Builder( JOB_ID, new ComponentName( getPackageName(),
                        FingerprintScanner.class.getName() ) );
        jobBuilder.setMinimumLatency(0);
        jobBuilder.setOverrideDeadline(200);
        jobBuilder.setPersisted(false);

        loadMap();              // Find and load map
        loadFingerprints();     // Loads fingerprint data
    }

    /**
     * Loads map and tiles of the map. Sets all the required variables like scale and markers.
     */
    private void loadMap() {
        mMap = findViewById(R.id.as_view_map);              // Find the map view

        mMap.setSize( MAP_WIDTH, MAP_HEIGHT );              // Size of original image at 100% scale
        // Map detail levels for zooms
        mMap.addDetailLevel( 1.000f, "tiles/j3np/1000/j3np-%d_%d.png");
        mMap.addDetailLevel( 0.500f, "tiles/j3np/500/j3np-%d_%d.png");
        mMap.addDetailLevel( 0.250f, "tiles/j3np/250/j3np-%d_%d.png");
        mMap.addDetailLevel( 0.125f, "tiles/j3np/125/j3np-%d_%d.png");

        mMap.setScaleLimits( 0, 2 );            // Let the image explode (set scale limits)
        mMap.setScale(0.50F);                   // Set initial scale to have the map zoomed in at first

        mMap.setMarkerAnchorPoints( -0.5f, -0.5f );      // Lets center all markers both horizontally and vertically
        mMap.setMarkerTapListener(mMarkerTapListener);                  // Sets listener on tap events for the markers
        mMap.defineBounds(0,0, MAP_WIDTH, MAP_HEIGHT);         // Define bound to be able to convert Absolute X to Relative X for marker clicks

        // Make the whole map clickable to be able to add new pins (fingerprints)
        HotSpot hotSpot = new HotSpot();
        hotSpot.set(0,0,MAP_WIDTH,MAP_HEIGHT);
        mMap.addHotSpot(hotSpot);
        mMap.setHotSpotTapListener(mHotspotTapListener);

        frameTo( 0.5, 0.5 );                       // Frame the map to the center of the view (does not work in onCreate())

        mMap.setShouldRenderWhilePanning( true );       // Render while panning
        mMap.setShouldLoopScale( false );               // Disallow going back to minimum scale while double-taping at maximum scale (for demo purpose)
    }

    /**
     * Initiates loading of the fingerprints
     */
    private void loadFingerprints() {
        // Connects to the database via AsyncTask and downloads basic fingerprint information
        DatabaseDataLoader<List<Fingerprint>> loader = new DatabaseDataLoader<>(this);
        loader.execute(DatabaseDataLoader.MODE_FINGERPRINT);
    }

    /**
     * Displays all the fingerprints on the map
     */
    private void displayFingerprints() {
        for (Fingerprint fingerprint : mFingerprints) {
            // Initiates image view
            ImageView iw = new ImageView(this);
            int[] fingerprintInfo = { fingerprint.getX(), fingerprint.getY() };
            iw.setTag(fingerprintInfo);                  // Set x and y of the Fingerprint to the view.

            // Sets image resource of the marker
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(84,120);
            iw.setLayoutParams(lp);                                 // Sets layout to the ImageView
            iw.setImageResource(R.drawable.map_marker_normal);      // Sets image resource to the view

            // Adds marker to the map
            mMap.addMarker(iw, fingerprint.getX(), fingerprint.getY(), null, null);
        }
    }

    /**
     * OnClickListener for Markers.
     * It enables to see more information about the specific fingerprint.
     */
    private MarkerLayout.MarkerTapListener mMarkerTapListener = new MarkerLayout.MarkerTapListener() {
        @Override
        public void onMarkerTap(View markerView, int x, int y ) {
            mMarkerClicked = true;           // Set to disable HotSpot click to trigger

            // Adds callout layout to the map
            int[] position = (int[]) markerView.getTag();         // Position of the callout widget
            View markerActions = generateMarkerActionView(position);  // Generates layout of the callout widget
            mMap.addCallout( markerActions, position[0], position[1], -0.5f, -1.0f );
        }
    };

    /**
     * Generates view that enables user to show more information about Fingerprints.
     * Also enables to create new one in the same place.
     *
     * @param position x and y position of the marker
     * @return View with the actions
     */
    private View generateMarkerActionView(int[] position) {
        // Load view via inflater
        LayoutInflater inflater = LayoutInflater.from(ScanActivity.this);
        View calloutView = inflater.inflate(R.layout.actions_marker, null);

        // Parse fingerprint data
        final int fpX = position[0];
        final int fpY = position[1];

        // Button to show more information about selected fingerprint.
        ImageButton buttonInfo = calloutView.findViewById(R.id.am_show_info);
        buttonInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText( ScanActivity.this, "Show information fragment with fingerprints at this position.", Toast.LENGTH_SHORT ).show();
            }
        });

        // Button to create new fingerprint.
        ImageButton buttonCreate = calloutView.findViewById(R.id.am_create);
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create fingerprint for scanning
                Fingerprint fingerprint = new Fingerprint();
                // TODO: have a setter for building and floor
                fingerprint.setLocationEntry(new LocationEntry("J3NP"));
                fingerprint.setX(fpX);
                fingerprint.setY(fpY);

                // Create instance of scanner and start it with execute
                String jsonFinger = gson.toJson(fingerprint);
                PersistableBundle bundle = new PersistableBundle();
                bundle.putString(FingerprintScanner.PARAM_FINGERPRINT, jsonFinger);

                jobBuilder.setExtras(bundle);
                jobScheduler.schedule(jobBuilder.build());

                // Remove single does not work :(
                mMap.getCalloutLayout().removeAllViews();
            }
        });

        // Cancel button in markerView
        ImageButton buttonCancel = calloutView.findViewById(R.id.am_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Remove single does not work :(
                mMap.getCalloutLayout().removeAllViews();
            }
        });

        return calloutView;
    }

    /**
     * OnClickListener for the HotSpots (whole map)
     * It will enable to add new fingerprints at specific positions.
     */
    private HotSpot.HotSpotTapListener mHotspotTapListener = new HotSpot.HotSpotTapListener() {
        @Override
        public void onHotSpotTap(HotSpot hotSpot, int x, int y) {
            // Dos not show anything if Marker was clicked
            if(!mMarkerClicked) {
                CoordinateTranslater ct = mMap.getCoordinateTranslater();
                int realX = (int) ct.translateAndScaleAbsoluteToRelativeX(x, mMap.getScale());
                int realY = (int) ct.translateAndScaleAbsoluteToRelativeY(y, mMap.getScale());
                //Toast.makeText(ScanActivity.this, "You tapped at position: " + realX + ":" + realY, Toast.LENGTH_SHORT).show();

                // Round both numbers to tens to make the steps little easier
                int roundedX = (int) Math.round(realX/10.0) * 10;
                int roundedY = (int) Math.round(realY/10.0) * 10;

                // Adds callout layout to the map
                View markerActions = generateNewMarkerActionView(roundedX, roundedY);  // Generates layout of the callout widget
                mMap.addCallout( markerActions, roundedX, roundedY, -0.5f, -1.0f );
            }
            mMarkerClicked = false;             // Reset marker click variable
        }
    };

    /**
     * Generates view that enables user to create new Fingerprint.
     *
     * @param posX of the new fingerprint
     * @param posY of the new fingerprint
     * @return View of the action menu
     */
    private View generateNewMarkerActionView(final int posX, final int posY) {
        // Load view via inflater
        LayoutInflater inflater = LayoutInflater.from(ScanActivity.this);
        View calloutView = inflater.inflate(R.layout.actions_marker_new, null);

        // Sets position text to the view
        TextView textPosition = calloutView.findViewById(R.id.amn_position);
        textPosition.setText(String.format(getResources().getString(R.string.amn_position), posX, posY));

        // Button to create new fingerprint.
        ImageButton buttonCreate = calloutView.findViewById(R.id.amn_create);
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create fingerprint for scanning
                Fingerprint fingerprint = new Fingerprint();
                // TODO: have a setter for building and floor
                fingerprint.setLocationEntry(new LocationEntry("J3NP"));
                fingerprint.setX(posX);
                fingerprint.setY(posY);

                // Create instance of scanner and start it with execute
                String jsonFinger = gson.toJson(fingerprint);
                PersistableBundle bundle = new PersistableBundle();
                bundle.putString(FingerprintScanner.PARAM_FINGERPRINT, jsonFinger);

                jobBuilder.setExtras(bundle);
                jobScheduler.schedule(jobBuilder.build());

                // Remove single does not work :(
                mMap.getCalloutLayout().removeAllViews();
            }
        });

        // Cancel button in markerView
        ImageButton buttonCancel = calloutView.findViewById(R.id.amn_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Remove single does not work :(
                mMap.getCalloutLayout().removeAllViews();
            }
        });

        return calloutView;
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
