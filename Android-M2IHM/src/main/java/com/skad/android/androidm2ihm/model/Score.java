package com.skad.android.androidm2ihm.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.skad.android.androidm2ihm.R;

/**
 * Created by pschmitt on 1/6/14.
 */
public class Score {

    private int mCollisions;
    private int mLevelId;

    public Score(int levelId) {
        mCollisions = 0;
        mLevelId = levelId;
    }

    public void collided() {
        mCollisions++;
    }

    public int getTotalScore() {
        int score = (100 * mLevelId) - mCollisions;
        return score > 0 ? score : 0;
    }

    public int getHighScore(Context context, int levelId) {
        mLevelId = levelId;
        return getHighScore(context);
    }

    public int getHighScore(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getInt(String.format(context.getString(R.string.pref_key_highscore), mLevelId), 0);
    }

    public void saveHighScore(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.edit().putInt(String.format(context.getString(R.string.pref_key_highscore), mLevelId), getTotalScore()).commit();
    }

    public static void resetHighScores(Context context) {
        for (int i = 1; i < 4; i++) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPref.edit().remove(String.format(context.getString(R.string.pref_key_highscore), i)).commit();
        }
    }
}