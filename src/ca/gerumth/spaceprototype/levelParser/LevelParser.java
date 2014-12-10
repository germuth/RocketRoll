package ca.gerumth.spaceprototype.levelParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import ca.gerumth.spaceprototype.R;
import ca.gerumth.spaceprototype.Satellite;

public class LevelParser {
	public static ArrayList<Satellite> parseLevel(XmlPullParser xmlParser, Context context,
			Satellite rocket, int height, int width) throws XmlPullParserException, IOException {
		ArrayList<Satellite> celestialBodies = new ArrayList<Satellite>();

		HashMap<String, Satellite> nameToObject = new HashMap<String, Satellite>();
		nameToObject.put("rocket", rocket);
		
		int eventType = xmlParser.getEventType();
		Satellite currentSat = null;
		LevelElement currentTag = null;
		String[] elements = null;
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
				case XmlPullParser.START_DOCUMENT :
					break;
				case XmlPullParser.START_TAG :
					String tag = xmlParser.getName();
					if (tag.equals("CelestialBody")) {
						currentSat = new Satellite();
						celestialBodies.add(currentSat);
					} else if (tag.equals("Name")) {
						currentTag = LevelElement.NAME;
					} else if (tag.equals("XPosition")) {
						currentTag = LevelElement.XPOS;
					} else if (tag.equals("YPosition")) {
						currentTag = LevelElement.YPOS;
					} else if (tag.equals("XSpeed")) {
						currentTag = LevelElement.XSPEED;
					} else if (tag.equals("YSpeed")) {
						currentTag = LevelElement.YSPEED;
					} else if (tag.equals("XGravity")) {
						currentTag = LevelElement.XGRAV;
					} else if (tag.equals("YGravity")) {
						currentTag = LevelElement.YGRAV;
					} else if (tag.equals("Satellites")) {
						currentTag = LevelElement.SATELLITES;
					} else if (tag.equals("Image")) {
						currentTag = LevelElement.IMAGE;
					}
					break;
				case XmlPullParser.END_TAG :
					currentTag = null;
					break;
				case XmlPullParser.TEXT :
					if(currentTag != null){
						switch (currentTag) {
							case NAME :
								currentSat.name = xmlParser.getText();
								nameToObject.put(xmlParser.getText(), currentSat);
								break;
							case XPOS :
								elements = xmlParser.getText().split(",");
								currentSat.xPos = (int)(width / Double.parseDouble(elements[0]));
								if(elements.length > 1){
									currentSat.xPos += Integer.parseInt(elements[1]);
								}
								break;
							case YPOS :
								elements = xmlParser.getText().split(",");
								currentSat.yPos = (int)(height / Double.parseDouble(elements[0]));
								if(elements.length > 1){
									currentSat.yPos += Integer.parseInt(elements[1]);
								}
								break;
							case XSPEED :
								currentSat.xVel = Double.parseDouble(xmlParser.getText());
								break;
							case YSPEED :
								currentSat.yVel = Double.parseDouble(xmlParser.getText());
								break;
							case XGRAV :
								currentSat.xGrav = Double.parseDouble(xmlParser.getText());
								break;
							case YGRAV :
								currentSat.yGrav = Double.parseDouble(xmlParser.getText());
								break;
							case SATELLITES :
								String planetName = xmlParser.getText();
								if(!planetName.isEmpty()){
									currentSat.effected.add(nameToObject.get(xmlParser.getText()));
								} break;
							case IMAGE :
								currentSat.image = nameToDrawable(context.getResources(),
										xmlParser.getText());
								currentSat.imageHeight = currentSat.image.getIntrinsicHeight();
								currentSat.imageWidth = currentSat.image.getIntrinsicWidth();
								break;
						}
					}
					
					break;
			}
			eventType = xmlParser.next();
		}
		return celestialBodies;
	}

	public static Drawable nameToDrawable(Resources res, String name) {
		if (name.equals("earth")) {
			return res.getDrawable(R.drawable.earth);
		} else if (name.equals("jupiter")) {
			return res.getDrawable(R.drawable.jupiter);
		} else if (name.equals("sun")) {
			return res.getDrawable(R.drawable.sun);
		}
		return null;
	}
}