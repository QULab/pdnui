package de.tub.tlabs.qu.mpi.record;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public interface VideoRecordingListener {
	public void videoCaptureFrame(VideoRecorder sender, IplImage frame8uc3);
}
