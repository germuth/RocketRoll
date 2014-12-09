package ca.gerumth.spaceprototype;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

public class GameThread extends Thread {
	/*
	 * State-tracking constants
	 */
	public static final int STATE_LOSE = 1;
	public static final int STATE_PAUSE = 2;
	public static final int STATE_READY = 3;
	public static final int STATE_RUNNING = 4;
	public static final int STATE_WIN = 5;

	/*
	 * Member (state) fields
	 */
	private Context mContext;
	private int mCanvasHeight = 1;
	private int mCanvasWidth = 1;

	private Bitmap mBackgroundImage;
	private Satellite mShip;
	private Satellite mPlanet;
	private Satellite mSun;
	private Satellite mJupiter;

	/** Message handler used by thread to interact with TextView */
	private Handler mHandler;
	
	/** Used to figure out elapsed time between frames */
	private long mLastTime;

	/** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
	private int mMode;

	/** Indicate whether the surface has been created & is ready to draw */
	private boolean mRun = false;

	private final Object mRunLock = new Object();

	/** Handle to the surface manager object we interact with */
	private SurfaceHolder mSurfaceHolder;

	public GameThread(SurfaceHolder surfaceHolder, Context context,
			Handler handler) {
		// get handles to some important objects
		mSurfaceHolder = surfaceHolder;
		mHandler = handler;
		mContext = context;

		Resources res = context.getResources();
		this.mShip = new Satellite(context.getResources().getDrawable(
				R.drawable.rocket));
		this.mPlanet = new Satellite(context.getResources().getDrawable(
				R.drawable.planet));
		this.mSun = new Satellite(context.getResources().getDrawable(
				R.drawable.sun));
		this.mJupiter = new Satellite(context.getResources().getDrawable(
				R.drawable.jupiter));
		
		// load background image as a Bitmap instead of a Drawable b/c
		// we don't need to transform it and it's faster to draw this way
		mBackgroundImage = BitmapFactory.decodeResource(res,
				R.drawable.space);
	}

	/**
	 * Starts the game, setting parameters for the current difficulty.
	 */
	public void doStart() {
		synchronized (mSurfaceHolder) {
			//pick initial locations
			mShip.setPos(mCanvasWidth / 2, mCanvasHeight - mCanvasHeight / 6);
			mPlanet.setPos(mCanvasWidth / 2, (mCanvasHeight / 6) + 150);
			mPlanet.xVel += 200;
			mSun.setPos(mCanvasWidth / 2, mCanvasHeight / 6);
			mJupiter.setPos(mCanvasWidth / 4, mCanvasHeight / 2);

			mLastTime = System.currentTimeMillis() + 100;
			setState(STATE_RUNNING);
		}
	}

	/**
	 * Pauses the physics update & animation.
	 */
	public void pause() {
		synchronized (mSurfaceHolder) {
			if (mMode == STATE_RUNNING)
				setState(STATE_PAUSE);
		}
	}

	/**
	 * Restores game state from the indicated Bundle. Typically called when the
	 * Activity is being restored after having been previously destroyed.
	 * 
	 * @param savedState
	 *            Bundle containing the game state
	 */
	public synchronized void restoreState(Bundle savedState) {
		synchronized (mSurfaceHolder) {
			setState(STATE_PAUSE);
//			mX = savedState.getDouble(KEY_X);
//			mY = savedState.getDouble(KEY_Y);
//			mDX = savedState.getDouble(KEY_DX);
//			mDY = savedState.getDouble(KEY_DY);
//
//			mShipWidth = savedState.getInt(KEY_LANDER_WIDTH);
//			mShipHeight = savedState.getInt(KEY_LANDER_HEIGHT);
//			mGoalX = savedState.getInt(KEY_GOAL_X);
		}
	}

	@Override
	public void run() {
		while (mRun) {
			Canvas c = null;
			try {
				c = mSurfaceHolder.lockCanvas(null);
				synchronized (mSurfaceHolder) {
					if (mMode == STATE_RUNNING)
						updatePhysics();
					// Critical section. Do not allow mRun to be set false until
					// we are sure all canvas draw operations are complete.
					//
					// If mRun has been toggled false, inhibit canvas
					// operations.
					synchronized (mRunLock) {
						if (mRun)
							doDraw(c);
					}
				}
			} finally {
				// do this in a finally so that if an exception is thrown
				// during the above, we don't leave the Surface in an
				// inconsistent state
				if (c != null) {
					mSurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}

	/**
	 * Dump game state to the provided Bundle. Typically called when the
	 * Activity is being suspended.
	 * 
	 * @return Bundle with this view's state
	 */
	public Bundle saveState(Bundle map) {
		synchronized (mSurfaceHolder) {
			if (map != null) {
//				map.putDouble(KEY_X, Double.valueOf(mX));
//				map.putDouble(KEY_Y, Double.valueOf(mY));
//				map.putDouble(KEY_DX, Double.valueOf(mDX));
//				map.putDouble(KEY_DY, Double.valueOf(mDY));
//				map.putInt(KEY_LANDER_WIDTH, Integer.valueOf(mShipWidth));
//				map.putInt(KEY_LANDER_HEIGHT, Integer.valueOf(mShipHeight));
//				map.putInt(KEY_GOAL_X, Integer.valueOf(mGoalX));
			}
		}
		return map;
	}

	/**
	 * Used to signal the thread whether it should be running or not. Passing
	 * true allows the thread to run; passing false will shut it down if it's
	 * already running. Calling start() after this was most recently called with
	 * false will result in an immediate shutdown.
	 * 
	 * @param b
	 *            true to run, false to shut down
	 */
	public void setRunning(boolean b) {
		// Do not allow mRun to be modified while any canvas operations
		// are potentially in-flight. See doDraw().
		synchronized (mRunLock) {
			mRun = b;
		}
	}

	/**
	 * Sets the game mode. That is, whether we are running, paused, in the
	 * failure state, in the victory state, etc.
	 * 
	 * @see #setState(int, CharSequence)
	 * @param mode
	 *            one of the STATE_* constants
	 */
	public void setState(int mode) {
		synchronized (mSurfaceHolder) {
			setState(mode, null);
		}
	}

	/**
	 * Sets the game mode. That is, whether we are running, paused, in the
	 * failure state, in the victory state, etc.
	 * 
	 * @param mode
	 *            one of the STATE_* constants
	 * @param message
	 *            string to add to screen or null
	 */
	public void setState(int mode, CharSequence message) {
		/*
		 * This method optionally can cause a text message to be displayed to
		 * the user when the mode changes. Since the View that actually renders
		 * that text is part of the main View hierarchy and not owned by this
		 * thread, we can't touch the state of that View. Instead we use a
		 * Message + Handler to relay commands to the main thread, which updates
		 * the user-text View.
		 */
		synchronized (mSurfaceHolder) {
			mMode = mode;

			if (mMode == STATE_RUNNING) {
				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putString("text", "");
				b.putInt("viz", View.INVISIBLE);
				msg.setData(b);
				mHandler.sendMessage(msg);
			} else {
//				mRotating = 0;
				Resources res = mContext.getResources();
				CharSequence str = "";
				if (mMode == STATE_READY)
					str = res.getText(R.string.mode_ready);
				else if (mMode == STATE_PAUSE)
					str = res.getText(R.string.mode_pause);
				else if (mMode == STATE_LOSE)
					str = res.getText(R.string.mode_lose);
				else if (mMode == STATE_WIN)
					str = "You win";

				if (message != null) {
					str = message + "\n" + str;
				}

				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putString("text", str.toString());
				b.putInt("viz", View.VISIBLE);
				msg.setData(b);
				mHandler.sendMessage(msg);
			}
		}
	}

	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(int width, int height) {
		// synchronized to make sure these all change atomically
		synchronized (mSurfaceHolder) {
			mCanvasWidth = width;
			mCanvasHeight = height;

			// don't forget to resize the background image
			mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage,
					width, height, true);
		}
	}

	/**
	 * Resumes from a pause.
	 */
	public void unpause() {
		// Move the real time clock up to now
		synchronized (mSurfaceHolder) {
			mLastTime = System.currentTimeMillis() + 100;
		}
		setState(STATE_RUNNING);
	}

	private float lastXPos;
	private float lastYPos;
	boolean doTouchEvent(MotionEvent event) {
		synchronized (mSurfaceHolder) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				lastXPos = event.getX();
				lastYPos = event.getY();
				if(this.mMode != STATE_RUNNING){
					doStart();					
				}
				break;
			case MotionEvent.ACTION_UP:
				mShip.xVel = event.getX() - lastXPos;
				mShip.yVel = -Math.abs(event.getY() - lastYPos);
//				if(mShip.yVel >= 30){
				mShip.image = mContext.getResources().getDrawable(R.drawable.animation_rocket);
				((AnimationDrawable)mShip.image).start();
//				}
				break;
			}
			return true;
		}
	}
	
	/**
	 * Draws the ship, fuel/speed bars, and background to the provided Canvas.
	 */
	private void doDraw(Canvas canvas) {
		// Draw the background image. Operations on the Canvas accumulate
		// so this is like clearing the screen.
		canvas.drawBitmap(mBackgroundImage, 0, 0, null);
		canvas.save();
//		canvas.rotate((float) mHeading, (float) mX, mCanvasHeight - (float) mY);
//		if (mMode == STATE_LOSE) {
//			mCrashedImage.setBounds(xLeft, yTop, xLeft + mShipWidth, yTop
//					+ mShipHeight);
//			mCrashedImage.draw(canvas);
//		} else {
		
		mSun.setBounds();
		mSun.image.draw(canvas);

		mPlanet.setBounds();
		mPlanet.image.draw(canvas);
		
		mShip.setBounds();
		mShip.image.draw(canvas);
		
		mJupiter.setBounds();
		mJupiter.image.draw(canvas);
		
		canvas.restore();
	}

	/**
	 * Figures the lander state (x, y, fuel, ...) based on the passage of
	 * real time. Does not invalidate(). Called at the start of draw(). Detects
	 * the end-of-game and sets the UI to the next state.
	 */
	private void updatePhysics() {
		long now = System.currentTimeMillis();

		// Fg = G * m1 * m2
		//          -------
		//            r^2
		//
		
		// Do nothing if mLastTime is in the future.
		// This allows the game-start to delay the start of the physics
		// by 100ms or whatever.
		if (mLastTime > now)
			return;

		double elapsed = (now - mLastTime) / 1000.0;

		//change position of each celestial object
		//the ship
		//move around jupiter
		int xDiff = mShip.xPos - mJupiter.xPos;
		int yDiff = mShip.yPos - mJupiter.yPos;
		mShip.xAcc = -(xDiff / 200);
		mShip.yAcc = -(yDiff / 200);
		mShip.updatePhysics(elapsed);
		
		//the sun
		//doesn't move
		
		//the planet
		//should be accelerating in direction of sun
		xDiff = mPlanet.xPos - mSun.xPos;
		yDiff = mPlanet.yPos - mSun.yPos;
		mPlanet.xAcc = -(xDiff / 20);
		mPlanet.yAcc = -(yDiff / 20);
		mPlanet.updatePhysics(elapsed);

		mLastTime = now;

		int result = STATE_LOSE;
		if(mShip.getRect().intersect(mSun.getRect())){
			setState(result, "YOU BURN IN HELL");
		}
		if(mShip.getRect().intersect(mJupiter.getRect())){
			setState(result, "GAS GIANT");
		}
		if(mShip.getRect().intersect(mPlanet.getRect())){
			result = STATE_WIN;
			setState(result, "YOU WIN");
		}
	}
}
