package ca.gerumth.spaceprototype;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
/**
 * TODO
 * add animated gifs to canvas!
 * maybe with this:
 * http://developingthedream.blogspot.in/2011/01/android-canvas-frame-by-frame-animation.html
 * 
 * Planet Images:
 * http://cache2.asset-cache.net/xc/187541964.jpg?v=2&c=IWSAsset&k=2&d=IQFlM360i9ZsAJYW8pRyli9fES7dfkuBjwBtQNnGCS81
 * @author Aaron
 */
public class MainActivity extends Activity {

	private GameView mGameView;
	private GameThread mGameThread;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mGameView = (GameView) findViewById(R.id.gameView);
        mGameThread = mGameView.getThread();

        mGameView.setTextView((TextView)findViewById(R.id.text));
        
		if (savedInstanceState == null) {
			// we were just launched: set up a new game
			mGameThread.setState(GameState.READY);
		} else {
			// we are being restored: resume a previous game
			mGameThread.restoreState(savedInstanceState);
		}
	}
    
    @Override
    protected void onPause() {
    	mGameView.getThread().pause();
    	super.onPause();
    }
}
