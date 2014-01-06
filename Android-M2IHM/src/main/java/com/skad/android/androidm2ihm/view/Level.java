package com.skad.android.androidm2ihm.view;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.View;
import com.skad.android.androidm2ihm.R;
import com.skad.android.androidm2ihm.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static java.lang.System.currentTimeMillis;

/**
 * Created by skad on 19/12/13.
 */
public class Level extends View {

    private Score mScore;
    // Bitmap background;
    private Ball mBall;
    private Hole mEnd;
    private SoundPool mSoundPool;
    private long mLastTime = 0;
    private int mLevelResId = 0;
    private int mIdSoundWall;
    private int mIdSoundGameOver;
    private int mIdSoundWin;
    private boolean mPaused = false;
    private ArrayList<Wall> mListWall = new ArrayList<>();
    private ArrayList<Hole> mListHole = new ArrayList<>();
    private ArrayList<Bullet> mListBullet = new ArrayList<>();
    private ArrayList<Gun> mListGun = new ArrayList<>();
    private onLevelEventListener mParentActivity;

    public Level(Context context, int levelResId, int levelId) {
        super(context);
        try {
            mParentActivity = (onLevelEventListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement onLevelEventListener");
        }
        mLevelResId = levelResId;
        mScore = new Score(levelId);
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mIdSoundWall = mSoundPool.load(context, R.raw.wall_hit, 1);
        mIdSoundGameOver = mSoundPool.load(context, R.raw.gameover, 1);
        mIdSoundWin = mSoundPool.load(context, R.raw.fins_level_completed, 1);
        loadLevel();
    }

    protected void loadLevel() {
        InputStream filelevelstream = getResources().openRawResource(this.mLevelResId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(filelevelstream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (!line.substring(0, 1).matches("#")) {
                    String[] temp = line.split("/");
                    String objectType = temp[0];
                    int xPos = Integer.parseInt(temp[1]);
                    int yPos = Integer.parseInt(temp[2]);
                    int width = Integer.parseInt(temp[3]);
                    int height = Integer.parseInt(temp[4]);

                    switch (objectType) {
                        case "p": // player (ball)
                            mBall = new Ball(xPos, yPos, width, height);
                            break;
                        case "h": // hole
                            Hole hole = new Hole(xPos, yPos, width, height);
                            hole.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.hole_texture));
                            mListHole.add(hole);
                            break;
                        case "w": // wall (straight)
                            Wall wall = new Wall(xPos, yPos, width, height);
                            wall.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.wall_grey_texture));
                            mListWall.add(wall);
                            break;
                        case "abl": // wall (curved - bottom left)
                            Wall wall1 = new Wall(xPos, yPos, width, height);
                            wall1.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.arcwall_bottom_left));
                            mListWall.add(wall1);
                            break;
                        case "abr": // wall (curved - bottom right)
                            Wall wall2 = new Wall(xPos, yPos, width, height);
                            wall2.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.arcwall_bottom_right));
                            mListWall.add(wall2);
                            break;
                        case "atl": // wall (curved - top left)
                            Wall wall3 = new Wall(xPos, yPos, width, height);
                            wall3.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.arcwall_top_left));
                            mListWall.add(wall3);
                            break;
                        case "atr": // wall (curved - top right)
                            Wall wall4 = new Wall(xPos, yPos, width, height);
                            wall4.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.arcwall_top_right));
                            mListWall.add(wall4);
                            break;
                        case "g": // gun
                            Gun gun = new Gun(xPos, yPos, width, height);
                            gun.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.cannon));
                            mListGun.add(gun);
                            break;
                        case "e":
                            mEnd = new Hole(xPos, yPos, width, height);
                            break;

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                filelevelstream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mBall.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.balle));
        mEnd.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.cible));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPaused) {
            return;
        }

        update();
        for (final Wall wall : mListWall) {
            canvas.drawBitmap(wall.getSprite(), wall.getX(), wall.getY(), null);
        }
        for (final Hole hole : mListHole) {
            canvas.drawBitmap(hole.getSprite(), hole.getX(), hole.getY(), null);
        }
        for (final Gun gun : mListGun) {
            canvas.drawBitmap(gun.getSprite(), gun.getX(), gun.getY(), null);
        }
        for (final Bullet bullet : mListBullet) {
            canvas.drawBitmap(bullet.getSprite(), bullet.getX(), bullet.getY(), null);
        }

        canvas.drawBitmap(mEnd.getSprite(), mEnd.getX(), mEnd.getY(), null);
        canvas.drawBitmap(mBall.getSprite(), mBall.getX(), mBall.getY(), null);
        invalidate();
    }

    private void update() {
        for (final Hole mHole : mListHole) { //gameover
            if (mHole.intoHole(mBall)) {
                mSoundPool.play(mIdSoundGameOver, 1, 1, 0, 0, 1);
                mParentActivity.onLevelFailed();
            }
        }
        if (mEnd.intoHole(mBall)) {
            // win
            mSoundPool.play(mIdSoundWin, 1, 1, 0, 0, 1);
            mParentActivity.onLevelCompleted();
        }
        for (final Bullet mBullet : mListBullet) {
            mBullet.forward();
        }
        if (currentTimeMillis() - mLastTime > 100) {
            for (final Gun mGun : mListGun) {
                Bullet mBullet = mGun.fire();
                mBullet.setSprite(BitmapFactory.decodeResource(getResources(), R.drawable.boulet));
                mListBullet.add(mBullet);
            }
        }
        mLastTime = currentTimeMillis();
        // TODO Handle player failures (wall/bullet collision)
    }

    public void setForceX(float forceX) {
        int lastX = mBall.getX();
        mBall.applyForceX(forceX);
        if (collision()) {
            mBall.setX(lastX);
        }
    }

    public void setForceY(float forceY) {
        int lastY = mBall.getY();
        mBall.applyForceY(forceY);
        if (collision()) {
            mBall.setY(lastY);
        }
    }

    protected boolean collision() {
        for (final Wall wall : mListWall) {
            if (mBall.intersects(wall)) {
                mSoundPool.play(mIdSoundWall, 1, 1, 0, 0, 1);
                mScore.collided();
                return true;
            }
        }
        return false;
    }

    public void pause() {
        mPaused = true;
    }

    public void resume() {
        mPaused = false;
    }

    public Score getScore() {
        return mScore;
    }

    public interface onLevelEventListener {
        public void onLevelCompleted();

        public void onLevelFailed();
    }
}
