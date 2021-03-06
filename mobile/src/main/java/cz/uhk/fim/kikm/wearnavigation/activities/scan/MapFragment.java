package cz.uhk.fim.kikm.wearnavigation.activities.scan;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.LocationEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseDataInterface;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseDataLoader;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.FingerprintScanner;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.ScanProgress;
import cz.uhk.fim.kikm.wearnavigation.utils.animations.AnimationHelper;
import cz.uhk.fim.kikm.wearnavigation.utils.wearCommunication.WearDataSender;

public class MapFragment extends Fragment implements DatabaseDataInterface {

    private DeviceEntry mDevice;

    // Map stuff
    private TileView mMap;                      // Map view
    private List<Fingerprint> mFingerprints;    // List of the fingerprints loaded and kept for display here
    private boolean mMarkerClicked = false;     // Disables HotSpot click if marker was clicked

    // Sizes of the map
    private static final int MAP_WIDTH = 3000;
    private static final int MAP_HEIGHT = 3000;

    private final Gson gson = new Gson();       // Class to json (and reverse) parser
    private JobScheduler mJobScheduler;         // JobScheduler used to run FingerprintScanner
    private JobInfo.Builder mJobBuilder;        // JobInfo.Builder used to create a job to run in Scheduler
    private LocationManager mLocationManager;   // Location manager to get location from the network
    private DatabaseCRUD mDatabase;
    private FragmentActivity mActivity;
    private WearDataSender mWearDataSender;
    private AnimationHelper mAnimationHelper;
    private MapClickCallback mCallback;    // Callback to the activity

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = getActivity();
        mFingerprints = new ArrayList<>();

        // Load location manager
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mJobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        mDatabase = new DatabaseCRUD(context);

        Configuration config = Configuration.getConfiguration(context);
        if(config != null) {
            mDevice = config.getDevice(context);
        }

        // Initiate callback connection to the activity or throw an Exception
        try {
            mCallback = (MapClickCallback) context;
        } catch (Exception e) {
            throw new ClassCastException(context.getClass().getName()
                    + " must implement MapClickCallback");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map_fingerprint, container, false);

        // Load instances from Activity
        BaseActivity bActivity = ((BaseActivity) getActivity());
        if (bActivity != null) {
            mWearDataSender = bActivity.getWearDataSender();
            mJobBuilder = bActivity.getJobBuilder();
            mAnimationHelper = bActivity.getAnimationHelper();
        }

        if(mMap == null) {
            loadMap(rootView);      // Find and load map
            loadFingerprints();     // Loads fingerprint data
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMap.resume();

        if(mActivity != null)
            mActivity = getActivity();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMap.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMap.destroy();
        mMap = null;
    }

    /**
     * Loads map and tiles of the map. Sets all the required variables like scale and markers.
     */
    private void loadMap(View rootView) {
        if(rootView != null) {
            mMap = rootView.findViewById(R.id.fma_view_map);   // Find the map view

            mMap.setSize(MAP_WIDTH, MAP_HEIGHT);              // Size of original image at 100% scale
            // Map detail levels for zooms
            mMap.addDetailLevel(1.000f, "tiles/j3np/1000/j3np-%d_%d.png");
            mMap.addDetailLevel(0.500f, "tiles/j3np/500/j3np-%d_%d.png");
            mMap.addDetailLevel(0.250f, "tiles/j3np/250/j3np-%d_%d.png");
            mMap.addDetailLevel(0.125f, "tiles/j3np/125/j3np-%d_%d.png");

            mMap.setScaleLimits(0, 2);            // Let the image explode (set scale limits)
            mMap.setScale(0.50F);                   // Set initial scale to have the map zoomed in at first

            mMap.setMarkerTapListener(mMarkerTapListener);                  // Sets listener on tap events for the markers
            mMap.defineBounds(0, 0, MAP_WIDTH, MAP_HEIGHT);         // Define bound to be able to convert Absolute X to Relative X for marker clicks
            mMap.setMarkerAnchorPoints(-0.5f, -0.5f);     // Lets center all markers both horizontally and vertically

            // Make the whole map clickable to be able to add new pins (fingerprints)
            HotSpot hotSpot = new HotSpot();
            hotSpot.set(0, 0, MAP_WIDTH, MAP_HEIGHT);
            mMap.addHotSpot(hotSpot);
            mMap.setHotSpotTapListener(mHotspotTapListener);

            frameTo(MAP_WIDTH / 2, MAP_HEIGHT / 2);  // Frame the map to the center of the view (does not work in onCreate())

            mMap.setShouldRenderWhilePanning(true);       // Render while panning
            mMap.setShouldLoopScale(false);               // Disallow going back to minimum scale while double-taping at maximum scale (for demo purpose)
        }
    }

    /**
     * Initiates loading of the fingerprints.
     */
    private void loadFingerprints() {
        if(mDatabase != null) {
            // Connects to the database via AsyncTask and downloads basic fingerprint information
            mFingerprints.clear();
            DatabaseDataLoader loader = new DatabaseDataLoader(mDatabase, this);
            loader.execute(DatabaseDataLoader.MODE_FINGERPRINT_POSITIONS);
        }
    }

    /**
     * Displays all the fingerprints on the map.
     */
    private void displayFingerprints() {
        // Clear the map
        mMap.getMarkerLayout().removeAllViews();

        for (Fingerprint fingerprint : mFingerprints) {
            // Initiates image view
            ImageView iw = new ImageView(getActivity());
            int[] fingerprintInfo = { fingerprint.getX(), fingerprint.getY() };
            iw.setTag(fingerprintInfo);                  // Set x and y of the Fingerprint to the view.

            // Sets image resource of the marker
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(84,120);
            iw.setLayoutParams(lp);                                 // Sets layout to the ImageView

            DeviceEntry fingerprintDevice = fingerprint.getDeviceEntry();
            // Sets image resource to the view
            if(fingerprintDevice != null && fingerprintDevice.getType().equals("wear"))
                iw.setImageResource(R.drawable.map_marker_normal_wear);     // Set wear icon
            else
                iw.setImageResource(R.drawable.map_marker_normal_phone);    // Set phone icon

            // TODO: add images for wear devices
            // Set image resource based this device
            if(mDevice != null && fingerprintDevice != null && (mDevice.equals(fingerprintDevice) ||
                    (mDevice.getTelephone() != null &&
                            mDevice.getTelephone().equals(fingerprintDevice.getTelephone())))) {
                // Sets image resource to the view
                if( fingerprintDevice.getType().equals("wear"))
                    iw.setImageResource(R.drawable.map_marker_own_wear);     // Set wear icon
                else
                    iw.setImageResource(R.drawable.map_marker_own_phone);    // Set phone icon
            }

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

            // Adds call-out layout to the map
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
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View calloutView = inflater.inflate(R.layout.actions_marker, null);

        // Parse fingerprint data
        final int posX = position[0];
        final int posY = position[1];

        // Button to show more information about selected position.
        ImageButton buttonInfo = calloutView.findViewById(R.id.am_show_info);
        buttonInfo.setOnClickListener(v -> {
            mCallback.onPositionClick(posX, posY);
        });

        // Button to create new fingerprint.
        ImageButton buttonCreate = calloutView.findViewById(R.id.am_create);
        buttonCreate.setOnClickListener(v -> {
            // Run a new scan only if there is no other running.
            if(!isScanRunning()) {
                Fingerprint fingerprint = buildFingerprint(posX, posY);     // Build fingerprint to save scan data to
                mWearDataSender.initiateScanStart(fingerprint);             // Triggers scan on wear device
                runLocalFingerprintScanner(fingerprint);                    // Triggers a fingerprint scanner job on the phone
                mMap.getCalloutLayout().removeAllViews();                   // Remove single does not work :(
            } else {
                Toast.makeText(mActivity, R.string.fma_scan_already_running, Toast.LENGTH_SHORT).show();
            }
        });

        // Button to delete last two fingerprints
        ImageButton buttonDelete = calloutView.findViewById(R.id.am_delete);
        buttonDelete.setOnClickListener(v -> {
            // TODO: This is only a test
            mDatabase.deleteNewestFingerprintAtPosition(posX, posY);
            updateUI();
            mMap.getCalloutLayout().removeAllViews();                   // Remove single does not work :(
            Toast.makeText(mActivity, "Deleted last fingerprint group", Toast.LENGTH_SHORT).show();
        });

        // Cancel button in markerView
        ImageButton buttonCancel = calloutView.findViewById(R.id.am_cancel);
        buttonCancel.setOnClickListener(v -> {
            // Remove single does not work :(
            mMap.getCalloutLayout().removeAllViews();
        });

        // Disable button if there is a scan running.
        if(isScanRunning()) {
            buttonCreate.setEnabled(false);
        } else {
            buttonCreate.setEnabled(true);
        }

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
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View calloutView = inflater.inflate(R.layout.actions_marker_new, null);

        // Sets position text to the view
        TextView textPosition = calloutView.findViewById(R.id.amn_position);
        textPosition.setText(String.format(getResources().getString(R.string.amn_position), posX, posY));

        // Button to create new fingerprint.
        ImageButton buttonCreate = calloutView.findViewById(R.id.amn_create);
        buttonCreate.setOnClickListener(v -> {
            if(!isScanRunning()) {
                Fingerprint fingerprint = buildFingerprint(posX, posY);     // Build fingerprint to save scan data to
                mWearDataSender.initiateScanStart(fingerprint);             // Triggers scan on wear device
                runLocalFingerprintScanner(fingerprint);                    // Triggers a fingerprint scanner job on the phone
                mMap.getCalloutLayout().removeAllViews();                   // Remove single does not work :(
            } else {
                Toast.makeText(mActivity, R.string.fma_scan_already_running, Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel button in markerView
        ImageButton buttonCancel = calloutView.findViewById(R.id.amn_cancel);
        buttonCancel.setOnClickListener(v -> {
            // Remove single does not work :(
            mMap.getCalloutLayout().removeAllViews();
        });

        // Disable button if there is a scan running.
        if(isScanRunning()) {
            buttonCreate.setEnabled(false);
        } else {
            buttonCreate.setEnabled(true);
        }

        return calloutView;
    }

    /**
     * Creates fingerprint for scanning. Sets all necessary data for scanning.
     *
     * @param posX of fingerprint
     * @param posY of fingerprint
     * @return Fingerprint to add scan data to
     */
    private Fingerprint buildFingerprint(int posX, int posY) {
        // Create fingerprint for scanning
        Fingerprint fingerprint = new Fingerprint(mActivity);
        // TODO: have a setter for building and floor
        fingerprint.setLocationEntry(new LocationEntry("J3NP"));
        //fingerprint.setLocationEntry(new LocationEntry("TEST"));
        // TODO: Have a parameter for that
        fingerprint.setScanLength(30000);
        fingerprint.setX(posX);
        fingerprint.setY(posY);

        return fingerprint;
    }

    /**
     * Builds and runs the FingerprintScanner job.
     * Creates Fingerprint based on position in the map and sends it into the scanner.
     *
     * @param fingerprint to save scan data to
     */
    private void runLocalFingerprintScanner(Fingerprint fingerprint) {
        displayScanProgressOverlay();   // Displays scan overlay to show that scan is scheduling

        // Getting last knows location from Network
        double[] lastKnownLocation = {0, 0};
        if (mLocationManager != null &&
                ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            lastKnownLocation[0] = location.getLatitude();
            lastKnownLocation[1] = location.getLongitude();
        }

        // Create instance of scanner and start it with execute
        String jsonFinger = gson.toJson(fingerprint);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(FingerprintScanner.PARAM_FINGERPRINT, jsonFinger);
        bundle.putDoubleArray(FingerprintScanner.PARAM_LOCATION, lastKnownLocation);

        // Run the job
        mJobBuilder.setExtras(bundle);                   // Set extra bundle data
        mJobScheduler.schedule(mJobBuilder.build());     // Schedule job to run
    }

    /**
     * Display scan status before scan is started to show it is being scheduled.
     */
    private void displayScanProgressOverlay() {
        // Create instance of scanProgress and set proper variables
        ScanProgress scanProgress = new ScanProgress();
        scanProgress.setStateString(getResources().getString(R.string.spo_status_creating));  // Set state to scheduling
        scanProgress.setScanLength(100);    // Scan length to 100 so progress bar would not be 0 max.

        // Display scan status via BaseActivity AnimationHelper
        mAnimationHelper.displayScanStatus(mActivity, scanProgress, View.VISIBLE, 1000);
    }

    /**
     * Check if there is a scan running.
     *
     * @return true/false scan running.
     */
    private boolean isScanRunning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return mJobScheduler.getPendingJob(FingerprintScanner.JOB_ID) != null;   // Check if job is pending based on id
        } else {
            // Check all the jobs and if one of the ids is the same then it is running
            List<JobInfo> jobs = mJobScheduler.getAllPendingJobs();
            for (JobInfo job : jobs) {
                if (job.getId() == FingerprintScanner.JOB_ID)
                    return true;
            }

            return false;
        }
    }

    /**
     * This is a convenience method to scrollToAndCenter after layout (which won't happen if called directly in onCreate
     * see https://github.com/moagrius/TileView/wiki/FAQ
     */
    public void frameTo( final double x, final double y ) {
        mMap.post(() -> mMap.scrollToAndCenter( x, y ));
    }

    /**
     * Updates map by loading fingerprints.
     */
    public void updateUI() {
        loadFingerprints();     // Loads fingerprint data
    }

    @Override
    public void loadedFingerprintPositions(List<Fingerprint> result) {
        if(result != null && !result.isEmpty()) {
            mFingerprints.addAll(result);
            displayFingerprints();
        }
    }

    @Override
    public void loadError() {
        Toast.makeText(mActivity, "There was an error while loading data.", Toast.LENGTH_SHORT).show();
    }
}
