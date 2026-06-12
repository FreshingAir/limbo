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
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.limbo.emu.lib.R;
import org.libsdl.app.SDLActivity;
import org.libsdl.app.SDLAudioManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Our overloaded SDLActivity with compatibility reroutes for the mouse and other extra functionality
 * for better usability. In general SDL is slower than VNC but offers Audio support for QEMU.
 */
public class LimboSDLActivity extends SDLActivity {
    private static final String TAG = "LimboSDLActivity";
    public static MouseMode mouseMode = MouseMode.Trackpad;
    private final ExecutorService mouseEventsExecutor = Executors.newFixedThreadPool(1);
    private final ExecutorService keyEventsExecutor = Executors.newFixedThreadPool(1);
    public AudioManager am;
    protected int maxVolume;
    // store state
    private AudioTrack audioTrack;
    private AudioRecord mAudioRecord;
    private boolean quit = false;
    private View mGap;
    private boolean resettingLayout;

    // ===================== 新增：加载Native库 =====================
    static {
        try {
            System.loadLibrary("compat-limbo");
            Log.d(TAG, "load libcompat-limbo.so success");
        } catch (Throwable e) {
            Log.e(TAG, "load libcompat-limbo.so failed", e);
        }
    }

    // ===================== 新增：Native方法声明 =====================
    public native void initSdlResJniCache();
    public native void releaseSdlResJniCache();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        // 新增：释放JNI全局引用
        releaseSdlResJniCache();

        mNextNativeState = NativeState.PAUSED;
        mIsResumedCalled = false;
        if (SDLActivity.mBrokenLibraries) {
            return;
        }
        SDLActivity.handleNativeState();
        SDLActivity.mSuspendOnly = true;
        quit = true;
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.sdlactivitymenu, menu);
        int maxMenuItemsShown = 4;
        int actionShow = MenuItem.SHOW_AS_ACTION_IF_ROOM;
        // Remove Monitor console for SDL2 it creates 2 SDL windows and SDL for
        // android supports only 1
        menu.removeItem(menu.findItem(R.id.itemMonitor).getItemId());
        // Remove scaling for now
        menu.removeItem(menu.findItem(R.id.itemScaling).getItemId());
        // Remove external mouse for now
        menu.removeItem(menu.findItem(R.id.itemExternalMouse).getItemId());
        menu.removeItem(menu.findItem(R.id.itemCtrlAltDel).getItemId());
        menu.removeItem(menu.findItem(R.id.itemCtrlC).getItemId());
        for (int i = 0; i < menu.size() && i < maxMenuItemsShown; i++) {
            menu.getItem(i).setShowAsAction(actionShow);
        }
        return true;
    }

    // FIXME: We need this to able to catch complex characters strings like
    // grave and send it as text
    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
            sendText(event.getCharacters());
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            onBackPressed();
            return true;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // We emulate right click with volume down
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            // We emulate middle click with volume up
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    private void sendText(@NonNull String string) {
        KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        KeyEvent[] keyEvents = keyCharacterMap.getEvents(string.toCharArray());
        if (keyEvents == null)
            return;
        for (KeyEvent keyEvent : keyEvents) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                sendKeyEvent(keyEvent.getKeyCode(), true);
            } else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                sendKeyEvent(keyEvent.getKeyCode(), false, 10);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 新增：主线程初始化JNI缓存（必须最先执行）
        initSdlResJniCache();

        setupScreen();
        saveAudioState();
        super.onCreate(savedInstanceState);
        mSingleton = this;
        restoreAudioState();
        setupWidgets();
        setupUserInterface();
        setupAudio();
    }

    private void setupUserInterface() {
        Config.keyDelay = LimboSettingsManager.getKeyPressDelay(this);
        Config.mouseButtonDelay = LimboSettingsManager.getMouseButtonDelay(this);
    }

    private void setupScreen() {
        if (LimboSettingsManager.getFullscreen(this)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if(pm.isSustainedPerformanceModeSupported())
                getWindow().setSustainedPerformanceMode(true);
        }
    }

    private void saveAudioState() {
        audioTrack = SDLAudioManager.mAudioTrack;
        mAudioRecord = SDLAudioManager.mAudioRecord;
    }

    private void restoreAudioState() {
        SDLAudioManager.mAudioTrack = audioTrack;
        SDLAudioManager.mAudioRecord = mAudioRecord;
    }

    private void setupWidgets() {
        mSurface = new LimboSDLSurface(this, this);
        setContentView(R.layout.limbo_sdl);
        mLayout = findViewById(R.id.sdl_layout);
        RelativeLayout mLayout = findViewById(R.id.sdl);
        mLayout.addView(mSurface);
        mGap = findViewById(R.id.gap);
        updateLayout(getResources().getConfiguration().orientation);
    }

    public void loadLibraries() {
        //XXX: Do not remove we need this to prevent loading libraries from SDL so
        // we handle libraries for specific architectures later
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        processTrackPadEvents(event);
        return true;
    }

    /**
     * For Virtual Trackpad we need relative coordinates so we capture the events from the
     * activity since we want to use the whole area for touch gestures therefore this should be
     * called from within the activity onTouchEvent callbacks
     *
     * @param event MotionEvent to be processed
     */
    public void processTrackPadEvents(MotionEvent event) {
        if (mouseMode == MouseMode.TOUCHSCREEN)
            return;
        ((LimboSDLSurface) mSurface).onTouchProcess(mSurface, event);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        invalidateOptionsMenu();
        updateLayout(newConfig.orientation);
    }

    public void updateLayout(int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
            mGap.setVisibility(View.VISIBLE);
        else
            mGap.setVisibility(View.GONE);
    }

    //    private static Thread limboSDLThread = null;
    @Override
    protected synchronized void runSDLMain() {
        //XXX: we hold the thread because SDLActivity will exit
        while (!quit) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "SDLThread exited");
    }

    protected void setupAudio() {
        if (am == null) {
            am = (AudioManager) mSingleton.getSystemService(Context.AUDIO_SERVICE);
            maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
    }

    //XXX: We want to suspend only when app is calling onPause()
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!processKey(keyCode, event))
            return super.onKeyDown(keyCode, event);
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!processKey(keyCode, event))
            return super.onKeyUp(keyCode, event);
        return true;
    }

    public boolean processKey(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) || (keyCode == KeyEvent.KEYCODE_FORWARD)) {
            // dismiss android back and forward keys
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            return false;
        } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
            sendKey(keyCode, event.getAction(), true);
            return true;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            sendKey(keyCode, event.getAction(), false);
            return true;
        } else {
            return false;
        }
    }

    private synchronized void sendKey(final int keyCode, final int action,
                                      final boolean down) {
        if (!handleMissingKeys(keyCode, action)) {
            sendKeyEvent(keyCode, down);
        }
    }

    // Handles key codes missing in sdl2-keymap.h
    // This function will create them with a Shift Modifier
    private boolean handleMissingKeys(int keyCode, int action) {
        int newKeyCode;
        switch (keyCode) {
            case 77:
                newKeyCode = 9;
                break;
            case 81:
                newKeyCode = 70;
                break;
            case 17:
                newKeyCode = 15;
                break;
            case 18:
                newKeyCode = 10;
                break;
            default:
                return false;
        }
        if (action == KeyEvent.ACTION_DOWN) {
            sendKeyEvent(59, true);
            sendKeyEvent(newKeyCode, true);
        } else {
            sendKeyEvent(59, false);
            sendKeyEvent(newKeyCode, false);
        }
        return true;
    }

    private void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ===================== 新增：JNI 回调方法（分辨率变更） =====================
    /**
     * 供C层JNI回调：虚拟机分辨率变化
     */
    public static void onVMResolutionChanged(int width, int height) {
        Log.d("ResolutionCallback", "VM Resolution: " + width + " x " + height);
        if (mSurface instanceof LimboSDLSurface) {
            LimboSDLSurface sdlSurf = ((LimboSDLSurface) mSurface);
            // 传递虚拟机原生分辨率给渲染层
            sdlSurf.setVmNativeSize(width, height);
            // 执行带居中缩放的刷新
            sdlSurf.refreshSurfaceView();
        }
    }

    /**
     * We treat as relative mode only events with TOOL_TYPE_FINGER as long as the user has not
     * selected to emulate a touch screen.
     *
     * @param toolType Event Tool type
     * @return True if the device will be expected as relative mode by the emulator
     */
    public boolean isRelativeMode(int toolType) {
        return toolType == MotionEvent.TOOL_TYPE_FINGER
                && LimboSDLActivity.mouseMode != MouseMode.TOUCHSCREEN;
    }

    protected void sendMouseEvent(int action, int toolType, float x, float y) {
        //HACK: we generate an artificial delay since the qemu main event loop
        // is probably not able to process them if the timestamps are too close together?
        sendMouseEvent(toolType, x, y, action == MotionEvent.ACTION_UP ? Config.mouseButtonDelay : 0);
    }

    private void sendMouseEvent(final int toolType,
                                final float x, final float y, final long delayMs) {
        if(resettingLayout)
            return;
        mouseEventsExecutor.submit(() -> {
            boolean relative = isRelativeMode(toolType);
            if (delayMs > 0 && toolType != MotionEvent.TOOL_TYPE_MOUSE)
                delay(delayMs);
            //XXX: for mouse events we use our jni compatibility extensions instead of the sdl native functions
            // SDLActivity.onSDLNativeMouse(button, action, x, y);
            LimboSDLSurface.MouseState mouseState = ((LimboSDLSurface) mSurface).mouseState;
//                Log.d(TAG, "sendMouseEvent button: " + button + ", action: " + action
//                        + ", relative: " + relative + ", nx = " + nx + ", ny = " + ny
//                        + ", delay = " + delayMs);
            if (delayMs > 0 && toolType != MotionEvent.TOOL_TYPE_MOUSE)
                delay(delayMs);
        });
    }

    protected void sendKeyEvent(int keycode, boolean down) {
        //HACK: we generate an artificial delay since the qemu main event loop
        // is probably not able to process them if the timestamps are too close together?
        sendKeyEvent(keycode, down, !down ? Config.keyDelay : 0);
    }

    private void sendKeyEvent(final int keycode, final boolean down, final long delayMs) {
        keyEventsExecutor.submit(() -> {
            if (delayMs > 0)
                delay(delayMs);
//                Log.d(TAG, "sendKeyEvent: " + ", keycode = " + keycode + ", down = " + down
//                + ", delay = " + delayMs);
            if (down)
                SDLActivity.onNativeKeyDown(keycode);
            else {
                SDLActivity.onNativeKeyUp(keycode);
            }
        });
    }

    public synchronized void setFullscreen() {
        resettingLayout = true;
        try {
            Log.d(TAG, "Requesting fullscreen");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        resettingLayout = false;
    }

    public enum MouseMode {
        Trackpad, TOUCHSCREEN
    }
}