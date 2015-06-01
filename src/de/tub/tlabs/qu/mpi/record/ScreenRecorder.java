package de.tub.tlabs.qu.mpi.record;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvFlip;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RGBA2BGR;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;

import java.nio.ByteBuffer;

import processing.core.PApplet;
import processing.opengl.PGL;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class ScreenRecorder extends VideoRecorder {
	
	PApplet parent;
	ByteBuffer screenBuffer;
	ByteBuffer videoBufferDst;

	
	public ScreenRecorder(PApplet parent, int width, int height) {
		super(parent.width, parent.height, width, height, false);
		this.parent = parent;
		this.type = "screen";
		screenBuffer = ByteBuffer.allocateDirect(4 * sourceWidth * sourceHeight);
		videoBufferDst = iplFull8uc4.getByteBuffer();
	}
	
	

	@Override
	void recordFrame(IplImage iplTargetBgr) {
		readScreen();
		
		videoBufferDst.rewind();
		synchronized (screenBuffer) {
			videoBufferDst.put(screenBuffer);
		}

		cvCvtColor(iplFull8uc4, iplFull8uc3, CV_RGBA2BGR);
		cvFlip(iplFull8uc3, iplFull8uc3, 0);
		cvResize(iplFull8uc3, iplTargetBgr);
	}

	void readScreen() {
//		LOGGER.finest("readScreen("+sourceWidth+","+sourceHeight+")"); // wrong format!
		
		// processing 1.x
		//GL2 gl = ((PGraphicsOpenGL)pgl).beginPGL().gl.getGL2();
//		GL2 gl = ((PJOGL)pgl.beginPGL()).gl.getGL2();
//		screenBuffer.rewind();
//		gl.glReadBuffer(GL2.GL_BACK);
//		gl.glReadPixels(0, 0, sourceWidth, sourceHeight, GL2.GL_BGRA, GL2.GL_UNSIGNED_BYTE, screenBuffer);
//		pgl.endPGL();
		
		// processing 2.x
		PGL pgl = parent.beginPGL();
		screenBuffer.rewind();
		pgl.readPixels(0, 0, sourceWidth, sourceHeight, PGL.RGBA, PGL.UNSIGNED_BYTE, screenBuffer);
		parent.endPGL();
		
	}
	
	public void screenshot(String fname) {
		readScreen();
		
		IplImage iplScreenFullBgra = IplImage.create(sourceWidth, sourceHeight, IPL_DEPTH_8U, 4);
		ByteBuffer screencapBufferSrc = screenBuffer;
		ByteBuffer videoBufferDst = iplScreenFullBgra.getByteBuffer();
		synchronized (screenBuffer) {
			videoBufferDst.put(screencapBufferSrc);
		}
		cvFlip(iplScreenFullBgra, iplScreenFullBgra, 0);
		cvSaveImage(fname, iplScreenFullBgra);
	}		

}
