package com.google.android.noisealert;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class NoiseMonitor extends Service {

    private Handler mHandler = new Handler();

    /** config state **/
    private int mThreshold = 2;
    private int mHitCount =0;

    /* data source */
    private SoundMeter mSensor;

    private Runnable mPollTask = new Runnable() {
        public void run() {
            double amp = mSensor.getAmplitude();
            Log.i("noiseMonitor", "amp=" + amp);

            if (amp > mThreshold) {
                mHitCount++;
                if (mHitCount > 1){
                    Toast.makeText(NoiseMonitor.this.getApplicationContext(),"laut",Toast.LENGTH_SHORT).show();
                    mHitCount=0;
                }
            }

            mHandler.postDelayed(mPollTask, 300);
        }
    };

    public NoiseMonitor() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        mSensor = new SoundMeter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSensor.start();
        mHandler.postDelayed(mPollTask, 300);
        Toast.makeText(NoiseMonitor.this.getApplicationContext(),"gestartet",Toast.LENGTH_SHORT).show();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(NoiseMonitor.this.getApplicationContext(),"destroy",Toast.LENGTH_SHORT).show();
        mSensor.stop();
    }


}
