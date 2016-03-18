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

import com.google.android.noisealert.SmsRemote.SmsRemoteReceiver;
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

public class NoiseAlert extends Activity implements SmsRemoteReceiver {
	/* constants */
	private static final String LOG_TAG = "NoiseAlert";
	private static final int POLL_INTERVAL = 300;
	private static final String[] REMOTE_CMDS = {"start", "stop", "panic"};

	/** running state **/
	private boolean mAutoResume = false;
	private boolean mRunning = false;
	private boolean mTestMode = false;

	/** config state **/
	private int mThreshold;
	private String mSmsSecurityCode;
	
	private PowerManager.WakeLock mWakeLock;

	private Handler mHandler = new Handler();

	/* References to view elements */
	private TextView mStatusView;
	private SoundLevelView mDisplay;

	/* data source */
	private SoundMeter mSensor;
	
	/* SMS remote control */
	private SmsRemote mRemote;

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
		mRemote = new SmsRemote();
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "NoiseAlert");
	}

	
	@Override
	public void onResume() {
		super.onResume();
		readApplicationPreferences();
		if (mSmsSecurityCode.length() != 0) {
			mRemote.register(this, mSmsSecurityCode, REMOTE_CMDS);
		}
		mDisplay.setLevel(0, mThreshold);
		if (mAutoResume) {
			start();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		stop();
		mRemote.deregister();
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
		menu.findItem(R.id.test).setEnabled(!mRunning);
		if (mRunning || mTestMode) {
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
			Log.i(LOG_TAG, "settings");
			Intent prefs = new Intent(this, Preferences.class);
			startActivity(prefs);
			break;
		case R.id.start_stop:
			if (!mRunning && !mTestMode) {
				mAutoResume = true;
				mRunning = true;
				mTestMode = false;
				start();
			} else {
				mAutoResume = false;
				mRunning = false;
				stop();
			}
			break;
		case R.id.test:
			if (!mTestMode) {
				mTestMode = true;
				start();
			} else {
				Toast.makeText(this, "Test already running...", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.panic:
			callForHelp();
			break;
		case R.id.help:
			Intent myIntent = new Intent();
			myIntent.setClass(this, HelpActivity.class);
			startActivity(myIntent);
		}
		return true;
	}

	public void receive(String cmd) {
		if (cmd == "start" & !mRunning) {
			mAutoResume = true;
			mRunning = true;
			mTestMode = false;
			start();
		} else if (cmd == "stop" & mRunning) {
			mAutoResume = false;
			mRunning = false;
			stop();
		} else if (cmd == "panic") {
			callForHelp();
		}
	}
	
	private void start() {
		if (!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
		if (!mTestMode) {
			Intent noiseMonitor = new Intent(this, NoiseMonitor.class);
			this.startService(noiseMonitor);
		} else {
			mSensor.start();
			mHandler.postDelayed(mPollTask, POLL_INTERVAL);
		}
	}

	private void stop() {
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		mHandler.removeCallbacks(mPollTask);
		mSensor.stop();
		mDisplay.setLevel(0, 0);
		updateDisplay("stopped...", 0.0);
		mRunning = false;
		mTestMode = false;
		if (!mTestMode) {
			Intent noiseMonitor = new Intent(this, NoiseMonitor.class);
			this.stopService(noiseMonitor);
		}
	}

	private void readApplicationPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mThreshold = Integer.parseInt(prefs.getString("threshold", null));
		Log.i(LOG_TAG, "threshold=" + mThreshold);
		mSmsSecurityCode = prefs.getString("sms_security_code", null);
	}

	private void updateDisplay(String status, double signalEMA) {
		mStatusView.setText(status);

		mDisplay.setLevel((int)signalEMA, mThreshold);
	}

	private void callForHelp() {
		mAutoResume = false;
		stop();
	}

};