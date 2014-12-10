package ca.gerumth.spaceprototype;

import java.util.ArrayList;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Celestial body which orbits another thing
 * @author Aaron
 */
public class Satellite {
	public String name;
	//position
	public int xPos;
	public int yPos;
	//velocity
	public double xVel;
	public double yVel;
	//acceleration
	public double xAcc;
	public double yAcc;
	//gravity strength
	public double xGrav;
	public double yGrav;
	//other celestial bodies whose acceleration are effected by this celestial body
	//think of it as the things that orbit this
	public ArrayList<Satellite> effected;
	//image
	public Drawable image;
	public int imageHeight;
	public int imageWidth;
	
	public Satellite(){
		name = "";
		xPos = 0;
		yPos = 0;
		xVel = 0;
		yVel = 0;
		xAcc = 0;
		yAcc = 0;
		xGrav = 0;
		yGrav = 0;
		effected = new ArrayList<Satellite>();
		image = null;
		imageHeight = -1;
		imageWidth = -1;
	}
	
	public Satellite(Drawable im){
		name = "";
		xPos = 0;
		yPos = 0;
		xVel = 0;
		yVel = 0;
		xAcc = 0;
		yAcc = 0;
		xGrav = 0;
		yGrav = 0;
		effected = new ArrayList<Satellite>();
		image = im;
		imageHeight = im.getIntrinsicHeight();
		imageWidth = im.getIntrinsicWidth();
	}
	
	public void updatePhysics(double elapsed){
		//remember old velocity
		double oldXVel = xVel;
		double oldYVel = yVel;
		//calculate new velocity
		xVel += xAcc;
		yVel += yAcc;
		//figure out new position based on average speed during the period
		xPos += elapsed * (xVel + oldXVel) / 2;
		yPos += elapsed * (yVel + oldYVel) / 2;
	}
	
	public void setPos(int x, int y){
		xPos = x;
		yPos = y;
	}
	public void setBounds(){
		image.setBounds(getLeftBorder(), getTopBorder(), getRightBorder(), getBottomBorder());
	}
	
	public int getLeftBorder(){
		return xPos - (imageWidth / 2);
	}
	public int getRightBorder(){
		return xPos + (imageWidth / 2);
	}
	public int getTopBorder(){
		return yPos - (imageHeight / 2);
	}
	public int getBottomBorder(){
		return yPos + (imageHeight / 2);
	}
	
	public Rect getRect(){
		return new Rect(getLeftBorder(), getTopBorder(), getRightBorder(), getBottomBorder());
	}
}
