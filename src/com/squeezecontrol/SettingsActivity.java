/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;
import com.squeezecontrol.io.SqueezeBroker;
import com.squeezecontrol.io.SqueezeDiagnostics;
import com.squeezecontrol.io.SqueezeEventListener;

import java.io.IOException;

public class SettingsActivity extends PreferenceActivity implements
        OnPreferenceClickListener, SqueezeEventListener {

    private final int PICK_PLAYER = 0;
    private final int DIALOG_HELP = 0;

    private Preference mTestCli;
    private Preference mTestHttp;
    private Preference mConnect;
    private ConnectivityManager mConnectivityManager;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the XML preferences file
        addPreferencesFromResource(R.xml.preferences);

        mConnect = (Preference) getPreferenceScreen().findPreference(
                "preferences_connect");
        mConnect.setOnPreferenceClickListener(this);

        mTestCli = (Preference) getPreferenceScreen()
                .findPreference("test_cli");
        mTestCli.setOnPreferenceClickListener(this);

        mTestHttp = (Preference) getPreferenceScreen().findPreference(
                "test_http");
        mTestHttp.setOnPreferenceClickListener(this);

        ServiceUtils.requireWifiOrFinish(this);
        mHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ServiceUtils.unbindFromService(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mTestCli) {
            Toast.makeText(this, "Testing CLI port...", Toast.LENGTH_SHORT)
                    .show();
            SqueezeDiagnostics.Result result = SqueezeDiagnostics.testCLI(
                    Settings.getHost(this), Settings.getCLIPort(this), Settings
                    .getUsername(this), Settings.getPassword(this));
            Toast.makeText(this, result.getResult(), Toast.LENGTH_LONG).show();
        } else if (preference == mTestHttp) {
            Toast.makeText(this, "Testing HTTP port...", Toast.LENGTH_LONG)
                    .show();
            String result = SqueezeDiagnostics.testHTTP(Settings.getHost(this),
                    Settings.getHTTPPort(this));
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        } else if (preference == mConnect) {
            connect();
        }
        return true;
    }

    private void connect() {
        new Thread() {
            @Override
            public void run() {
                final SqueezeDiagnostics.Result result = SqueezeDiagnostics.testCLI(
                        Settings.getHost(SettingsActivity.this), Settings
                        .getCLIPort(SettingsActivity.this), Settings
                        .getUsername(SettingsActivity.this), Settings
                        .getPassword(SettingsActivity.this));
                if (!result.isSuccess()) {
                    Settings.setConfigured(SettingsActivity.this, false);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SettingsActivity.this, result.getResult(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                } else {
                    ServiceUtils.bindToService(SettingsActivity.this,
                            new SqueezeServiceConnection() {

                                @Override
                                public void onServiceConnected(
                                        SqueezeService service) {
                                    service.initialize();
                                    service.getBroker().addEventListener(
                                            SettingsActivity.this);
                                    service.getBroker().connect();
                                }
                            });
                }
            }
        }.start();
    }

    @Override
    public void onConnect(SqueezeBroker broker) {
        SqueezeService.showConnectionNotification(this, broker);
        startActivityForResult(new Intent(this, BrowsePlayersActivity.class),
                PICK_PLAYER);
        broker.removeEventListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_PLAYER && resultCode == RESULT_OK) {
            Settings.setConfigured(this, true);
            // TODO: If we came here from a player activity, this will start a new one.
            startActivity(new Intent(this, PlayerActivity.class));
            finish();
        }
    }

    @Override
    public void onConnectionError(SqueezeBroker broker, IOException cause) {
        broker.removeEventListener(this);
        SqueezeService.showConnectionError(this, broker, cause);
    }

    @Override
    public void onDisconnect(SqueezeBroker broker) {
        broker.removeEventListener(this);
    }

}
