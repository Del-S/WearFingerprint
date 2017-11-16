package kikm.fim.uhk.cz.wearnavigationsimple.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import kikm.fim.uhk.cz.wearnavigationsimple.R;

public class SimpleDialogHelper {

    /**
     * Shows alert dialog for leaving app.
     * - Because with bottom menu implementation you can click back button and kill the app instantly.
     */
    public static AlertDialog dialogLeavingApp(final Activity activity, DialogInterface.OnClickListener confirm) {
        // Build alert dialog to display
        AlertDialog.Builder adb = new AlertDialog.Builder(activity, R.style.DarkAlertDialog)
                .setTitle(R.string.app_dialog_leaving_title)
                .setMessage(R.string.app_dialog_leaving_message)
                .setNegativeButton(R.string.app_no, null);

        // Set passed confirm listener or create default one closing the app
        if(confirm != null) {
            adb.setPositiveButton(R.string.app_yes, confirm);
        } else {
            adb.setPositiveButton(R.string.app_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activity.finishAffinity();
                }

            });
        }
        return adb.create();
    }

}
