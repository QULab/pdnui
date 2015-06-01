package de.tub.tlabs.qu.mpi.record;

import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;

import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class WebcamRecorder extends VideoRecorder {

	OpenCVFrameGrabber grabber;
	
	public WebcamRecorder(int width, int height, OpenCVFrameGrabber grabber) {
		super(grabber.getImageWidth(), grabber.getImageHeight(), width, height, true);
		this.grabber = grabber;
		this.type = "webcam";
	}

//	@Override
//	public void start(String fname, float fps) {
//		super.start(fname, fps);
//		try {
//			grabber.start();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	@Override
//	public void stop() {
//		super.stop();
//		try {
//			grabber.stop();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	@Override
	void recordFrame(IplImage iplTargetBgr) {
		try {
			cvResize(grabber.grab(), iplTargetBgr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	

}
