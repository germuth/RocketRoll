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
	double xForce;
	double yForce;
	double mass;
	Drawable image;
	int imageHeight;
	int imageWidth;
	
	public Satellite(Drawable im, double m){
		xPos = 0;
		yPos = 0;
		xVel = 0;
		yVel = 0;
		xForce = 0;
		yForce = 0;
		mass = m;
		image = im;
		imageHeight = im.getIntrinsicHeight();
		imageWidth = im.getIntrinsicWidth();
	}
	
	public void updatePhysics(double elapsed){
		//remember old velocity
		double oldXVel = xVel;
		double oldYVel = yVel;
		//calculate current acceleration
		//assume constant entire period
		double xAcc = xForce / mass;
		double yAcc = yForce / mass;
		//calculate new velocity
		xVel += xAcc;
		yVel += yAcc;
		//figure out new position based on average speed during the period
		xPos += elapsed * (xVel + oldXVel) / 2;
		yPos += elapsed * (yVel + oldYVel) / 2;
	}
	
	private final static double MAX_FORCE = 150;
	private final static double TOO_CLOSE = 15;
	
	public void applyGravityFrom(Satellite sun){
		int xDiff = this.xPos - sun.xPos;
		int yDiff = this.yPos - sun.yPos;
		
		if(xDiff - TOO_CLOSE > 0 || xDiff + TOO_CLOSE < 0){
			this.xForce = this.mass*sun.mass / Math.pow(xDiff, 2);
			if(xForce >= 0){
				// gravity is attractive, not repulsive force
				this.xForce *= -1;				
			}
		} else {
			this.xForce = 0;
		}

		if (yDiff - TOO_CLOSE > 0 || yDiff + TOO_CLOSE < 0) {
			this.yForce = this.mass * sun.mass / Math.pow(yDiff, 2);
//			if(yForce >= 0){
				this.yForce *= -1;				
//			}
		} else {
			this.yForce = 0;
		}
				
		if(xForce > MAX_FORCE){
			xForce = MAX_FORCE;
		}
		if(xForce < -MAX_FORCE){
			xForce = -MAX_FORCE;
		}
		if(yForce > MAX_FORCE){
			yForce = MAX_FORCE;
		}
		if(yForce < -MAX_FORCE){
			yForce = -MAX_FORCE;
		}
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
