package cz.uhk.fim.kikm.wearnavigation.activities.scan;

/**
 * Handles communication between Fragment and Activity.
 */
public interface MapClickCallback {
    /**
     * Enables to show fingerprints at specific position.
     *
     * @param posX clicked
     * @param posY clicked
     */
    void onPositionClick(int posX, int posY);
}
