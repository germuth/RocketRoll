package ca.gerumth.spaceprototype;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
/**
 * 
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
			mGameThread.setState(GameThread.STATE_READY);
			Log.w(this.getClass().getName(), "SIS is null");
		} else {
			// we are being restored: resume a previous game
			mGameThread.restoreState(savedInstanceState);
			Log.w(this.getClass().getName(), "SIS is nonnull");
		}
	}
    
    @Override
    protected void onPause() {
    	mGameView.getThread().pause();
    	super.onPause();
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mGameThread.saveState(outState);
        Log.w(this.getClass().getName(), "SIS called");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
