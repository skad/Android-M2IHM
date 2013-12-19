package com.skad.android.androidm2ihm.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.skad.android.androidm2ihm.R;
import com.skad.android.androidm2ihm.view.Level;

/**
 * Created by pschmitt on 12/19/13.
 */
public class LevelActivity extends Activity   implements SensorEventListener {
    private static final String TAG = "LevelActivity";

    private Level mLevel;
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLevel = new Level(this);
        setContentView(mLevel);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

        // Action bar
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Carefull! This ouputs a lot of data as it gets called reaaaaally ofte
        mLevel.setForceX(-event.values[0]);
        mLevel.setForceY(event.values[1]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        //overridePendingTransition(android.R.anim.fade_out, R.anim.push_right_in);
    }
}