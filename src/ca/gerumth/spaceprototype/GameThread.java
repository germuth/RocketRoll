package ca.gerumth.spaceprototype;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

public class GameThread extends Thread {
	private Context mContext;
	// width and height of screen in pixels
	private int mCanvasHeight;
	private int mCanvasWidth;

	private Bitmap mBackgroundImage;
	private Satellite mShip;
	private Satellite mPlanet;
	private Satellite mSun;
	private Satellite mJupiter;

	// handler used to talk with containing view (GameView)
	// more specifically, set textview with win/lose etc
	private Handler mHandler;

	// Used to figure out elapsed time between frames
	private long mLastTime;

	private GameState mState;

	// Indicate whether the surface has been created and is ready to draw
	private boolean mRun = false;
	// Handle to the surface manager object we interact with
	private SurfaceHolder mSurfaceHolder;
	private final Object mRunLock = new Object();

	public GameThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
		mSurfaceHolder = surfaceHolder;
		mHandler = handler;
		mContext = context;

		Resources res = context.getResources();
		this.mShip = 	new Satellite(context.getResources().getDrawable(R.drawable.rocket));
		this.mPlanet = 	new Satellite(context.getResources().getDrawable(R.drawable.planet));
		this.mSun = 	new Satellite(context.getResources().getDrawable(R.drawable.sun));
		this.mJupiter = new Satellite(context.getResources().getDrawable(R.drawable.jupiter));

		// load background image as a Bitmap instead of a Drawable b/c
		// we don't need to transform it and it's faster to draw this way
		mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.space);
	}

	// starts the game
	public void doStart() {
		synchronized (mSurfaceHolder) {
			setInitialPositions();
			mLastTime = System.currentTimeMillis() + 100;
			setState(GameState.PRERUN);
		}
	}
	
	public void setInitialPositions() {
		// pick initial locations of all planets
		mShip.setPos(mCanvasWidth / 2, mCanvasHeight - mCanvasHeight / 6);
		mPlanet.setPos(mCanvasWidth / 2, (mCanvasHeight / 6) + 150);
		mPlanet.xVel = 200;
		mPlanet.yVel = 0;
		mSun.setPos(mCanvasWidth / 2, mCanvasHeight / 6);
		mJupiter.setPos(mCanvasWidth / 4, mCanvasHeight / 2);
	}

	@Override
	public void run() {
		while (mRun) {
			Canvas c = null;
			try {
				c = mSurfaceHolder.lockCanvas(null);
				synchronized (mSurfaceHolder) {
					if (mState == GameState.RUNNING || mState == GameState.PRERUN)
						updatePhysics();
					// Critical section. Do not allow mRun to be set false until
					// we are sure all canvas draw operations are complete.
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

	private float lastXPos;
	private float lastYPos;
	boolean doTouchEvent(MotionEvent event) {
		synchronized (mSurfaceHolder) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN :
					lastXPos = event.getX();
					lastYPos = event.getY();
					if (this.mState != GameState.RUNNING) {
						doStart();
					}
					break;
				case MotionEvent.ACTION_UP :
					mShip.xVel = event.getX() - lastXPos;
					mShip.yVel = -Math.abs(event.getY() - lastYPos);
					// if(mShip.yVel >= 30){
					mShip.image = mContext.getResources().getDrawable(R.drawable.animation_rocket);
					((AnimationDrawable) mShip.image).start();
					// }
					if(this.mState == GameState.PRERUN){
						this.mState = GameState.RUNNING;
					}
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

	private void updatePhysics() {
		int xDiff, yDiff;
		
		long now = System.currentTimeMillis();
		// Do nothing if mLastTime is in the future.
		// This allows the game-start to delay the start of the physics
		// by 100ms or whatever.
		if (mLastTime > now)
			return;

		double elapsed = (now - mLastTime) / 1000.0;

		// change position of each celestial object
		// the ship
		// move around jupiter
		if (mState == GameState.RUNNING) {
			xDiff = mShip.xPos - mJupiter.xPos;
			yDiff = mShip.yPos - mJupiter.yPos;
			mShip.xAcc = -(xDiff / 20.0);
			mShip.yAcc = -(yDiff / 20.0);
			mShip.updatePhysics(elapsed);
		}

		// the sun
		// doesn't move

		// the planet
		// should be accelerating in direction of sun
		xDiff = mPlanet.xPos - mSun.xPos;
		yDiff = mPlanet.yPos - mSun.yPos;
		mPlanet.xAcc = -(xDiff / 20.0);
		mPlanet.yAcc = -(yDiff / 20.0);
		mPlanet.updatePhysics(elapsed);

		mLastTime = now;

		GameState result = GameState.LOSE;
		if (mShip.getRect().intersect(mSun.getRect())) {
			setState(result, "YOU BURN IN HELL");
		}
		if (mShip.getRect().intersect(mJupiter.getRect())) {
			setState(result, "GAS GIANT");
		}
		if (mShip.getRect().intersect(mPlanet.getRect())) {
			result = GameState.WIN;
			setState(result, "YOU WIN");
		}
	}
	
	//on state change, send message to containing GameView
	public void setState(GameState state, CharSequence message) {
		synchronized (mSurfaceHolder) {
			mState = state;

			Resources res = mContext.getResources();
			CharSequence str = "";
			int visibility = View.VISIBLE;
			switch (mState) {
				case RUNNING :
					str = "";
					visibility = View.INVISIBLE;
					break;
				case READY :
					str = res.getText(R.string.mode_ready);
					break;
				case PAUSE :
					str = res.getText(R.string.mode_pause);
					break;
				case LOSE :
					str = res.getText(R.string.mode_lose);
					break;
				case WIN :
					str = "You win";
					break;
			}
			if (message != null) {
				str = message + "\n" + str;
			}

			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putString("message", str.toString());
			b.putInt("visible", visibility);
			msg.setData(b);
			mHandler.sendMessage(msg);
		}
	}
	
	public void setState(GameState state) {
		setState(state, null);
	}
	
	//Callback invoked when the surface dimensions change
	public void setSurfaceSize(int width, int height) {
		// synchronized to make sure these all change atomically
		synchronized (mSurfaceHolder) {
			mCanvasWidth = width;
			mCanvasHeight = height;

			// don't forget to resize the background image
			mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, width, height, true);
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
		setState(GameState.RUNNING);
	}
	
	/**
	 * Used to signal the thread whether it should be running or not. Passing true allows the thread
	 * to run; passing false will shut it down if it's already running.
	 */
	public void setRunning(boolean b) {
		// Do not allow mRun to be modified while any canvas operations
		// are potentially in-flight. See doDraw().
		synchronized (mRunLock) {
			mRun = b;
		}
	}
	
	public void pause() {
		synchronized (mSurfaceHolder) {
			if (mState == GameState.RUNNING)
				setState(GameState.PAUSE);
		}
	}

	public synchronized void restoreState(Bundle savedState) {
		synchronized (mSurfaceHolder) {
			setState(GameState.PAUSE);
		}
	}
}
