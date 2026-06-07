package com.max2idea.android.limbo;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.Keep;

import com.max2idea.android.limbo.files.FileInstaller;
import com.max2idea.android.limbo.jni.VMExecutor;
import com.max2idea.android.limbo.main.Config;
import com.max2idea.android.limbo.main.LimboApplication;

import java.io.File;

public class VmUtils {
    public static boolean libLoaded = false;
    private static final String TAG = "VM";
    @Keep public static void init(){
        LimboApplication.initialize();
        setupNativeLibs();
    }

    //XXX: this needs to be called from the main thread otherwise
    //  qemu crashes when it is started later
    public static void setupNativeLibs() {
        if (libLoaded)
            return;
        //Compatibility lib
        System.loadLibrary("compat-limbo");

        //Glib deps
        System.loadLibrary("compat-musl");

        //Glib
        System.loadLibrary("glib-2.0");

        //Pixman for qemu
        System.loadLibrary("pixman-1");

        // SDL library
        if (Config.enable_SDL) {
            if (Build.VERSION.SDK_INT >= 26)
                System.loadLibrary("compat-SDL2-addons");
            System.loadLibrary("SDL2");
        }

        System.loadLibrary("compat-SDL2-ext");

        //Limbo needed for vmexecutor
        System.loadLibrary("limbo");

        // qemu arch specific lib
        loadQEMULib();

        libLoaded = true;
    }
    protected static void loadQEMULib() {
        try {
            System.loadLibrary("qemu-system-i386");
        } catch (Error ex) {
            System.loadLibrary("qemu-system-x86_64");
        }
        try {
            System.loadLibrary("qemu-system-arm");
        } catch (Error ex) {
            System.loadLibrary("qemu-system-aarch64");
        }
    }

    // Main event function
    // Retrives values from saved preferences
    public static void start(Activity activity, String[] params) {
        FileInstaller.installFiles(activity, false);
        VMExecutor.start(params);
    }
    private void createMachineDir(String dir) throws Exception {
        File destDir = new File(dir);
        if (!destDir.exists()) {
            if (!destDir.mkdirs())
                throw new Exception("Could not create internal machine directory");
        }
    }
}
