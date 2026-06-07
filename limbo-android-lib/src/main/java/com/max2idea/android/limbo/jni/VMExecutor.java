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
package com.max2idea.android.limbo.jni;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.limbo.emu.lib.R;
import com.max2idea.android.limbo.files.FileUtils;
import com.max2idea.android.limbo.main.Config;
import com.max2idea.android.limbo.main.LimboApplication;
import com.max2idea.android.limbo.main.LimboSettingsManager;
import com.max2idea.android.limbo.qmp.QmpClient;

import org.json.JSONObject;

/**
 * Class is used to start and stop the qemu process and communicate file descriptions, mouse,
 * and keyboard events.
 */
public class VMExecutor {
    private static final String TAG = "VMExecutor";
    private static int vm_width;
    private static int vm_height;

    // ==================== 新增：适配原生代码的 JNI 方法 ====================
    public static native void nativeSetBinderObject(Object obj);

    // JNI Methods
    public static native String start(String storage_dir, String base_dir,
                                      String lib_filename, String lib_path,
                                      int sdl_scale_hint, Object[] params);

    public static native String stop(int restart);

    public static native void setSDLRefreshRateDefault(int value);

    public static native void setSDLRefreshRateIdle(int value);

    public static native int getSDLRefreshRateDefault();

    public static native int getSDLRefreshRateIdle();

    public static native void nativeIgnoreBreakpointInvalidate(int value);

    public static native void nativeMouseEvent(int button, int action, int relative, int x, int y);

    public static native void nativeMouseBounds(int xmin, int xmax, int ymin, int ymax);

    public static native void nativeFullscreen();

    public static native void nativeRefreshScreen(int value);

    public static native void nativeEnableAaudio(int value, String aaudioLibName, String aaudioLibPath);

    protected String changedev(String dev, String value) {
        try {
            String response = QmpClient.sendCommand(QmpClient.getChangeDeviceCommand(dev, value));
            String displayDevValue = FileUtils.getFullPathFromDocumentFilePath(value);
            if (Config.debug) {
                Log.i(TAG, LimboApplication.getInstance().getString(R.string.ChangedDevice) + ": " + dev + ": " + displayDevValue);
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "changedev failed: " + e.getMessage());
            return null;
        }
    }

    protected String ejectdev(String dev) {
        try {
            String response = QmpClient.sendCommand(QmpClient.getEjectDeviceCommand(dev));
            if (Config.debug) {
                Log.i(TAG, LimboApplication.getInstance().getString(R.string.EjectedDevice) + ": " + dev);
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "ejectdev failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Starts the native process. This should be called from a background thread from a
     * foreground service in order to prevent the process from being killed
     *
     * @return String from the native code vm-executor-jni.cpp
     */
    @Nullable
    public static String start(String[] params) {
        if (params == null || params.length == 0) {
            Log.e(TAG, "start: params is null or empty");
        } else {
            printParams(params);
        }

        String res = null;
        try {
            // ==================== 关键：初始化绑定原生对象（修复崩溃） ====================
            nativeSetBinderObject(new VMExecutor());

            QmpClient.setExternal(LimboSettingsManager.getEnableExternalQMP(LimboApplication.getInstance()));
            String libFilename = getQemuLibrary();
            String libPath = FileUtils.getNativeLibDir(LimboApplication.getInstance()) + "/" + libFilename;

            Log.i(TAG, "Loading QEMU library: " + libPath);

            res = start(Config.storagedir,
                    LimboApplication.getBasefileDir(),
                    libFilename,
                    libPath,
                    Config.SDLHintScale,
                    params);

        } catch (Exception ex) {
            Log.e(TAG, "VM start failed: " + ex.getMessage(), ex);
        }
        return res;
    }

    public static void printParams(@NonNull String[] params) {
        Log.d(TAG, "Params:");
        for (int i = 0; i < params.length; i++) {
            Log.d(TAG, i + ": " + params[i]);
        }
    }

    @NonNull
    private static String getQemuLibrary() {
        if (LimboApplication.arch == null) {
            throw new IllegalStateException("LimboApplication.arch is not initialized");
        }

        switch (LimboApplication.arch) {
            case x86:
                return "libqemu-system-i386.so";
            case x86_64:
                return "libqemu-system-x86_64.so";
            case arm:
                return "libqemu-system-arm.so";
            case arm64:
                return "libqemu-system-aarch64.so";
            default:
                throw new IllegalStateException("Unsupported architecture: " + LimboApplication.arch);
        }
    }

    public void stopvm(final int restart) {
        new Thread(() -> {
            try {
                if (restart != 0) {
                    Log.i(TAG, "Restarting VM via QMP reset");
                    QmpClient.sendCommand(QmpClient.getResetCommand());
                } else {
                    Log.i(TAG, "Stopping VM native process");
                    stop(restart);
                }
            } catch (Exception e) {
                Log.e(TAG, "stopvm failed: " + e.getMessage(), e);
            }
        }).start();
    }

    public int getSdlRefreshRate(boolean idle) {
        return idle ? getSDLRefreshRateIdle() : getSDLRefreshRateDefault();
    }

    public void setSdlRefreshRate(int value, boolean idle) {
        if (idle) {
            setSDLRefreshRateIdle(value);
        } else {
            setSDLRefreshRateDefault(value);
        }
    }

    public void setFullscreen() {
        nativeFullscreen();
        if (LimboApplication.arch == Config.Arch.x86
                || LimboApplication.arch == Config.Arch.x86_64
                || LimboApplication.arch == Config.Arch.arm
                || LimboApplication.arch == Config.Arch.arm64) {
            nativeRefreshScreen(1);
        }
    }

    public void enableAaudio(int value) {
        try {
            String aaudioPath = FileUtils.getNativeLibDir(LimboApplication.getInstance()) + "/" + Config.aaudioLibName;
            nativeEnableAaudio(value, Config.aaudioLibName, aaudioPath);
        } catch (Exception e) {
            Log.e(TAG, "enableAaudio failed: " + e.getMessage());
        }
    }

    public int get_fd(String path) {
        try {
            return FileUtils.get_fd(path);
        } catch (Exception e) {
            Log.e(TAG, "get_fd failed: " + e.getMessage());
            return -1;
        }
    }

    public int close_fd(int fd) {
        try {
            return FileUtils.close_fd(fd);
        } catch (Exception e) {
            Log.e(TAG, "close_fd failed: " + e.getMessage());
            return -1;
        }
    }

    public void continueVM() {
        try {
            String command = QmpClient.getContinueVMCommand();
            QmpClient.sendCommand(command);
        } catch (Exception e) {
            Log.e(TAG, "continueVM failed: " + e.getMessage());
        }
    }

    @Nullable
    private String processMigrationResponse(String response) {
        if (response == null) return null;
        try {
            JSONObject object = new JSONObject(response);
            String errorStr = object.optString("error", null);
            if (errorStr == null) return null;

            JSONObject descObj = new JSONObject(errorStr);
            return descObj.optString("desc", errorStr);

        } catch (Exception ex) {
            if (Config.debug) ex.printStackTrace();
            return null;
        }
    }

    public boolean getQMPAllowExternal() {
        return LimboSettingsManager.getEnableExternalQMP(LimboApplication.getInstance());
    }
}