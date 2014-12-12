package ca.gerumth.spaceprototype;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import ca.gerumth.spaceprototype.Geometry.Polygon;
import ca.gerumth.spaceprototype.levelParser.LevelParser;

public class GameThread extends Thread {
	private static final int ALLOWED_DISTANCE_OFF_SCREEN = 250;
	private Context mContext;
	// width and height of screen in pixels
	private int mCanvasHeight;
	private int mCanvasWidth;

	private Bitmap mBackgroundImage;
	private Satellite mRocket;
	private ArrayList<Satellite> mSatellites;
	// private Satellite mPlanet;
	// private Satellite mSun;
	// private Satellite mJupiter;

	// handler used to talk with containing view (GameView)
	// more specifically, set textview with win/lose etc
	private Handler mHandler;

	// Used to figure out elapsed time between frames
	private long mLastTime;
	// holds the current level number
	private int mLevel;
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
		this.mLevel = 1;

		this.mSatellites = new ArrayList<Satellite>();
		Resources res = context.getResources();
		this.mRocket = new Satellite(context.getResources().getDrawable(R.drawable.rocket));

		// load background image as a Bitmap instead of a Drawable b/c
		// we don't need to transform it and it's faster to draw this way
		mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.space);
	}

	// starts the game
	public void doStart() {
		synchronized (mSurfaceHolder) {
			XmlPullParser xmlParser;
			try {
				xmlParser = XmlPullParserFactory.newInstance().newPullParser();

				InputStream levelStream = mContext.getApplicationContext()
						.getAssets().open("level_" + mLevel + ".xml");
				xmlParser.setInput(levelStream, null);

				mSatellites = LevelParser.parseLevel(xmlParser, mContext, mRocket, mCanvasHeight,
						mCanvasWidth);

				mRocket.setPos(mCanvasWidth / 2, (mCanvasHeight - mCanvasHeight / 6) + 75);

				mLastTime = System.currentTimeMillis() + 100;
				setState(GameState.PRERUN);
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
					mRocket.xVel = event.getX() - lastXPos;
					mRocket.yVel = -Math.abs(event.getY() - lastYPos);
					// if(mShip.yVel >= 30){
//					mRocket.image = mContext.getResources()
//							.getDrawable(R.drawable.animation_rocket);
//					((AnimationDrawable) mRocket.image).start();
					// }
					if (this.mState == GameState.PRERUN) {
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

		// rotate rocket so it always faces where it is headed
		canvas.save();
		
		//rotates rocket in the direction it is moving based on x and y velocity
		//http://gamedev.stackexchange.com/questions/19209/rotate-entity-to-match-current-velocity
//		canvas.rotate((int) -(Math.tan(mRocket.xVel / mRocket.yVel) * 57.2957795), mRocket.xPos,
		canvas.rotate((int) (-270 + Math.atan2(mRocket.yVel, mRocket.xVel) * 57.2957795), mRocket.xPos,
				mRocket.yPos);
		mRocket.setBounds();
		mRocket.image.draw(canvas);
		canvas.restore();

		for (Satellite s : mSatellites) {
			s.setBounds();
			s.image.draw(canvas);
		}
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

		// change all accelarations due to orbit
		for (Satellite sat : mSatellites) {
			for (Satellite orbiting : sat.effected) {
				xDiff = orbiting.xPos - sat.xPos;
				yDiff = orbiting.yPos - sat.yPos;
				orbiting.xAcc = -(xDiff / (1.0 / sat.xGrav));
				orbiting.yAcc = -(yDiff / (1.0 / sat.yGrav));
			}
		}
		// update new velocities and positions
		for (Satellite sat : mSatellites) {
			sat.updatePhysics(elapsed);
		}
		if (mState == GameState.RUNNING) {
			this.mRocket.updatePhysics(elapsed);
		}

		mLastTime = now;

		GameState result = GameState.LOSE;
		// check for intersection
		Polygon rocketRect = mRocket.getRectangle();
		for (Satellite sat : mSatellites) {
			if (sat.getCircle().intersects(rocketRect)){
				if (sat.name.equals("earth")) {
						result = GameState.WIN;
				} else {
					setState(GameState.LOSE, "");
					break;
				}
			}
		}
		if (result == GameState.WIN)
			setState(result, "");
		// check if rocket has left screen
		if (mRocket.xPos - ALLOWED_DISTANCE_OFF_SCREEN > mCanvasWidth
				|| mRocket.xPos + ALLOWED_DISTANCE_OFF_SCREEN < 0) {
			setState(GameState.LOSE, "");
		}
		if (mRocket.yPos - ALLOWED_DISTANCE_OFF_SCREEN > mCanvasHeight
				|| mRocket.yPos + ALLOWED_DISTANCE_OFF_SCREEN < 0) {
			setState(GameState.LOSE, "");
		}
	}

	// on state change, send message to containing GameView
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
					this.mLevel++;
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

	// Callback invoked when the surface dimensions change
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
