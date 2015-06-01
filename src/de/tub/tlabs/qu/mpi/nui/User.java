package de.tub.tlabs.qu.mpi.nui;

import static com.googlecode.javacv.cpp.opencv_core.CV_CMP_EQ;
import static com.googlecode.javacv.cpp.opencv_core.CV_WHOLE_SEQ;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvAvg;
import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvCmpS;
import static com.googlecode.javacv.cpp.opencv_core.cvCountNonZero;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_POLY_APPROX_DP;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RETR_CCOMP;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvApproxPoly;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvContourArea;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvFindContours;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import org.OpenNI.MapOutputMode;
import org.OpenNI.Point3D;
import org.OpenNI.SkeletonJoint;
import org.OpenNI.StatusException;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvContour;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import de.tub.tlabs.qu.mpi.util.Ticker;

//import de.tub.tlabs.qu.mpi.vis.UserObject;

public class User {
	
	// FIXME sometimes shows UNCALIBRATED though it's tracking
	
	static int idCounter = 0;
	
	public Object userObject;
	
	int numberOfPixels;
	float coverage;	
		
	/**
	 * represents a tracking state
	 * @author robbe
	 *
	 */
	public enum State {
		/**
		 * user identified but not yet calibrated (skeleton is not tracked)
		 */
		UNCALIBRATED,
		
		/**
		 * user and skeleton is tracked normally
		 */
		TRACKED,
		
		/**
		 * user is currently not seen by the sensor
		 */
		UNDETECTED,
		
		/**
		 * user is calibrated but skeleton is currently not possible to track
		 */
		UNTRACKED,
		
		/**
		 * user was lost and while be deleted the next time it's possible (avoids ConcurrentModificationExceptions)
		 */
		LOST
	}	
	
	/**
	 * minimal user coverage before user is marked UNDETECTED
	 * @see #coverage
	 * @see State
	 */
	static final float MIN_COVERAGE = 0.025f; //0.025f
	/**
	 * approximation accuracy for user contour.
	 * implies approximation for both outer and inner contours
	 */
	static final float CONTOUR_DP_EPSILON = 3.0f;
	/**
	 * minimal area of inner contours (smaller ones will be dropped)
	 */
	static final double MIN_INNER_CONTOUR_AREA = 100;
 

	int id; 
	long timeStamp;
	
	float xOffset, yOffset; // is propagated to Contour points
	
	/**
	 * current tracking state of the user.
	 * @see State
	 */
	State state;
	
	/**
	 * list of all tracked joints.
	 * this list is updated automatically, every time new data arrives from the sensor
	 * @see #getJoints()
	 * @sse {@link #fetch(Nui)}
	 */
	HashMap<SkeletonJoint, Joint> skeleton;
	
	Nui nui;
	PointTracker pointTracker;
	
//	/**
//	 * holds a pixel mask of this user 
//	 */
//	SceneImage pixelMask;
//	boolean pixelMaskUpdated;
	
	
	
	/**
	 * vectorized user contour
	 */
	Contour contour, pContour;
	//boolean contourUpdated;	


	IplImage pixelMask8u1; // user pixel mask; 8 bit unsigned, 1 channel
	CvMemStorage cvMemStorage; // some memory for openCV
	
	Vector3f centerOfMass3d;
	boolean centerOfMass3dUpdated;
	
	Vector2f centerOfMass2d;
	boolean centerOfMass2dUpdated;
	
	public float getxOffset() {
		return xOffset;
	}

	public void setxOffset(float xOffset) {
		this.xOffset = xOffset;
	}

	public float getyOffset() {
		return yOffset;
	}

	public void setyOffset(float yOffset) {
		this.yOffset = yOffset;
	}	
	
	public IplImage getPixelMask8u1() {
		return pixelMask8u1;
	}
	
	public Contour getPContour() {
		synchronized (contour) {
			return pContour;
		}
	}
		
	public Contour getContour() {
		synchronized (contour) {
			return contour;
		}
	}
	
	public int getId() {
		return id;
	}
	
	public Nui getNui() {
		return nui;
	}
	
	public Vector4f getFaceRect(int width, int height) {
		Vector2f headPosition = getJoint(SkeletonJoint.HEAD).getPosition2d();
		return new Vector4f(headPosition.x - width/2, headPosition.y, width, height);  
	}
	
	public Vector4f getFaceRect(int width, int height, int imageWidth, int imageHeight) {
		Vector2f headPosition = getJoint(SkeletonJoint.HEAD).getPosition2d();
		return new Vector4f(headPosition.x / 640.0f * imageWidth - width/2, headPosition.y / 480.0f * imageHeight, width, height);  
	}	
	
	public Vector4f getFaceRect(float sizeMeters, int imageWidth, int imageHeight) { // FIXME causes program freeze (no error)
		Vector2f headPosition = getJoint(SkeletonJoint.HEAD).getPosition2d();
		final float z = getJoint(SkeletonJoint.HEAD).getPosition3d().z;
		
		final float f = imageWidth; // roughly approximated focal length
		float sizePixels = sizeMeters / z * f;
		
		float x = headPosition.x / 640.0f * imageWidth - sizePixels / 2;
		float y = headPosition.y / 480.0f * imageHeight - sizePixels / 2;
		float w = sizePixels;
		float h = sizePixels;
		
		return new Vector4f(x, y, w, h);
	}
	
	public Vector4f getFaceRectDepthAdjusted(float size, float width, float height) {
		Vector2f headPosition = getJoint(SkeletonJoint.HEAD).getPosition2d();
		final float z = getCenterOfMass3d().z;
		
    	final float f = 650.0f;
    	float sMeters = size / 2.0f;
    	float s = sMeters / z * f / 640.0f;
    	float sPixels = s * width;
		
    	float x = (headPosition.x / 640.0f - s) * width;
    	float y = (headPosition.y / 480.0f - s) * height;
    	float w = 2*sPixels;
    	float h = 2*sPixels;
		
		return new Vector4f(x, y, w, h);  
	}
	
	public User(Nui nui, int id, PointTracker pointTracker) {
		this.nui = nui;
		this.id = id;
		this.timeStamp = System.currentTimeMillis();
		this.skeleton = new HashMap<SkeletonJoint, Joint>();
		this.state = State.UNCALIBRATED;
		this.pointTracker = pointTracker;
		
		contour = new Contour();
		
		MapOutputMode mapOutputMode = null;
		try {
			mapOutputMode = nui.depthGenerator.getMapOutputMode();
		} catch (StatusException e) {
			e.printStackTrace();
		}
		
		pixelMask8u1 = IplImage.create(mapOutputMode.getXRes(), mapOutputMode.getYRes(), IPL_DEPTH_8U, 1);
		cvMemStorage = CvMemStorage.create();
	}
	
	public boolean contains(int x, int y) {
		if (x < 0 || x >= pixelMask8u1.width() || y < 0 || y >= pixelMask8u1.height()) return false;
		return pixelMask8u1.imageData().position(x + y*pixelMask8u1.width()).get() != 0;
	}
	
	public void remove() {
		if (contour != null) pointTracker.clear(contour.getPoints());
		if (pContour != null) pointTracker.clear(pContour .getPoints());
	}
	
	public Vector3f getCenterOfMass3d() {
		//if (centerOfMass3dUpdated) {
			Point3D com;
			try {
				com = nui.userGenerator.getUserCoM(id); // FIXME org.OpenNI.StatusException: Error!
				centerOfMass3d = new Vector3f(com.getX() / 1000.0f, com.getY() / 1000.0f, com.getZ() / 1000.0f);
				centerOfMass3dUpdated = false;
			} catch (StatusException e) {
				e.printStackTrace();
			}
		//}
		return centerOfMass3d;
	}
	
	public Vector2f getCenterOfMass2d() {
		//if (centerOfMass2dUpdated) {
			Point3D com;
			try {
				com = nui.userGenerator.getUserCoM(id);
//				centerOfMass3d = new Vector3f(com.getX() / 1000.0f, com.getY() / 1000.0f, com.getZ() / 1000.0f);
				Point3D com2d = nui.depthGenerator.convertRealWorldToProjective(com);
				centerOfMass2d = new Vector2f(com2d.getX(), com2d.getY());
				centerOfMass2dUpdated = false;
			} catch (StatusException e) {
				e.printStackTrace();
			}
		//}
		return centerOfMass2d;
	}	

	@Override
	protected void finalize() throws Throwable {
//		cvReleaseImage(pixelMask8u1); // should happen automatically but you can never be sure enough! double free corruption origin?
	}
	
	public String vector3fToString(Vector3f v) {
		return new StringBuilder()
			.append("(").append(v.x).append(",").append(v.y).append(",").append(v.z).append(")")
		.toString();
	}
	
	public String vector4fToString(Vector4f v) {
		return new StringBuilder()
			.append("(").append(v.x).append(",").append(v.y).append(",").append(v.z).append(",").append(v.w).append(")")
		.toString();
	}
	
	Vector4f getJointVector(SkeletonJoint skeletonJoint) {
		Joint joint = getJoint(skeletonJoint);
		Vector3f p = joint.getPosition3d();
		return new Vector4f(p.x, p.y, p.z, joint.confidence);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("id:").append(id).append(" ")
			.append("timestamp:").append(timeStamp).append(" ")
			.append("com:").append(vector3fToString(getCenterOfMass3d())).append(" ")
			.append("rightHand:").append(vector4fToString(getJointVector(SkeletonJoint.RIGHT_HAND))).append(" ")
			.append("leftHand:").append(vector4fToString(getJointVector(SkeletonJoint.LEFT_HAND))).append(" ")
			.append("state:").append(state).append(" ")
			.append(userObject)
		.toString();
	}
	
	public Joint getJoint(SkeletonJoint skeletonJoint) {
		Joint joint = skeleton.get(skeletonJoint);
		synchronized (skeleton) {
			if (joint == null) {
				joint = new Joint(skeletonJoint);
				skeleton.put(skeletonJoint, joint);
				joint.fetch(nui, id);
			}
		}
		return joint;
	}
	
	void fetch() {
//		pixelMaskUpdated = false;
//		contourUpdated = false;
		centerOfMass3dUpdated = true;
		centerOfMass2dUpdated = true;

		// pixel mask		
		IplImage iplImg = nui.getSceneImage().getIplImage16u1();
		cvCmpS(iplImg, id, pixelMask8u1, CV_CMP_EQ);		
		numberOfPixels = cvCountNonZero(pixelMask8u1);
		coverage = (float) numberOfPixels / nui.numberOfDepthPixels;
		
		// user state
		if (coverage < MIN_COVERAGE) {
			state = State.UNDETECTED;
		} else {
			if (nui.skeletonCapability.isSkeletonTracking(id)) {
				state = State.TRACKED;
			} else {
				state = State.UNTRACKED;
			}
		}		
		
		// skeleton
		synchronized (skeleton) {
			for (Joint joint : skeleton.values()) { // FIXME (fixed?) java.util.ConcurrentModificationException
				joint.fetch(nui, id);
			}
		}
		
		// update contour
		synchronized (contour) {
			pContour = contour;
			contour = findLargestContour(pixelMask8u1, cvMemStorage, MIN_COVERAGE, 0, CONTOUR_DP_EPSILON);
			
			// track contour points
			if (pointTracker != null) {
				List<Vector2fTrackable> pPoints = pContour != null ? pContour.getPoints() : null;
				List<Vector2fTrackable> points = contour.getPoints();
				Ticker.tickProfile("user tracking (per user)");
				pointTracker.track(pPoints, points);
				Ticker.tockProfile();
			}
		}
	}
	
	Contour findLargestContour(IplImage img, CvMemStorage cvMemStorage, final double minOuterArea, final double minInnerArea, final double epsilon) {
		Contour contour = new Contour();
		
		CvSeq cvSeqContour = new CvSeq(null);  
		cvFindContours(img.clone(), cvMemStorage, cvSeqContour, Loader.sizeof(CvContour.class), CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE); // 2ms
				
		// find largest outer contour
		double largestOuterArea = 0;
		CvSeq largestOuterCvSeqContour = null;
		for(CvSeq outer = cvSeqContour; outer != null && !outer.isNull(); outer = outer.h_next()) {
			double area = cvContourArea(outer, CV_WHOLE_SEQ, 0);			
			if (area >= minOuterArea && area > largestOuterArea) {
				largestOuterArea = area;
				largestOuterCvSeqContour = outer;
			}			
		}
		
		if (largestOuterCvSeqContour != null) {
			// update contour area
			contour.area = largestOuterArea;
			
			// update contour center
			CvScalar avg = cvAvg(largestOuterCvSeqContour, null);
			contour.center = new Vector2f((float)avg.getVal(0), (float)avg.getVal(1));			
			
			// DP approximation and update of points
			CvSeq largestOuterCvSeqContourApprox = cvApproxPoly(largestOuterCvSeqContour, Loader.sizeof(CvContour.class), cvMemStorage, CV_POLY_APPROX_DP, epsilon, 0);			
			int total = largestOuterCvSeqContourApprox.total();
	    	for (int i = 0; i < total; i++) {
	        	CvPoint point = new CvPoint(cvGetSeqElem(largestOuterCvSeqContourApprox, i));
	        	contour.points.add(new Vector2fTrackable(point.x(), point.y(), xOffset, yOffset));
	    	}
			
			// for all inner contours
			for (CvSeq inner = largestOuterCvSeqContour.v_next(); inner != null; inner = inner.h_next()) {
				// update inner contour area
				double area = cvContourArea(inner, CV_WHOLE_SEQ, 0);
				
				// DP approximation and update of inner points
				if (area > minInnerArea) {
					CvSeq innerCvSeqContourApprox = cvApproxPoly(inner, Loader.sizeof(CvContour.class), cvMemStorage, CV_POLY_APPROX_DP, epsilon, 0);
					int innerTotal = innerCvSeqContourApprox.total();
					Contour innerContour = new Contour();
					innerContour.area = area;
					
					// update inner contour center
					CvScalar avgInner = cvAvg(inner, null);
					innerContour.center = new Vector2f((float)avgInner.getVal(0), (float)avgInner.getVal(1));
					
			    	for (int i = 0; i < innerTotal; i++) {
			        	CvPoint point = new CvPoint(cvGetSeqElem(innerCvSeqContourApprox, i));
			        	innerContour.points.add(new Vector2fTrackable(point.x(), point.y(), xOffset, yOffset));
			    	}
					contour.childs.add(innerContour);
				}
			}			
		}
		
		cvClearMemStorage(cvMemStorage);
		return contour;
	}
	
	public State getState() {
		return state;
	}
	
	

}
