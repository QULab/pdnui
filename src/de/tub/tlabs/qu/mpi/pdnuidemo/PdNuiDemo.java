package de.tub.tlabs.qu.mpi.pdnuidemo;

import java.util.Set;
import java.util.logging.Logger;

import javax.vecmath.Vector2f;

import processing.core.PApplet;
import de.tub.tlabs.qu.mpi.nui.Contour;
import de.tub.tlabs.qu.mpi.nui.Nui;
import de.tub.tlabs.qu.mpi.nui.User;
import de.tub.tlabs.qu.mpi.nui.Vector2fTrackable;
import de.tub.tlabs.qu.mpi.record.KinectDepthRecorder;
import de.tub.tlabs.qu.mpi.record.KinectVideoRecorder;
import de.tub.tlabs.qu.mpi.record.ScreenRecorder;
import de.tub.tlabs.qu.mpi.util.Logging;

public class PdNuiDemo extends PApplet {

	private static final long serialVersionUID = 1L;
	static final Logger LOGGER = Logger.getLogger("PdNuiDemo");
	static {
		LOGGER.setParent(Logger.getGlobal());
	}		
	
	Nui nui;
	ScreenRecorder screenRecorder;
	KinectVideoRecorder videoRecorder;
	KinectDepthRecorder depthRecorder;

	@Override
	public void setup() {
		size(1280, 800 , P2D);
		nui = new Nui();
		nui.initFromXml("config/Depth+Rgb.xml");
		
		Logging.init("log.dat");
		
		screenRecorder = new ScreenRecorder(this, width/2, height/2);
		screenRecorder.start("screen.avi", 30);
		
		videoRecorder = new KinectVideoRecorder(640, 480, nui); // not anonymized (lab studies only!)
		videoRecorder.start("user.avi", 30);
		
		depthRecorder = new KinectDepthRecorder(640, 480, nui); // anonymized (okay for field studies)
		depthRecorder.start("depth.avi", 30);
	}
	
	void update() {
		nui.updateAsync();
		
		screenRecorder.update();
		videoRecorder.update();
		depthRecorder.update();
		
		// determine screen offset of contour points (hard-coded boundaries here)
		Set<User> users = nui.getUsers();
		for (User user : users) {
			float xOffset = user.getCenterOfMass3d().x * (width / 2) * 2.0f + (width - 640) / 2;
			float yOffset = height - 480 - 72;

			user.setxOffset(xOffset);
			user.setyOffset(yOffset);
		}		
	}
	
	void drawUser(User user) {
		pushMatrix();
		
		translate(user.getxOffset(), user.getyOffset());
		
		// draw outer contours
		Contour outerContour = user.getContour();
		fill(255); noStroke();
		beginShape();
		for (Vector2f outerPoint : outerContour.getPoints()) {
			vertex(outerPoint.x, outerPoint.y);
		}
		endShape();

		// draw inner contours
		fill(0);
		for (Contour innerContour : outerContour.getChilds()) {
			beginShape();
			for (Vector2fTrackable innerPoint : innerContour.getPoints()) {
				vertex(innerPoint.x, innerPoint.y, innerPoint.x + innerPoint.xOffset, innerPoint.y + innerPoint.yOffset);
			}
			endShape();
		}
		
		// draw user-attached stuff here!
		
		popMatrix();		
	}

	@Override
	public void draw() {
		update();
		
		LOGGER.finest("draw");
		
		// draw scene
		background(0);
		Set<User> users = nui.getUsers();
		for (User user : users) {
			drawUser(user);
		}
	}

	public static void main(String[] args) {
		PApplet.main("de.tub.tlabs.qu.mpi.pdnuidemo.PdNuiDemo");
	}

}
