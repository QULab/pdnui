package de.tub.tlabs.qu.mpi.record;

import static com.googlecode.javacv.cpp.opencv_core.CV_CMP_EQ;
import static com.googlecode.javacv.cpp.opencv_core.cvCmpS;
import static com.googlecode.javacv.cpp.opencv_core.cvSet;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GRAY2BGR;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;

import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import de.tub.tlabs.qu.mpi.nui.Nui;

public class KinectDepthRecorder extends VideoRecorder {

	Nui nui;
	
	public KinectDepthRecorder(int width, int height, Nui nui) {
		super(nui.getDepthImage().getWidth(), nui.getDepthImage().getHeight(), width, height, false);
		this.nui = nui;
		this.type = "depth";
	}
	
	void recordFrame(IplImage iplTargetBgr) {			
		cvCvtColor(nui.getDepthImage().getIplImage8u1(5.0f), iplFull8uc3, CV_GRAY2BGR);
		
		IplImage scene = nui.getSceneImage().getIplImage16u1();
		cvCmpS(scene, 0, iplFullMask8uc1, CV_CMP_EQ);
		cvSet(iplFull8uc3, CvScalar.ZERO, iplFullMask8uc1);
		
		cvResize(iplFull8uc3, iplTargetBgr);
	}
}
