package ca.gerumth.spaceprototype;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Celestial body which orbits another thing
 * @author Aaron
 */
public class Satellite {
	int xPos;
	int yPos;
	double xVel;
	double yVel;
	double xAcc;
	double yAcc;
	Drawable image;
	int imageHeight;
	int imageWidth;
	
	public Satellite(Drawable im){
		xPos = 0;
		yPos = 0;
		xVel = 0;
		yVel = 0;
		xAcc = 0;
		yAcc = 0;
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
		//0 0 is at top right corner
		image.setBounds(getLeftBorder(), getTopBorder(), getRightBorder(), getBottomBorder());
//		image.setBounds(0, 0, imageWidth, imageHeight);
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
