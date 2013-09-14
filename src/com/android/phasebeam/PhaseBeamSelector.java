/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phasebeam;

import android.app.Activity;
import android.app.WallpaperManager;
import android.app.WallpaperInfo;
import android.app.Dialog;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperService;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.WallpaperSettingsActivity;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.Context;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.view.Window;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

public class PhaseBeamSelector extends Activity implements
        CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    private static final String LOG_TAG = "PhaseBeamSelector";

    public static final String KEY_PREFS = "phasebeam";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_HUE = "hue";
    public static final String KEY_SATURATION = "saturation";
    public static final String KEY_BRIGHTNESS = "brightness";

    private static final float HUE_RANGE_MIN = 0.0f;
    private static final float HUE_RANGE_MAX = 1.0f;
    private static final float SATURATION_RANGE_MIN = 0.0f;
    private static final float SATURATION_RANGE_MAX = 1.0f;
    private static final float BRIGHTNESS_RANGE_MIN = 0.5f;
    private static final float BRIGHTNESS_RANGE_MAX = 1.5f;

    private WallpaperManager mWallpaperManager;
    private WallpaperConnection mWallpaperConnection;
    private Intent mWallpaperIntent;

    private SharedPreferences mSharedPref;

    private CheckBox mEnableBox;
    private SeekBar mColorSeekBar;
    private SeekBar mSaturationSeekBar;
    private SeekBar mBrightnessSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.selector);

        mWallpaperIntent = new Intent(this, PhaseBeamWallpaper.class);
        mWallpaperManager = WallpaperManager.getInstance(this);
        mWallpaperConnection = new WallpaperConnection(mWallpaperIntent);

        mSharedPref = getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE);

        mEnableBox = (CheckBox) findViewById(R.id.recolor);
        mEnableBox.setOnCheckedChangeListener(this);
        mColorSeekBar = (SeekBar) findViewById(R.id.hue);
        mColorSeekBar.setOnSeekBarChangeListener(this);
        mSaturationSeekBar = (SeekBar) findViewById(R.id.saturation);
        mSaturationSeekBar.setOnSeekBarChangeListener(this);
        mBrightnessSeekBar = (SeekBar) findViewById(R.id.brightness);
        mBrightnessSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWallpaperConnection != null && mWallpaperConnection.mEngine != null) {
            try {
                mWallpaperConnection.mEngine.setVisibility(true);
            } catch (RemoteException e) {
                // Ignore
            }
        }
        updateUiFromPrefs();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWallpaperConnection != null && mWallpaperConnection.mEngine != null) {
            try {
                mWallpaperConnection.mEngine.setVisibility(false);
            } catch (RemoteException e) {
                // Ignore
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mWallpaperConnection.connect()) {
            mWallpaperConnection = null;
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mWallpaperConnection != null) {
            mWallpaperConnection.disconnect();
        }
        mWallpaperConnection = null;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mColorSeekBar.setEnabled(isChecked);
        mSaturationSeekBar.setEnabled(isChecked);
        mBrightnessSeekBar.setEnabled(isChecked);
        mSharedPref.edit().putBoolean(KEY_ENABLED, isChecked).apply();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }

        float min, max;
        String key;

        if (seekBar == mColorSeekBar) {
            min = HUE_RANGE_MIN;
            max = HUE_RANGE_MAX;
            key = KEY_HUE;
        } else if (seekBar == mSaturationSeekBar) {
            min = SATURATION_RANGE_MIN;
            max = SATURATION_RANGE_MAX;
            key = KEY_SATURATION;
        } else {
            min = BRIGHTNESS_RANGE_MIN;
            max = BRIGHTNESS_RANGE_MAX;
            key = KEY_BRIGHTNESS;
        }

        float value = ((max - min) * progress / seekBar.getMax()) + min;
        mSharedPref.edit().putFloat(key, value).apply();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Nothing to do here
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Nothing to do here
    }

    private void updateUiFromPrefs() {
        mEnableBox.setChecked(mSharedPref.getBoolean(KEY_ENABLED, false));

        updateSeekBarFromFloat(mColorSeekBar, mSharedPref.getFloat(KEY_HUE, 0.0f),
                HUE_RANGE_MIN, HUE_RANGE_MAX);
        updateSeekBarFromFloat(mSaturationSeekBar, mSharedPref.getFloat(KEY_SATURATION, 1.0f),
                SATURATION_RANGE_MIN, SATURATION_RANGE_MAX);
        updateSeekBarFromFloat(mBrightnessSeekBar, mSharedPref.getFloat(KEY_BRIGHTNESS, 1.0f),
                BRIGHTNESS_RANGE_MIN, BRIGHTNESS_RANGE_MAX);
    }

    private void updateSeekBarFromFloat(SeekBar seekBar, float value, float min, float max) {
        float progress = (value - min) * seekBar.getMax() / (max - min);
        seekBar.setProgress((int) progress);
    }

    class WallpaperConnection extends IWallpaperConnection.Stub implements ServiceConnection {
        final Intent mIntent;
        IWallpaperService mService;
        IWallpaperEngine mEngine;
        boolean mConnected;

        WallpaperConnection(Intent intent) {
            mIntent = intent;
        }

        public boolean connect() {
            synchronized (this) {
                if (!bindService(mIntent, this, Context.BIND_AUTO_CREATE)) {
                    return false;
                }

                mConnected = true;
                return true;
            }
        }

        public void disconnect() {
            synchronized (this) {
                mConnected = false;
                if (mEngine != null) {
                    try {
                        mEngine.destroy();
                    } catch (RemoteException e) {
                        // Ignore
                    }
                    mEngine = null;
                }
                unbindService(this);
                mService = null;
            }
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (mWallpaperConnection == this) {
                mService = IWallpaperService.Stub.asInterface(service);
                try {
                    final View view = findViewById(R.id.backgroundview);
                    final View root = view.getRootView();
                    mService.attach(this, view.getWindowToken(),
                            WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA_OVERLAY,
                            true, root.getWidth(), root.getHeight());
                } catch (RemoteException e) {
                    Log.w(LOG_TAG, "Failed attaching wallpaper; clearing", e);
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mEngine = null;
            if (mWallpaperConnection == this) {
                Log.w(LOG_TAG, "Wallpaper service gone: " + name);
            }
        }

        public void attachEngine(IWallpaperEngine engine) {
            synchronized (this) {
                if (mConnected) {
                    mEngine = engine;
                    try {
                        engine.setVisibility(true);
                    } catch (RemoteException e) {
                        // Ignore
                    }
                } else {
                    try {
                        engine.destroy();
                    } catch (RemoteException e) {
                        // Ignore
                    }
                }
            }
        }

        public ParcelFileDescriptor setWallpaper(String name) {
            return null;
        }

        public void engineShown(IWallpaperEngine engine) throws RemoteException {
        }
    }
}
