package de.tub.tlabs.qu.mpi.nui;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

import java.util.logging.Logger;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class InterceptionRecognizer {
	
	// init logger
	static final Logger LOGGER = Logger.getLogger("IR");
	static {
		LOGGER.setParent(Logger.getGlobal());
	}	
	
	IplImage buffer8u1;
	
	IplImage mask8u1;
	float maskArea;
	
	Nui nui;
	boolean clear;
	
	public void clear() {
		maskArea = 0;
		cvSet(mask8u1, CvScalar.ZERO);
	}
	
	public void dump(String fname) {
		cvSaveImage(fname, mask8u1);
	}
	
	public void clearCircle(int x, int y, int r) {
		if (r > 0 && r < 640 && x > -r && y > -r && x < 640 + r && y < 480 + r) {
			// create point
			CvPoint p = new CvPoint();
			p.x(x);
			p.y(y);
			// draw circle
//			LOGGER.fine("cvCircle(...)");
			cvCircle(mask8u1, p, r, CvScalar.BLACK, -1, 8, 0);
		}
		maskArea -= (float)(Math.PI*r*r);		
	}
	
	public void pie(int x, int y, int r, float startAngle, float endAngle) {
		if (r > 0 && r < 640 && x > -r && y > -r && x < 640 + r && y < 480 + r) {
			// create point
			CvPoint p = new CvPoint();
			p.x(x);
			p.y(y);
			// create size
			CvSize axes = new CvSize(r, r);
			
			// draw pie
			cvEllipse(mask8u1, p, axes, 0, startAngle, endAngle, CvScalar.WHITE, -1, 8, 0);
		}
	}
	
	public void pieClear(int x, int y, int r, float startAngle, float endAngle) {
		if (r > 0 && r < 640 && x > -r && y > -r && x < 640 + r && y < 480 + r) {
			// create point
			CvPoint p = new CvPoint();
			p.x(x);
			p.y(y);
			// create size
			CvSize axes = new CvSize(r, r);
			
			// draw pie
			cvEllipse(mask8u1, p, axes, 0, startAngle, endAngle, CvScalar.BLACK, -1, 8, 0);
		}
	}
	
	public void circle(int x, int y, int r) {
//		LOGGER.fine("circle("+x+","+y+","+r+")");
		if (r > 0 && r < 640 && x > -r && y > -r && x < 640 + r && y < 480 + r) {
			// create point
			CvPoint p = new CvPoint();
			p.x(x);
			p.y(y);
			// draw circle
//			LOGGER.fine("cvCircle(...)");
			cvCircle(mask8u1, p, r, CvScalar.WHITE, -1, 8, 0);
		}
		maskArea += (float)(Math.PI*r*r);
	}
	
	/** destroys buffer **/
	public Vector2f interceptionCenter(IplImage a8u1) {
		CvMoments moments = new CvMoments();
		
		cvAnd(a8u1, mask8u1, buffer8u1, null);
		cvMoments(buffer8u1, moments, 1);
		return new Vector2f((float)(moments.m10() / moments.m00()), (float)(moments.m01() / moments.m00()));
	}
	
	/////////////////
	
	public InterceptionRecognizer(Nui nui) {
		this.nui = nui;
		buffer8u1 = cvCreateImage(new CvSize(640, 480), IPL_DEPTH_8U, 1);
		mask8u1 = cvCreateImage(new CvSize(640, 480), IPL_DEPTH_8U, 1);
		clear = true;
	}
	
	public float interception(IplImage a8u1) {
		cvAnd(a8u1, mask8u1, buffer8u1, null);
//		cvSaveImage("interception"+System.currentTimeMillis()+".png", buffer8u1);
		return cvCountNonZero(buffer8u1) / maskArea;
	}
	
	public float interceptionAbsolute(IplImage a8u1) {
		cvAnd(a8u1, mask8u1, buffer8u1, null);
//		cvSaveImage("interception"+System.currentTimeMillis()+".png", buffer8u1);
		return cvCountNonZero(buffer8u1);
	}		
	
	public void genCircleMask(int x, int y, int r) {
//		System.out.println("thread("+Thread.currentThread().getId()+")");
		
		// clear
		if (clear) {
			cvSet(mask8u1, CvScalar.ZERO);
		}
//		System.out.print("cvSet()");
		
		if (x >= 0 && x < 640 && y >= 0 && y < 480) {
		
			// create point
			CvPoint p = new CvPoint();
			p.x(x);
			p.y(y);
		
			// draw circle
			// cvCircle(CvArr* img, CvPoint center, int radius, CvScalar color, int thickness=1, int line_type=8, int shift=0 )¶
	//		System.out.print("cvCircle("+p+","+r+","+CvScalar.WHITE+",-1,8,0");
			
			cvCircle(mask8u1, p, r, CvScalar.WHITE, -1, 8, 0); // deadlock!
		}
//		System.out.print(")");
		maskArea = (float)(Math.PI*r*r);
	}
	
	public void genUserReferencedCircle(User user, float offsetXmeters, float offsetYmeters, float radiusMeters) {
		Vector3f com = new Vector3f(user.getCenterOfMass3d());
		com.x += offsetXmeters;
		com.y += offsetYmeters;
		Vector2f com2d = nui.convertRealWorldToProjective(com);
		
		// FIXME p = (2147483647, -2147483648) ; r = 2147483647 (occasionally, leads to deadlock/blocking in cvCircle)
		int x = (int)com2d.x;
		int y = (int)com2d.y;
		float r = radiusMeters / com.z;
		genCircleMask((int)com2d.x, (int)com2d.y, (int)r);
	}
	
	public void genUserReferencedCircleBoth(User user, float offsetXmeters, float offsetYmeters, float radiusMeters) {
		genUserReferencedCircle(user, offsetXmeters, offsetYmeters, radiusMeters);
		clear = false;
		genUserReferencedCircle(user, -offsetXmeters, offsetYmeters, radiusMeters);
		clear = true;
	}

		
}
