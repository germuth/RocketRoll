package ca.gerumth.spaceprototype;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

	private TextView mStatusText;
	// The thread that actually draws the animation
	private GameThread thread;

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// listen for changes in surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// create thread only; it's started in surfaceCreated()
		thread = new GameThread(holder, context, new Handler() {
			@Override
			public void handleMessage(Message m) {
				mStatusText.setVisibility(m.getData().getInt("visible"));
				mStatusText.setText(m.getData().getString("message"));
			}
		});

		// make sure we get key events
		setFocusable(true);
	}

	// Callback invoked when the surface dimensions change
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		thread.setSurfaceSize(width, height);
	}

	// Callback invoked when the Surface has been created and is ready to be used.
	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		thread.setRunning(true);
		thread.start();
	}

	// Callback invoked when the Surface has been destroyed and must no longer be touched.
	// WARNING: after this method returns, the Surface/Canvas must never be touched again!
	public void surfaceDestroyed(SurfaceHolder holder) {
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	// Warning here is because it won't work for users without touch screen
	// screw them
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return thread.doTouchEvent(event);
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus)
			thread.pause();
	}

	public GameThread getThread() {
		return thread;
	}

	public void setTextView(TextView textView) {
		mStatusText = textView;
	}
}
