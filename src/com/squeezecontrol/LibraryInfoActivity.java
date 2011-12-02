/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class LibraryInfoActivity extends Activity {

    private SqueezeService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_info);

        ServiceUtils.bindToService(this, new SqueezeServiceConnection() {
            @Override
            public void onServiceConnected(SqueezeService service) {
                mService = service;
                service.getBroker().postCommand(
                        "info total songs ?");

            }
        });

        Button rescanButton = (Button) findViewById(R.id.rescan_library);
        rescanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.getBroker().postCommand("rescan");
                Toast.makeText(LibraryInfoActivity.this,
                        "Library rescan started", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ServiceUtils.unbindFromService(this);
    }
}
