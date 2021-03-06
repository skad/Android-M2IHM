package com.skad.android.androidm2ihm.thread;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;
import com.skad.android.androidm2ihm.R;
import com.skad.android.androidm2ihm.model.Level;
import com.skad.android.androidm2ihm.view.LevelView;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by pschmitt on 1/12/14.
 */
public class GameThread extends Thread implements SensorEventListener, Observer {
    private static final String TAG = "GameThread";
    // Minimum movement value
    private static final double MOVEMENT_THRESHOLD = 0.3;
    private boolean mRunning = true;
    private Level mLevel;
    private SurfaceHolder mSurfaceHolder;
    private LevelView mLevelView;
    private Context mContext;
    private Canvas mcanvas;
    private SensorManager mSensorManager;

    public GameThread(SurfaceHolder surfaceHolder, Context context, LevelView levelView) {
        mSurfaceHolder = surfaceHolder;
        mContext = context;
        mRunning = false;
        mLevelView = levelView;
    }

    public void setRunning(boolean running) {
        mRunning = running;
    }

    @Override
    public void run() {
        mLevel = Level.getInstance();
        mLevel.addObserver(this);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean bounce = sharedPreferences.getBoolean(mContext.getString(R.string.pref_key_bounce_off_walls), false);
        synchronized (this) {
            while (mRunning) {
                mLevel.update(bounce);
                mcanvas = mSurfaceHolder.lockCanvas();
                if (mcanvas != null) {
                    mLevelView.drawGameElements(mcanvas);
                    mSurfaceHolder.unlockCanvasAndPost(mcanvas);
                }
            }
        }
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float xValue = event.values[1];
        float yValue = event.values[0];
        // Ensure x|y values aren't too low in order to prevent micro movement
        if (Math.abs(xValue) < MOVEMENT_THRESHOLD) {
            xValue = 0;
        }
        if (Math.abs(yValue) < MOVEMENT_THRESHOLD) {
            yValue = 0;
        }
        mLevel.updatePlayerPosition(xValue, yValue);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO ?
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof Level && data instanceof Level.EVENT) {
            Level.EVENT event = (Level.EVENT) data;
            switch (event) {
                case COLLISION_BULLET:
                    break;
                case COLLISION_WALL:
                    break;
                case GAME_OVER:
                case GAME_SUCCESS:
                    // Stop
                    mRunning = false;
                    mSensorManager.unregisterListener(this);
                    interrupt();
                    break;
            }
        }
    }
}
