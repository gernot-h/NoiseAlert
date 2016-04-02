/*
 * Copyright (C) 2008 Google Inc.
 *
 * Heavily modified 2016 by Gernot Hillier <gernot@hillier.de>
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

package com.google.android.noisealert;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.widget.Toast;

public class NoiseAlert extends Activity {
	/* constants */
	private static final String LOG_TAG = "NoiseAlert";
	private static final int POLL_INTERVAL = 300;

	/** running state **/
	private boolean mTestMode = false;

	/** config state **/
	private int mThreshold;
	private int mHitThreshold;

	private PowerManager.WakeLock mWakeLock;

	private Handler mHandler = new Handler();

	/* References to view elements */
	private TextView mStatusView;
	private SoundLevelView mDisplay;

	/* data source */
	private SoundMeter mSensor;
	
	private Runnable mPollTask = new Runnable() {
		public void run() {
			double amp = mSensor.getAmplitude();
			updateDisplay("testing...", amp);

			mHandler.postDelayed(mPollTask, POLL_INTERVAL);
		}
	};
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		setContentView(R.layout.main);
		mStatusView = (TextView) findViewById(R.id.status);

		mSensor = new SoundMeter();
		mDisplay = (SoundLevelView) findViewById(R.id.volume);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, LOG_TAG);
	}

	
	@Override
	public void onResume() {
		super.onResume();
		readApplicationPreferences();
		mDisplay.setLevel(0, mThreshold);
		if (NoiseMonitor.isRunning) {
			updateDisplay("Service running...", 0.0);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		stopTest();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.test).setVisible(!NoiseMonitor.isRunning);
		menu.findItem(R.id.test).setEnabled(!NoiseMonitor.isRunning);
		if (NoiseMonitor.isRunning || mTestMode) {
			menu.findItem(R.id.start_stop).setTitle(R.string.stop);
		} else {
			menu.findItem(R.id.start_stop).setTitle(R.string.start);
		}
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.settings:
			Intent prefs = new Intent(this, Preferences.class);
			startActivity(prefs);
			break;
		case R.id.start_stop:
			if (!NoiseMonitor.isRunning && !mTestMode) {
				startService();
			} else {
				if(NoiseMonitor.isRunning)
					stopService();
				else
					stopTest();
			}
			break;
		case R.id.test:
			startTest();
			break;
		case R.id.help:
			Intent myIntent = new Intent();
			myIntent.setClass(this, HelpActivity.class);
			startActivity(myIntent);
		}
		return true;
	}

	private void startTest() {
		if (mTestMode) {
			Toast.makeText(this, "Test already running...", Toast.LENGTH_SHORT).show();
			return;
		}
		if (!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
		mTestMode = true;
		mSensor.start();
		mHandler.postDelayed(mPollTask, POLL_INTERVAL);
	}

	private void startService() {
		Intent noiseMonitor = new Intent(this, NoiseMonitor.class);
		noiseMonitor.putExtra("com.google.android.noisealert.Threshold",mThreshold);
		noiseMonitor.putExtra("com.google.android.noisealert.HitThreshold", mHitThreshold);
		this.startService(noiseMonitor);
		updateDisplay("Service running...", 0.0);
	}

	private void stopTest() {
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		if (!mTestMode)
			return;
		mHandler.removeCallbacks(mPollTask);
		mSensor.stop();
		mDisplay.setLevel(0, 0);
		updateDisplay("stopped...", 0.0);
		mTestMode = false;
	}

	private void stopService() {
		Intent noiseMonitor = new Intent(this, NoiseMonitor.class);
		this.stopService(noiseMonitor);
		updateDisplay("stopped...", 0.0);
	}

	private void readApplicationPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mThreshold = Integer.parseInt(prefs.getString("threshold", null));
		Log.i(LOG_TAG, "threshold=" + mThreshold);
		mHitThreshold = Integer.parseInt(prefs.getString("hit_threshold", null));
		Log.i(LOG_TAG, "hit_threshold=" + mHitThreshold);
	}

	private void updateDisplay(String status, double signalEMA) {
		mStatusView.setText(status);
		mDisplay.setLevel((int) signalEMA, mThreshold);
	}

}