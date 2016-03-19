/*
 * Copyright (C) 2016 Gernot Hillier <gernot@hillier.de>
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

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class NoiseMonitor extends Service {

    private Handler mHandler = new Handler();

    private static final String LOG_TAG = "NoiseAlert";

    static boolean isRunning;

    /** config state **/
    private int mThreshold = 2;
    private int mHitThreshold = 2;
    private int mHitCount = 0;
    private int mPollInterval = 300;

    /* data source */
    private SoundMeter mSensor;

    private Runnable mPollTask = new Runnable() {
        public void run() {
            double amp = mSensor.getAmplitude();
            Log.d(LOG_TAG, "amp=" + amp);

            if (amp > mThreshold) {
                mHitCount++;
                if (mHitCount > mHitThreshold){
                    Toast.makeText(NoiseMonitor.this.getApplicationContext(),LOG_TAG+" detected noise",Toast.LENGTH_SHORT).show();
                    mHitCount=0;
                }
            }

            mHandler.postDelayed(mPollTask, mPollInterval);
        }
    };

    public NoiseMonitor() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(LOG_TAG,"NoiseMonitor Service onBind called");
        // TODO: Return the communication channel to the service.
        return null;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.i(LOG_TAG,"NoiseMonitor Service created");
        mSensor = new SoundMeter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning=true;
            mSensor.start();
            mThreshold = intent.getIntExtra("com.google.android.noisealert.Threshold", 2);
            mHitThreshold = intent.getIntExtra("com.google.android.noisealert.HitThreshold", 2);
            mPollInterval = intent.getIntExtra("com.google.android.noisealert.PollInterval", 300);
            Log.i(LOG_TAG, "NoiseMonitor Service started, threshold=" + mThreshold + ", hit_threshold=" + mHitThreshold);
            mHandler.postDelayed(mPollTask, mPollInterval);
        } else
            Log.i(LOG_TAG, "NoiseMonitor Service already running, threshold=" + mThreshold + ", hit_threshold=" + mHitThreshold);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG,"NoiseMonitor Service stopped");
        mSensor.stop();
        mHandler.removeCallbacks(mPollTask);
        isRunning=false;
    }


}
