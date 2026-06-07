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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.limbo.emu.lib.R;
import com.max2idea.android.limbo.network.NetworkUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Settings Manager for Limbo Emulator
 * Fixed deprecated APIs, null pointers, and crashes
 */
public class LimboSettingsManager extends PreferenceActivity {
    private static final String TAG = "LimboSettingsManager";

    // ====================== DNS ======================
    static String getDNSServer(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getString("dnsServer", Config.defaultDNSServer);
    }

    public static void setDNSServer(@NonNull Context context, String dnsServer) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString("dnsServer", dnsServer).apply();
    }

    // ====================== Screen ======================
    public static int getOrientationSetting(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return Integer.parseInt(prefs.getString("orientationPref", "0"));
    }

    public static boolean getAlwaysShowMenuToolbar(@NonNull Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(activity.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("AlwaysShowMenuToolbar", false);
    }

    public static boolean getFullscreen(@NonNull Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(activity.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("ShowFullscreen", true);
    }

    // ====================== Updates ======================
    public static boolean getPromptUpdateVersion(Context context) {
        if (!Config.enableSoftwareUpdates)
            return false;
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("updateVersionPrompt", Config.defaultCheckNewVersion);
    }

    public static void setPromptUpdateVersion(@NonNull Context context, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("updateVersionPrompt", value).apply();
    }

    // ====================== Advanced ======================
    public static boolean getPrio(@NonNull Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(activity.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("HighPrio", false);
    }

    // ====================== Files ======================
    public static boolean getEnableLegacyFileManager(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("EnableLegacyFileManager", false);
    }

    public static String getLastDir(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getString("lastDir", null);
    }

    public static void setLastDir(@NonNull Context context, String imagesPath) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString("lastDir", imagesPath).apply();
    }

    public static String getImagesDir(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getString("imagesDir", null);
    }

    public static void setImagesDir(@NonNull Context context, String imagesPath) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString("imagesDir", imagesPath).apply();
    }

    public static String getExportDir(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getString("exportDir", null);
    }

    public static void setExportDir(@NonNull Context context, String imagesPath) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString("exportDir", imagesPath).apply();
    }

    public static String getSharedDir(@NonNull Context context) {
        String lastDir = Environment.getExternalStorageDirectory().getPath();
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getString("sharedDir", lastDir);
    }

    public static void setSharedDir(@NonNull Context context, String lastDir) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString("sharedDir", lastDir).apply();
    }

    // ====================== Exit Code ======================
    public static int getExitCode(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getInt("exitCode", Config.EXIT_SUCCESS);
    }

    @SuppressLint("ApplySharedPref")
    public static void setExitCode(@NonNull Context context, int exitCode) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putInt("exitCode", exitCode).commit();
    }

    // ====================== First Launch ======================
    public static boolean isFirstLaunch(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("firstTime" + LimboApplication.getLimboVersionString(), true);
    }

    public static void setFirstLaunch(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("firstTime" + LimboApplication.getLimboVersionString(), false).apply();
    }

    // ====================== Key Mapper ======================
    @NonNull
    public static Set<String> getKeyMappers(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet("keyMappers", new HashSet<>()));
    }

    public static void setKeyMappers(@NonNull Context context, Set<String> keyMappers) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putStringSet("keyMappers", keyMappers).apply();
    }

    public static int getKeyMapperSize(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String sizeStr = prefs.getString("keyMapperSize", "3");
        return Integer.parseInt(sizeStr);
    }

    // ====================== VNC ======================
    public static boolean getVNCEnablePassword(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("enableVNCPassword", false);
    }

    public static String getVNCPass(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getString("vncPass", "");
    }

    public static void setVNCPass(@NonNull Context context, @Nullable String value) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString("vncPass", value).apply();
    }

    public static boolean getEnableExternalVNC(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("enableExternalVNC", false);
    }

    // ====================== QMP ======================
    public static boolean getEnableQmp(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("enableQMP", true);
    }

    public static boolean getEnableExternalQMP(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("enableExternalQMP", false);
    }

    // ====================== UI ======================
    public static boolean getImmersiveMode(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("immersiveMode", false);
    }

    public static int getKeyPressDelay(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String sizeStr = prefs.getString("keyPressDelay", "100");
        return Integer.parseInt(sizeStr);
    }

    public static int getMouseButtonDelay(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String sizeStr = prefs.getString("mouseButtonDelay", "100");
        return Integer.parseInt(sizeStr);
    }

    public static boolean getEnableAaudio(Context activity) {
        if (Build.VERSION.SDK_INT < 26) return false;
        SharedPreferences prefs = activity.getSharedPreferences(activity.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("enableAaudio", false);
    }

    public static String getDiskCache(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getString("diskCachePref", context.getString(R.string.Default));
    }

    public static boolean getPreventMouseOutOfBounds(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("preventMouseOutOfBounds", false);
    }

    public static boolean getIgnoreBreakpointInvalidation(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean("ignoreBreakpointInvalidation", false);
    }

    // ====================== Activity ======================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Config.SETTINGS_RETURN_CODE, new Intent());
        addPrefs();
        initListeners();
    }

    private void addPrefs() {
        addPreferencesFromResource(R.xml.settings);
        if (Config.enableSoftwareUpdates) {
            addPreferencesFromResource(R.xml.software_updates);
        }
        if (Config.enableImmersiveMode) {
            addPreferencesFromResource(R.xml.immersive);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            addPreferencesFromResource(R.xml.aaudio);
        }
    }

    private void initListeners() {
        try {
            // VNC Password change listener
            getPreferenceManager().findPreference("enableVNCPassword")
                    .setOnPreferenceChangeListener((preference, newValue) -> {
                        if (Boolean.TRUE.equals(newValue)) {
                            promptVNCPass(LimboSettingsManager.this);
                        }
                        return true;
                    });

            // VNC Password click listener
            getPreferenceManager().findPreference("vncPass")
                    .setOnPreferenceClickListener(preference -> {
                        promptVNCPass(LimboSettingsManager.this);
                        return true;
                    });
        } catch (Exception e) {
            Log.e(TAG, "Preference listener error: " + e.getMessage());
        }
    }

    public void promptVNCPass(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.VNCPassword));

        // Info Text
        TextView textView = new TextView(activity);
        textView.setPadding(40, 40, 40, 40);
        textView.setText(getString(R.string.vncServer) + ": " + NetworkUtils.getVNCAddress(activity)
                + ":" + Config.defaultVNCPort + "\n" + getString(R.string.externalVNCWarning));

        // Password EditText
        final EditText passwdView = new EditText(activity);
        passwdView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        passwdView.setHint(R.string.Password);
        passwdView.setSingleLine(true);
        passwdView.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // Layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(textView);
        layout.addView(passwdView);

        builder.setView(layout);

        // Buttons
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String pass = passwdView.getText().toString().trim();
            if (!pass.isEmpty()) {
                setVNCPass(activity, pass);
            } else {
                Log.e(TAG, getString(R.string.passwordCannotBeEmpty));
                setVNCPass(activity, null);
            }
        });

        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> setVNCPass(activity, null));
        builder.setOnCancelListener(dialog -> setVNCPass(activity, null));

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // Show/Hide Password Button
        try {
            Button viewButton = new Button(activity);
            viewButton.setBackgroundResource(android.R.drawable.ic_menu_view);
            viewButton.setOnClickListener(v -> {
                boolean showing = (boolean) passwdView.getTag();
                if (showing) {
                    passwdView.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    passwdView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
                passwdView.setTag(!showing);
                passwdView.setSelection(passwdView.getText().length());
            });

            alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.GONE);
        } catch (Exception ignored) {}
    }
}