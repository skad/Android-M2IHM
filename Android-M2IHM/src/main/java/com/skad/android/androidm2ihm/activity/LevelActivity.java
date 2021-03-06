package com.skad.android.androidm2ihm.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
import com.skad.android.androidm2ihm.R;
import com.skad.android.androidm2ihm.model.Level;
import com.skad.android.androidm2ihm.model.Score;
import com.skad.android.androidm2ihm.utils.FileUtils;
import com.skad.android.androidm2ihm.utils.LevelParser;
import com.skad.android.androidm2ihm.view.LevelView;

import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by pschmitt on 12/19/13.
 */
public class LevelActivity extends ActionBarActivity implements/* SensorEventListener,*/ DialogInterface.OnClickListener, DialogInterface.OnCancelListener, Observer/*, GameTask.OnGameEventListener*//*, LevelView.OnGameEventListener*/ {
    private static final String TAG = "LevelActivity";
    private final Handler mHandler = new Handler();
    // Views
    private TextView mScoreView;
    private LevelView mLevelView;
    private boolean mPlayerFailed = false;
    private Score mScore;
    private int mLevelNumber;
    private String mLevelDir;
    private Level mLevel;
    // Audio
    private boolean mMute;
    private MediaPlayer mBackgroundMusic;
    private SoundPool mSoundPool;
    private int mIdSoundWall;
    private int mIdSoundGameOver;
    private int mIdSoundWin;
    // Whether this class is observing mScore and mLevel
    private boolean mObserving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level);

        // Determine which level should be loaded
        mLevelNumber = getIntent().getIntExtra(getString(R.string.extra_key_level), 1);
        mLevelDir = getIntent().getStringExtra(getString(R.string.extra_key_level_dir));

        // Init score
        mScore = new Score(mLevelNumber);

        // Sound preferences
        determineSoundPreferences();

        // Retain views
        mLevelView = (LevelView) findViewById(R.id.level_view);
        mScoreView = (TextView) findViewById(R.id.txt_score);
        mScoreView.setText(String.format(getString(R.string.score), mScore.getTotalScore()));

        // Show level view
        drawLevel();

        // Audio
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mIdSoundWall = mSoundPool.load(FileUtils.getfileordefault(this, mLevel.getPath(), "wall_hit.wav"), 1);
        mIdSoundGameOver = mSoundPool.load(FileUtils.getfileordefault(this, mLevel.getPath(), "gameover.wav"), 1);
        mIdSoundWin = mSoundPool.load(FileUtils.getfileordefault(this, mLevel.getPath(), "fins_level_completed.wav"), 1);

        // Hide ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sound preferences
        determineSoundPreferences();
        registerObserver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBackgroundMusicPlayback();
        unregisterObserver();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    /**
     * Starts the game
     */
    private void startGame() {
        mScore.reset();
        drawLevel();
        mLevelView.startNewThread();
        registerObserver();
    }

    /**
     * Pauses the game
     */
    private void pauseGame() {
        stopBackgroundMusicPlayback();
        unregisterObserver();
    }

    /**
     * Advance to next level
     */
    private void nextLevel() {
        // Dialog not shown anymore -> Register observer again
        registerObserver();
        if (!isLastLevel()) {
            mLevelNumber++;
            mScore.setLevel(mLevelNumber);
            mScore.reset();
            mLevelDir = getNextLevelPath();
            drawLevel();
            mLevelView.startNewThread();
        } else {
            // Player completed last level, exit
            finish();
        }
    }

    /**
     * Register LevelActivity to observe both the Level and Score models
     */
    private void registerObserver() {
        if (!mObserving) {
            mLevel.addObserver(this);
            mScore.addObserver(this);
            mObserving = true;
        }
    }

    /**
     * Unregisters LevelActivity as an observer of Level and Score
     */
    private void unregisterObserver() {
        if (mObserving) {
            mLevel.deleteObserver(this);
            mScore.deleteObserver(this);
            mObserving = false;
        }
    }

    /**
     * Determines whether to mute sound or not
     * Sets the mMute variable
     */
    public void determineSoundPreferences() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mMute = sharedPrefs.getBoolean(getString(R.string.pref_key_mute), false);
    }

    /**
     * Starts playback of the (annoying) background music
     */
    private void startBackgroundMusicPlayback() {
        if (mBackgroundMusic == null || !mBackgroundMusic.isPlaying()) {
            mBackgroundMusic = MediaPlayer.create(this, Uri.fromFile(new File(FileUtils.getfileordefault(this, mLevel.getPath(), "background_music.wav"))));
            mBackgroundMusic.setLooping(true);
            mBackgroundMusic.start();
        }
    }

    /**
     * Stops music playback
     */
    private void stopBackgroundMusicPlayback() {
        if (mBackgroundMusic != null) {
            mBackgroundMusic.release();
            mBackgroundMusic = null;
        }
    }

    /**
     * Save player's score in SharedPreferences if he bet his highscore
     */
    private void saveHighscore() {
        int highscore = mScore.getHighScore(this);
        if (mScore.getTotalScore() > highscore) {
            mScore.saveHighScore(this);
        }
    }

    /**
     * Check whether the current level is the last one
     *
     * @return
     */
    private boolean isLastLevel() {
        String nextLevelFileName = getNextLevelPath();
        return (nextLevelFileName == null || nextLevelFileName.length() == 0);
    }

    /**
     * Get the filename (without whole path) of the next level resource
     *
     * @return Filename of next level, null if there is no next level
     */
    private String getNextLevelPath() {
        String nextPath = null;
        List<String> levelList = FileUtils.listLvl(this);
        String currentPath = FileUtils.basename(mLevel.getPath());
        int currentIndex = levelList.indexOf(currentPath);
        if (levelList != null && !levelList.isEmpty() && currentIndex >= 0 && currentIndex <= levelList.size() - 2) {
            nextPath = levelList.get(currentIndex + 1);
        }
        return nextPath;
    }

    /**
     * Load a level from resources
     */
    private void drawLevel() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mLevel = LevelParser.getLevelFromFile(this, mLevelDir, mLevelNumber, metrics.widthPixels, metrics.heightPixels);
        if (!mMute) {
            startBackgroundMusicPlayback();
        }
        // (Re)set displayed score
        mScoreView.setText(String.format(getString(R.string.score), mScore.getTotalScore()));
    }

    /**
     * This function gets called when the player finished the current level
     */
    public void onLevelCompleted() {
        // Play sound
        if (!mMute) {
            mSoundPool.play(mIdSoundWin, 1, 1, 0, 0, 1);
        }
        // Pause
        pauseGame();
        saveHighscore();
        showSuccessDialog();
    }

    /**
     * This function gets called when the player failed the current level
     */
    public void onLevelFailed() {
        // Play sound
        if (!mMute) {
            mSoundPool.play(mIdSoundGameOver, 1, 1, 0, 0, 1);
        }
        // Pause
        pauseGame();
        mPlayerFailed = true;
        showFailureDialog();
    }

    /**
     * Gets called when a collision is detected in game
     *
     * @param collisionType The type of collision
     */
    public void onCollisionDetected(Level.EVENT collisionType) {
        mScore.collided(collisionType);
        switch (collisionType) {
            case COLLISION_BULLET:
                Log.d(TAG, "Player got hit by a bullet!");
                break;
            case COLLISION_WALL:
                if (!mMute) {
                    mSoundPool.play(mIdSoundWall, 1, 1, 0, 0, 1);
                }
                break;
        }
    }

    /**
     * Displays the dialog telling the user he failed the level
     */
    private void showFailureDialog() {
        AlertDialog.Builder successDialogBuilder = new AlertDialog.Builder(this);
        successDialogBuilder.setTitle(getString(R.string.dialog_failure_title));
        successDialogBuilder.setMessage(getString(R.string.dialog_failure_msg));
        successDialogBuilder.setPositiveButton(getString(android.R.string.ok), this);
        successDialogBuilder.setCancelable(true);
        successDialogBuilder.setOnCancelListener(this);
        successDialogBuilder.create().show();
        mPlayerFailed = true;
    }

    /**
     * Displays the dialog informing the user he completed the level
     */
    private void showSuccessDialog() {
        AlertDialog.Builder successDialogBuilder = new AlertDialog.Builder(this);
        successDialogBuilder.setTitle(getString(R.string.dialog_success_title));
        String msg;
        if (!isLastLevel()) {
            msg = String.format(getString(R.string.dialog_success_msg), mLevelNumber, mScore.getTotalScore(), mScore.getHighScore(this), (mLevelNumber + 1));
        } else {
            msg = getString(R.string.dialog_success_msg_alt);
        }
        successDialogBuilder.setMessage(msg);
        successDialogBuilder.setPositiveButton(android.R.string.ok, this);
        successDialogBuilder.setNeutralButton(R.string.dialog_success_restart, this);
        successDialogBuilder.create().show();
        mPlayerFailed = false;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int btnId) {
        switch (btnId) {
            case DialogInterface.BUTTON_POSITIVE:
                if (!mPlayerFailed) {
                    nextLevel();
                    break;
                } else {
                    startGame();
                    break;
                }
            case DialogInterface.BUTTON_NEUTRAL:
                startGame();
                break;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // User cancelled dialog, restart game
        startGame();
    }

    @Override
    public void update(Observable observable, Object data) {
        Runnable action = null;
        if (observable instanceof Score) {
            action = new Runnable() {
                @Override
                public void run() {
                    mScoreView.setText(String.format(getString(R.string.score), mScore.getTotalScore()));
                }

            };
        } else if (observable instanceof Level) {
            if (data instanceof Level.EVENT) {
                switch ((Level.EVENT) data) {
                    case GAME_OVER:
                        action = new Runnable() {
                            @Override
                            public void run() {
                                onLevelFailed();
                            }
                        };
                        break;
                    case GAME_SUCCESS:
                        action = new Runnable() {
                            @Override
                            public void run() {
                                onLevelCompleted();
                            }
                        };
                        break;
                    case COLLISION_BULLET:
                        action = new Runnable() {
                            @Override
                            public void run() {
                                onCollisionDetected(Level.EVENT.COLLISION_BULLET);
                            }
                        };
                        break;
                    case COLLISION_WALL:
                        action = new Runnable() {
                            @Override
                            public void run() {
                                onCollisionDetected(Level.EVENT.COLLISION_WALL);
                            }
                        };
                        break;
                }
            }
        }

        if (action != null) {
            mHandler.post(action);
            // Unregister observer so that only one dialog get shown
            // unregisterObserver();
        }
    }
}