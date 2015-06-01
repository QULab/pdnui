package de.tub.tlabs.qu.mpi.record;

import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2RGB;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

import de.tub.tlabs.qu.mpi.nui.Nui;

public class KinectVideoRecorder extends VideoRecorder {

	Nui nui;
	
	public KinectVideoRecorder(int width, int height, Nui nui) {
		super(nui.getImage().getWidth(), nui.getImage().getHeight(), width, height, false);
		this.nui = nui;
		this.type = "rgb";
	}
	
	void recordFrame(IplImage iplTargetBgr) {			
		cvCvtColor(nui.getImage().getIplImage8uc3(), iplFull8uc3, CV_BGR2RGB);		
		cvResize(iplFull8uc3, iplTargetBgr);
	}	
	
}
