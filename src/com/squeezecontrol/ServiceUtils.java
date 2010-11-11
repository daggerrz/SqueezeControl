package com.squeezecontrol;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;

public class ServiceUtils {


	public static boolean bindToService(Context context,
			SqueezeServiceConnection serviceConnection) {
		serviceConnection.onServiceConnected(SqueezeService.getInstance());
		return true;
	}

	public static void unbindFromService(Context context) {
	}

	public static Dialog createWaitScreen(Context context) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setTitle("Connecting");
		dialog.setMessage("Please wait while connecting to SqueezeCenter...");
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		return dialog;

	}

	public static void requireWifiOrFinish(final Activity context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (!wifiManager.isWifiEnabled()) {

			AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			dialog.setMessage(R.string.wlan_required);
			dialog.setTitle("WLAN required");
			dialog.setPositiveButton("Yes", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((WifiManager) context
							.getSystemService(Context.WIFI_SERVICE))
							.setWifiEnabled(true);
				}
			});
			dialog.setNegativeButton("No", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					context.finish();
				}
			});
			dialog.create().show();
		}

	}
}
