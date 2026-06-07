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

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.max2idea.android.limbo.files.FileUtils;

import java.io.File;

/**
 * We use the application context for the initialization of some of the Storage and
 * Controller implementations.
 */
public class LimboApplication extends Application {
    private static final String TAG = "LimboApplication";
    //Do not update these directly, see inherited project java files
    public static Config.Arch arch;

    // 修复：使用应用上下文，避免内存泄漏
    private static Context sAppContext;

    private static String qemuVersionString = "unknown";
    private static int qemuVersion = 0;
    private static String limboVersionString = "unknown";
    private static int limboVersion = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化应用上下文
        arch = Config.Arch.x86_64;
        sAppContext = getApplicationContext();
        // 自动初始化
        initialize();
    }

    /**
     * 获取全局应用上下文（安全无泄漏）
     */
    public static Context getInstance() {
        if (sAppContext == null) {
            throw new IllegalStateException("LimboApplication 未初始化，请在 AndroidManifest 中注册");
        }
        return sAppContext;
    }

    /**
     * 初始化环境信息（版本、目录等）
     */
    public static void setupEnv(Context context) {
        if (context == null) return;

        try {
            // 修复：正确获取包名
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_META_DATA
            );
            limboVersion = packageInfo.versionCode;
            limboVersionString = packageInfo.versionName;

            Log.d(TAG, "Limbo Version Name: " + limboVersionString);
            Log.d(TAG, "Limbo Version Code: " + limboVersion);

            // 读取 QEMU 版本
            String version = FileUtils.LoadFile(context, "QEMU_VERSION", false);
            if (!version.trim().isEmpty()) {
                qemuVersionString = version.trim();
                String[] qemuVersionParts = qemuVersionString.split("\\.");
                if (qemuVersionParts.length >= 3) {
                    qemuVersion = Integer.parseInt(qemuVersionParts[0]) * 10000
                            + Integer.parseInt(qemuVersionParts[1]) * 100
                            + Integer.parseInt(qemuVersionParts[2]);
                }
            }

            Log.d(TAG, "Qemu Version: " + qemuVersionString);
            Log.d(TAG, "Qemu Version Number: " + qemuVersion);

        } catch (Exception e) {
            Log.e(TAG, "Could not load version information: " + e.getMessage());
        }
    }

    /**
     * 获取应用 UID
     */
    @NonNull
    public static String getUserId(@NonNull Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(),
                    PackageManager.GET_META_DATA
            );
            return String.valueOf(appInfo.uid);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getUserId: ", e);
            return "None";
        }
    }

    /**
     * 设备是否支持 64 位
     */
    public static boolean isHost64Bit() {
        return Build.SUPPORTED_64_BIT_ABIS != null && Build.SUPPORTED_64_BIT_ABIS.length > 0;
    }

    /**
     * 设备是否为 x86_64
     */
    public static boolean isHostX86_64() {
        if (Build.SUPPORTED_64_BIT_ABIS != null) {
            for (String abi : Build.SUPPORTED_64_BIT_ABIS) {
                if ("x86_64".equals(abi)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 设备是否为 32 位 x86
     */
    public static boolean isHostX86() {
        if (Build.SUPPORTED_32_BIT_ABIS != null) {
            for (String abi : Build.SUPPORTED_32_BIT_ABIS) {
                if ("x86".equals(abi)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 设备是否为 32 位 ARM
     */
    public static boolean isHostArm() {
        if (Build.SUPPORTED_32_BIT_ABIS != null) {
            for (String abi : Build.SUPPORTED_32_BIT_ABIS) {
                if ("armeabi-v7a".equals(abi)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 设备是否为 ARM64-v8a
     */
    public static boolean isHostArmv8() {
        if (Build.SUPPORTED_64_BIT_ABIS != null) {
            for (String abi : Build.SUPPORTED_64_BIT_ABIS) {
                if ("arm64-v8a".equals(abi)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 基础文件目录
     */
    @NonNull
    public static String getBasefileDir() {
        return getInstance().getCacheDir() + "/limbo/";
    }

    /**
     * 临时文件夹
     */
    @NonNull
    public static String getTmpFolder() {
        return getBasefileDir() + "var/tmp";
    }

    /**
     * 虚拟机目录
     */
    @NonNull
    public static String getMachineDir() {
        return getBasefileDir() + Config.machineFolder;
    }

    /**
     * QMP 套接字路径
     */
    @NonNull
    public static String getLocalQMPSocketPath() {
        return getInstance().getCacheDir() + "/qmpsocket";
    }

    // ====================== Getter ======================
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

    /**
     * 全局初始化
     */
    public static void initialize() {
        try {
            Class.forName("android.os.AsyncTask");
        } catch (Throwable ignore) {}

        setupEnv(getInstance());
        setupFolders();
    }

    /**
     * 创建必要文件夹
     */
    private static void setupFolders() {
        try {
            // 外部存储路径
            Config.storagedir = Environment.getExternalStorageDirectory().getAbsolutePath();

            // 创建临时目录
            File tmpFolder = new File(getTmpFolder());
            if (!tmpFolder.exists() && !tmpFolder.mkdirs()) {
                Log.e(TAG, "无法创建临时文件夹: " + tmpFolder.getPath());
            }
        } catch (Exception e) {
            Log.e(TAG, "setupFolders 失败: ", e);
        }
    }
}