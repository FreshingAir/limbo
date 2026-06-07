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

    //JNI Methods
    public static native String start(String storage_dir, String base_dir,
                                      String lib_filename, String lib_path,
                                      int sdl_scale_hint, Object[] params);

    private native String stop(int restart);

    public native void setSDLRefreshRateDefault(int value);

    public native void setSDLRefreshRateIdle(int value);

    public native int getSDLRefreshRateDefault();

    public native int getSDLRefreshRateIdle();

    public native void nativeIgnoreBreakpointInvalidate(int value);

    public native void nativeMouseEvent(int button, int action, int relative, int x, int y);

    public native void nativeMouseBounds(int xmin, int xmax, int ymin, int ymax);

    public native void nativeFullscreen();

    public native void nativeRefreshScreen(int value);

    public native void nativeEnableAaudio(int value, String aaudioLibName, String aaudioLibPath);

    protected String changedev(String dev, String value) {
        String response = QmpClient.sendCommand(QmpClient.getChangeDeviceCommand(dev, value));
        String displayDevValue = FileUtils.getFullPathFromDocumentFilePath(value);
        if (Config.debug)
            Log.i(TAG, LimboApplication.getInstance().getString(R.string.ChangedDevice) + ": " + dev + ": " + displayDevValue);
        return response;
    }
    protected String ejectdev(String dev) {
        String response = QmpClient.sendCommand(QmpClient.getEjectDeviceCommand(dev));
        if (Config.debug)
            Log.i(TAG, LimboApplication.getInstance().getString(R.string.EjectedDevice) + ": " + dev);
        return response;
    }
    /**
     * Starts the native process. This should be called from a background thread from a
     * foreground service in order to prevent the process from being killed
     *
     * @return String from the native code vm-executor-jni.cpp
     */
    public static String start(String[] params) {
        String res = null;
        try {
            QmpClient.setExternal(LimboSettingsManager.getEnableExternalQMP(LimboApplication.getInstance()));
            String libFilename = getQemuLibrary();
            res = start(Config.storagedir, LimboApplication.getBasefileDir(),
                    libFilename, FileUtils.getNativeLibDir(LimboApplication.getInstance()) + "/" + libFilename,
                    Config.SDLHintScale, params);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            return res;
        }
        return res;
    }

    public void printParams(@NonNull String[] params) {
        Log.d(TAG, "Params:");
        for (int i = 0; i < params.length; i++) {
            Log.d(TAG, i + ": " + params[i]);
        }
    }
    @NonNull
    private static String getQemuLibrary() {
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
                throw new IllegalStateException("Unexpected value: " + LimboApplication.arch);
        }
    }
    public void stopvm(final int restart) {
        new Thread(() -> {
            if (restart != 0) {
                QmpClient.sendCommand(QmpClient.getResetCommand());
            } else {
                //XXX: Qmp command only halts the VM but doesn't exit so we use force close
//            QmpClient.sendCommand(QmpClient.powerDown());
                stop(restart);
            }
        }).start();
    }
    public int getSdlRefreshRate(boolean idle) {
        if (idle)
            return getSDLRefreshRateIdle();
        else
            return getSDLRefreshRateDefault();
    }
    public void setSdlRefreshRate(int value, boolean idle) {
        if (idle)
            setSDLRefreshRateIdle(value);
        else
            setSDLRefreshRateDefault(value);
    }
    public void setFullscreen() {
        nativeFullscreen();
        //TODO: sparc doesn't not have vga so we need to
        // see if we can apply similar call to the cg3
        if(LimboApplication.arch == Config.Arch.x86
                || LimboApplication.arch == Config.Arch.x86_64
                || LimboApplication.arch == Config.Arch.arm
                || LimboApplication.arch == Config.Arch.arm64
        ) {
            nativeRefreshScreen(1);
        }
    }
    public void enableAaudio(int value) {
        nativeEnableAaudio(value, Config.aaudioLibName,
                FileUtils.getNativeLibDir(LimboApplication.getInstance())
                        + "/" + Config.aaudioLibName);
    }
    public int get_fd(String path) {
        return FileUtils.get_fd(path);
    }

    /**
     * Fuction is a pass thru from the c close_fd() function called from native code
     * This is similar to the above get_fd but perhaps not needed.
     *
     * @param fd File Descriptor to be closed
     * @return Return value of FileUtils.close_fd()
     */
    public int close_fd(int fd) {
        return FileUtils.close_fd(fd);
    }
    /* @NonNull
    private String getSaveStateName() {
        String machineSaveDirectory = MachineController.getInstance().getMachineSaveDir();
        return machineSaveDirectory + "/" + Config.stateFilename;
    }
    public String saveVM() {

        // Delete any previous state file
        File file = new File(getSaveStateName());
        if (file.exists()) {
            if (!file.delete()) {
                return LimboApplication.getInstance().getString(R.string.CannotDeletePreviousStateFile);
            }
        }

        if (Config.showToast)
            Log.i(TAG, LimboApplication.getInstance().getString(R.string.PleaseWaitSavingVMState));

        int currentFd = get_fd(getSaveStateName());
        String uri = "fd:" + currentFd;
        String command = QmpClient.getMigrateCommand(false, false, uri);
        String msg = QmpClient.sendCommand(command);
        if (msg != null) {
            return processMigrationResponse(msg);
        }
        return null;
    } */
    public void continueVM() {
        String command = QmpClient.getContinueVMCommand();
        QmpClient.sendCommand(command);
    }
    @Nullable
    private String processMigrationResponse(String response) {
        String errorStr = null;
        try {
            JSONObject object = new JSONObject(response);
            errorStr = object.getString("error");
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }
        if (errorStr != null) {
            String descStr = null;

            try {
                JSONObject descObj = new JSONObject(errorStr);
                descStr = descObj.getString("desc");
            } catch (Exception ex) {
                if (Config.debug)
                    ex.printStackTrace();
            }
            return descStr;
        }
        return null;
    }
    public boolean getQMPAllowExternal() {
        return LimboSettingsManager.getEnableExternalQMP(LimboApplication.getInstance());
    }
}

