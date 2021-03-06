package com.skad.android.androidm2ihm.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.skad.android.androidm2ihm.R;

import java.util.Observable;

/**
 * Created by pschmitt on 1/6/14.
 */
public class Score extends Observable {

    private int mWallCollisions;
    private int mBulletCollisions;
    private int mLevelId;

    /**
     * Default constructor
     *
     * @param levelId The id of the current level
     */
    public Score(int levelId) {
        mWallCollisions = 0;
        mBulletCollisions = 0;
        mLevelId = levelId;
    }

    /**
     * Call this method to decrement the score when a player hit a wall
     */
    public void collided(Level.EVENT event) {
        if (event == Level.EVENT.COLLISION_BULLET) {
            mBulletCollisions++;
        } else if (event == Level.EVENT.COLLISION_WALL) {
            mWallCollisions++;
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Reset the score
     */
    public void reset() {
        mWallCollisions = 0;
        mBulletCollisions = 0;
        setChanged();
        notifyObservers();
    }

    /**
     * Call this when a player completed a level and chooses to advance to the next one
     */
    public void nextLevel() {
        mLevelId++;
        reset();
    }

    /**
     * Set the level number (id)
     * @param levelNumber The new level number
     */
    public void setLevel(int levelNumber) {
        mLevelId = levelNumber;
    }

    /**
     * Compute the actual score, based on the number of collisions and the level number
     * @return The current score
     */
    public int getTotalScore() {
        int score = (1000 * mLevelId) - (mWallCollisions * 2) - mBulletCollisions;
        return score > 0 ? score : 0;
    }

    /**
     * Retrieve the highest score for a given level
     * @param context Current context
     * @param levelId The number of the level
     * @return The highest score for the corresponding level
     */
    public int getHighScore(Context context, int levelId) {
        mLevelId = levelId;
        return getHighScore(context);
    }

    /**
     * Retrieve the highest score for the current level
     * @param context Current context
     * @return The highscore
     */
    public int getHighScore(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getInt(String.format(context.getString(R.string.pref_key_highscore), mLevelId), 0);
    }

    /**
     * Set the number of level
     * @param context
     * @param nb
     */
    public static void setNbLevel(Context context, int nb) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.edit().putInt(context.getString(R.string.pref_key_max_lvl), nb).commit();
    }

    /**
     * Return the number of level
     *
     * @param context
     * @return the number of level
     */
    public static int getNbLevel(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getInt(context.getString(R.string.pref_key_max_lvl), 0);
    }

    /**
     * Save the current score to Preferences. Should be called when a player beats its highscore
     * @param context The current context
     */
    public void saveHighScore(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.edit().putInt(String.format(context.getString(R.string.pref_key_highscore), mLevelId), getTotalScore()).commit();
    }

    /**
     * Reset all highscores
     * @param context The current context
     */
    public static void resetHighScores(Context context) {
        for (int i = 1; i < getNbLevel(context); i++) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPref.edit().remove(String.format(context.getString(R.string.pref_key_highscore), i)).commit();
        }
    }
}
