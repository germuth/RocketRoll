package ca.gerumth.spaceprototype.levelParser;

import java.util.ArrayList;

import ca.gerumth.spaceprototype.Satellite;

public class Level {
	
	public ArrayList<Satellite> satellites;
	public Satellite rocket;
	public boolean startGravity;
	public int lvlNum;
	
	public Level(){
		satellites = new ArrayList<Satellite>();
		startGravity = false;
		lvlNum = 1;
	}
	
	public Level(ArrayList<Satellite> sats, Satellite rock, boolean sGrav){
		satellites = sats;
		rocket = rock;
		startGravity = sGrav;
		lvlNum = 1;
	}
}
