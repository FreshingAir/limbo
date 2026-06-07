/*
Copyright (C) Max Kastanas 2012

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package com.max2idea.android.limbo.main;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.max2idea.android.limbo.files.FileUtils;

import java.io.File;

/**
 * We use the application context for the initiliazation of some of the Storage and
 * Controller implementations.
 */
public class LimboApplication {
    private static final String TAG = "LimboApplication";
    //Do not update these directly, see inherited project java files
    public static Config.Arch arch;
    private static Context sInstance;
    private static String qemuVersionString;
    private static int qemuVersion;
    private static String limboVersionString;
    private static int limboVersion;

    public static Context getInstance() {
        return sInstance;
    }

    public static void setupEnv(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getClass().getPackage().getName(),
                    PackageManager.GET_META_DATA);
            limboVersion = packageInfo.versionCode;
            limboVersionString = packageInfo.versionName;
            Log.d(TAG, "Limbo Version: " + limboVersion);
            Log.d(TAG, "Limbo Version Code: " + limboVersionString);

            qemuVersionString = FileUtils.LoadFile(context, "QEMU_VERSION", false);
            String [] qemuVersionParts = qemuVersionString.trim().split("\\.");
            qemuVersion = Integer.parseInt(qemuVersionParts[0]) * 10000
                    + Integer.parseInt(qemuVersionParts[1]) * 100
                    + Integer.parseInt(qemuVersionParts[2]);
            Log.d(TAG, "Qemu Version: " + qemuVersionString);
            Log.d(TAG, "Qemu Version Number: " + qemuVersion);
        } catch (Exception e) {
//            e.printStackTrace();
            Log.e(TAG, "Could not load version information: " + e);
        }
    }

    public static String getUserId(@NonNull Context context) {
        String userid = "None";
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getClass().getPackage().getName(),
                    PackageManager.GET_META_DATA);
            userid = appInfo.uid + "";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return userid;
    }

    public static boolean isHost64Bit() {
        return Build.SUPPORTED_64_BIT_ABIS != null && Build.SUPPORTED_64_BIT_ABIS.length > 0;
    }

    // Legacy
    public static boolean isHostX86_64() {
        if (Build.SUPPORTED_64_BIT_ABIS != null) {
            for (int i = 0; i < Build.SUPPORTED_64_BIT_ABIS.length; i++)
                if (Build.SUPPORTED_64_BIT_ABIS[i].equals("x86_64"))
                    return true;
        }
        return false;
    }

    // Legacy
    public static boolean isHostX86() {
        if (Build.SUPPORTED_32_BIT_ABIS != null) {
            for (int i = 0; i < Build.SUPPORTED_32_BIT_ABIS.length; i++)
                if (Build.SUPPORTED_32_BIT_ABIS[i].equals("x86"))
                    return true;
        }
        return false;
    }

    public static boolean isHostArm() {
        if (Build.SUPPORTED_32_BIT_ABIS != null) {
            for (int i = 0; i < Build.SUPPORTED_32_BIT_ABIS.length; i++)
                if (Build.SUPPORTED_32_BIT_ABIS[i].equals("armeabi-v7a"))
                    return true;
        }
        return false;
    }

    public static boolean isHostArmv8() {
        if (Build.SUPPORTED_64_BIT_ABIS != null) {
            for (int i = 0; i < Build.SUPPORTED_64_BIT_ABIS.length; i++)
                if (Build.SUPPORTED_64_BIT_ABIS[i].equals("arm64-v8a"))
                    return true;
        }
        return false;
    }


    @NonNull
    public static String getBasefileDir() {
        return getInstance().getCacheDir() + "/limbo/";
    }

    @NonNull
    public static String getTmpFolder() {
        return getBasefileDir() + "var/tmp"; // Do not modify
    }

    @NonNull
    public static String getMachineDir() {
        return getBasefileDir() + Config.machineFolder;
    }

    @NonNull
    public static String getLocalQMPSocketPath() {
        return getInstance().getCacheDir() + "/qmpsocket";
    }

    public static String getQemuVersionString() {
        return qemuVersionString;
    }

    public static int getQemuVersion() {
        return qemuVersion;
    }

    public static String getLimboVersionString() {
        return limboVersionString;
    }

    public static int getLimboVersion() {
        return limboVersion;
    }

    public static void initialize() {
        try {
            Class.forName("android.os.AsyncTask");
        } catch (Throwable ignore) {
            // ignored
        }
//        MachineOpenHelper.initialize(context);
//        FavOpenHelper.initialize(context);
        setupFolders();
    }

    private static void setupFolders() {
        Config.storagedir = Environment.getExternalStorageDirectory().toString();
        File folder = new File(LimboApplication.getTmpFolder());
        if (!folder.exists()) {
            boolean res = folder.mkdirs();
            if (!res) {
                Log.e(TAG, "Could not create temp folder: " + folder.getPath());
            }
        }
    }
}
