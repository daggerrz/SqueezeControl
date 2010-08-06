package com.squeezecontrol;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class ServiceUtils {

	private static HashMap<Context, ServiceConnection> mServiceConnections = new HashMap<Context, ServiceConnection>();

	public static boolean bindToService(Context context,
			SqueezeServiceConnection serviceConnection) {
		Intent bindIntent = new Intent(context, SqueezeService.class);
		context.startService(bindIntent);
		mServiceConnections.put(context, serviceConnection);
		return context.bindService(bindIntent, serviceConnection,
				Context.BIND_AUTO_CREATE);
	}

	public static void unbindFromService(Context context) {
		ServiceConnection conn = (ServiceConnection) mServiceConnections
				.remove(context);
		if (conn == null) {
			Log.e("ServiceUtils", "Trying to unbind for unknown Context");
			return;
		}
		context.unbindService(conn);
		if (mServiceConnections.size() == 0) closeService();
	}

	public static void closeService() {
		for (Map.Entry<Context, ServiceConnection> e : mServiceConnections
				.entrySet()) {
			unbindFromService(e.getKey());
			Intent bindIntent = new Intent(e.getKey(), SqueezeService.class);
			e.getKey().stopService(bindIntent);
		}
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
