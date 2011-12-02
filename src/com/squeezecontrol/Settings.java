/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class Settings {

    private static final String TAG = "Settings";

    public static final String CONFIGURED_KEY = "configured";
    public static final String HOST_KEY = "server_address";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";
    public static final String CLI_PORT_KEY = "cli_port";
    public static final String HTTP_PORT_KEY = "http_port";
    public static final String PLAYER_ID_KEY = "player_id";

    public static final String LAST_RUN_VERSION = "last_run_version";

    private static int runs = 0;

    public static String getHost(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(HOST_KEY, null);
    }

    public static int getCLIPort(Context context) {
        String port = PreferenceManager.getDefaultSharedPreferences(
                context).getString(CLI_PORT_KEY, "9090");
        return port == null || "".equals(port) ? 9090 : Integer.valueOf(port);
    }

    public static int getHTTPPort(Context context) {
        return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(
                context).getString(HTTP_PORT_KEY, "9000"));
    }

    public static String getPassword(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PASSWORD_KEY, null);
    }

    public static String getUsername(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(USERNAME_KEY, null);
    }

    public static String getPlayerId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PLAYER_ID_KEY, null);
    }

    public static void setPlayerId(Context context, String id) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PLAYER_ID_KEY, id).commit();
    }

    public static boolean isConfigured(Context context) {

        //  if (runs++ == 0) {
        //  PreferenceManager.getDefaultSharedPreferences(context
        //  ).edit().clear().commit(); }

        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(CONFIGURED_KEY, false);
    }

    public static void setConfigured(Context context, boolean configured) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(CONFIGURED_KEY, configured).commit();
    }

    public static boolean isNewVersionInstalled(Activity context) {
        int lastRunVersion = PreferenceManager.getDefaultSharedPreferences(
                context).getInt(LAST_RUN_VERSION, -1);
        try {
            // Get the current version
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(),
                    0);
            if (lastRunVersion < info.versionCode) {
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putInt(LAST_RUN_VERSION, info.versionCode).commit();
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            Log
                    .e(
                            TAG,
                            "Couldn't find package information in PackageManager",
                            e);
            return false;
        }
    }

}
