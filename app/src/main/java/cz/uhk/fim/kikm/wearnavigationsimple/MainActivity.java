package cz.uhk.fim.kikm.wearnavigationsimple;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Bundle;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bluetooth check
        checkBluetooth();
    }

    @Override
    protected void updateUI() {
        // Not used
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_main;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.action_show_main;
    }
}
