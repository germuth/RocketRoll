package ca.gerumth.spaceprototype;

public enum GameState {
	//before starting lvl
	READY,
	//you have tapped the screen and the level is displaying and/or still loading and about to display
	STARTING_LVL, 
	//you are currently holding your finger on the screen planning your trajectory
	PLANNING_LVL, 
	//the rocket is in flight
	RUNNING_LVL, 
	//the rocket has crashed into earth
	WIN, 
	//the rocket has crashed into anything else
	LOSE, 
	//the app has been minimized and brought back
	PAUSE
}
