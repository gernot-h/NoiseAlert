package com.google.android.noisealert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NoiseAlertStartup extends BroadcastReceiver {
    public NoiseAlertStartup() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent noiseMonitor = new Intent(context, NoiseMonitor.class);
        context.startService(noiseMonitor);
    }
}
