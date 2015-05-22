package ca.gerumth.spaceprototype;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import ca.gerumth.spaceprototype.Geometry.LineSegment;
import ca.gerumth.spaceprototype.Geometry.Point;
import ca.gerumth.spaceprototype.Geometry.Polygon;
import ca.gerumth.spaceprototype.levelParser.Level;
import ca.gerumth.spaceprototype.levelParser.LevelParser;

public class GameThread extends Thread {
	private static final int ALLOWED_DISTANCE_OFF_SCREEN = 250;
	
	private Context mContext;
	// width and height of screen in pixels
	private int mCanvasHeight;
	private int mCanvasWidth;

	private Bitmap mBackgroundImage;
	private Satellite mRocket;
	
	private Level mLevel;
	// holds the current level number
	private int mLvlNum;

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
		mState = GameState.STARTING_LVL;
		mLvlNum = 1;
		//itialize mLevel as empty for now
		//TODO
		mLevel = new Level();

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
						.getAssets().open("level_" + mLvlNum + ".xml");
				xmlParser.setInput(levelStream, null);

				mLevel = LevelParser.parseLevel(xmlParser, mContext, mRocket, mCanvasHeight,
						mCanvasWidth);
//				mRocket.setPos(mCanvasWidth / 2, (mCanvasHeight - mCanvasHeight / 6) + 75);

				mLastTime = System.currentTimeMillis() + 100;
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
					//only move things if we are these states
					if(mState == GameState.STARTING_LVL 
							|| mState == GameState.PLANNING_LVL
							|| mState == GameState.RUNNING_LVL){
						updatePhysics();						
					}
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

	private float currXPos = Float.MIN_VALUE;
	private float currYPos = Float.MIN_VALUE;
	boolean doTouchEvent(MotionEvent event) {
		synchronized (mSurfaceHolder) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN :
					switch(mState){
						case RUNNING_LVL:
							break;
						case STARTING_LVL:
							break;
						case PLANNING_LVL:
//							currXPos = mRocket.xPos;
//							currYPos = mRocket.yPos;
							break;
						default:
							mState = GameState.STARTING_LVL;
							doStart();
					}
					break;
					// if(mShip.yVel >= 30){
//					mRocket.image = mContext.getResources()
//							.getDrawable(R.drawable.animation_rocket);
//					((AnimationDrawable) mRocket.image).start();
					// }
				case MotionEvent.ACTION_MOVE:
					if(mState == GameState.PLANNING_LVL){
						currXPos = event.getX();
						currYPos = event.getY();
						break;
					}
				case MotionEvent.ACTION_UP :
					switch(mState){
						case STARTING_LVL:
//							currXPos = mRocket.xPos;
//							currYPos = mRocket.yPos;
							setState(GameState.PLANNING_LVL);
							break;
						case PLANNING_LVL:
							mRocket.xVel = event.getX() - mRocket.xPos;
							mRocket.yVel = event.getY() - mRocket.yPos;
							setState(GameState.RUNNING_LVL);
							break;
						case RUNNING_LVL:
							break;
						default:
							setState(GameState.STARTING_LVL);
					}
					
					break;
				case MotionEvent.ACTION_POINTER_DOWN:
					setState(GameState.LOSE);
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

		for (Satellite s : mLevel.satellites) {
			s.setBounds();
			s.image.draw(canvas);
		}
		
		//draw line
		if(mState == GameState.PLANNING_LVL && currXPos != Float.MIN_VALUE){
			Paint p = new Paint();
			p.setAlpha(255);
			p.setStrokeWidth(3);
			p.setColor(Color.WHITE);
			p.setStyle(Style.FILL_AND_STROKE);
			p.setPathEffect(new DashPathEffect(new float[]{15,4}, 0));
//			LineSegment ls = new LineSegment(new Point(mRocket.xPos, mRocket.yPos), new Point(currXPos, currYPos));
//			ls.extendLine(mCanvasHeight/2);
//			canvas.drawLine((float)ls.a.x, (float)ls.a.y, (float)ls.b.x, (float)ls.b.y, p);
			canvas.drawLine(mRocket.xPos, mRocket.yPos, currXPos, currYPos, p);	
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
		for (Satellite sat : mLevel.satellites) {
			for (Satellite orbiting : sat.effected) {
				xDiff = orbiting.xPos - sat.xPos;
				yDiff = orbiting.yPos - sat.yPos;
				orbiting.xAcc = -(xDiff / (1.0 / sat.xGrav));
				orbiting.yAcc = -(yDiff / (1.0 / sat.yGrav));
			}
		}
		// update new velocities and positions
		for (Satellite sat : mLevel.satellites) {
			sat.updatePhysics(elapsed);
		}
		
		if (isRocketMoving()) {
			this.mRocket.updatePhysics(elapsed);
		}

		mLastTime = now;

		GameState result = GameState.LOSE;
		// check for intersection
		Polygon rocketRect = mRocket.getRectangle();
		for (Satellite sat : mLevel.satellites) {
			if (sat.getCircle().intersects(rocketRect)){
				if (sat.name.equals("earth")) {
						result = GameState.WIN;
				} else {
					setState(GameState.LOSE, "");
					break;
				}
				currXPos = Float.MIN_VALUE;
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
					this.mLvlNum++;
					break;
				default:
					str = "";
					visibility = View.INVISIBLE;
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
	
	public boolean isRocketMoving(){
		//if level starts with rocket gravity
		if(mLevel.startGravity){
			if(mState == GameState.STARTING_LVL || mState == GameState.PLANNING_LVL){
				return true;
			}
		}//if game is running then rocket is moving no matter what
		if(mState == GameState.RUNNING_LVL){
			return true;
		}
		
		return false;
	}

	/**
	 * Resumes from a pause.
	 */
	public void unpause() {
		// Move the real time clock up to now
		synchronized (mSurfaceHolder) {
			mLastTime = System.currentTimeMillis() + 100;
		}
		setState(GameState.RUNNING_LVL);
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
			if (mState == GameState.RUNNING_LVL)
				setState(GameState.PAUSE);
		}
	}

	public synchronized void restoreState(Bundle savedState) {
		synchronized (mSurfaceHolder) {
			setState(GameState.PAUSE);
		}
	}
}
