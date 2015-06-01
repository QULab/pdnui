package de.tub.tlabs.qu.mpi.record;

import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvPutText;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_FOURCC;

import java.util.logging.Logger;

import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.FrameRecorder.Exception;
import com.googlecode.javacv.OpenCVFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public abstract class VideoRecorder {
	
	boolean asynchronous;
	Thread worker;
	VideoRecordingListener listener;
	
	// init logger
	static final Logger LOGGER = Logger.getLogger("VR");
	static {
		LOGGER.setParent(Logger.getGlobal());
	}		
	
	private static final int fourcc = CV_FOURCC('X', 'V', 'I', 'D');

	static final CvPoint textPointLine1 = new CvPoint();
	static {
		textPointLine1.x(10);
		textPointLine1.y(20);
	}

	static final CvPoint textPointLine2 = new CvPoint();
	static {
		textPointLine2.x(10);
		textPointLine2.y(40);
	}

	static final CvFont textFont = new CvFont(CV_FONT_HERSHEY_PLAIN, 1.0, 1);	

	protected int sourceWidth, sourceHeight; // input
	protected int targetWidth, targetHeight; // output
	protected String videoFname;
	protected long videoStarted;
	protected long videoLastFrame;
	protected float videoFps;
	protected int videoMspf; // milliseconds per frame
	protected FrameRecorder videoFrameRecorder;

	// buffers
	protected IplImage iplFull8uc4;
	protected IplImage iplFull8uc3;
	protected IplImage iplSmall8uc3;
	protected IplImage iplFullMask8uc1;
	protected String type;
	private int frame;
	
//	private enum Mode {
//		SCREEN,
//		DEPTH
//	};
//	Mode mode;
	
	public VideoRecorder(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight, boolean asynchronous) {
		this.sourceHeight = sourceHeight;
		this.sourceWidth = sourceWidth;
		this.targetHeight = targetHeight;
		this.targetWidth = targetWidth;
		
		this.asynchronous = asynchronous;

		iplFull8uc4 = IplImage.create(sourceWidth, sourceHeight, IPL_DEPTH_8U, 4);
		iplFull8uc3 = IplImage.create(sourceWidth, sourceHeight, IPL_DEPTH_8U, 3);
		iplSmall8uc3 = IplImage.create(targetWidth, targetHeight, IPL_DEPTH_8U, 3);
		iplFullMask8uc1 = IplImage.create(sourceWidth, sourceHeight, IPL_DEPTH_8U, 1);
		
		frame = 1;
		type = "undefined";
	}

	public void start(String fname, float fps) {
		videoFname = fname;

		videoFrameRecorder = new OpenCVFrameRecorder(fname, targetWidth, targetHeight);
//		videoFrameRecorder.setCodecID(fourcc);
		videoFrameRecorder.setVideoCodec(fourcc); // opencv 2.4.2
		videoFrameRecorder.setFrameRate(fps);
		videoFrameRecorder.setPixelFormat(1);

		videoStarted = System.currentTimeMillis();
		videoLastFrame = 0;
		videoFps = fps;
		videoMspf = (int) (1000 / fps);

		try {
			videoFrameRecorder.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (asynchronous) {
			worker = new Thread() {
				boolean running = true;
				
				@Override
				public void run() {
					while (running) {
						try {
							capture();
							synchronized (this) {
								this.wait();
							}
							
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
			worker.start();
		}
	}
	
	public void setListener(VideoRecordingListener listener) {
		this.listener = listener;
	}
	
	void capture() {
		recordFrame(iplSmall8uc3);

		cvPutText(iplSmall8uc3, videoStarted + "", textPointLine1, textFont, CvScalar.RED);
		cvPutText(iplSmall8uc3, System.currentTimeMillis() + "", textPointLine2, textFont, CvScalar.RED);
		
		if (listener != null) {
			listener.videoCaptureFrame(this, iplSmall8uc3);
		}
		
		try {
			videoFrameRecorder.record(iplSmall8uc3);
			LOGGER.fine("capture frame:"+(frame++)+ " type:"+type);
		} catch (Exception e) {
			e.printStackTrace();
		}			
	}
	
	public void update() {
		if (isRecording()) {
			long now = System.currentTimeMillis();
			long frame = (now - videoStarted) / videoMspf;
			if (frame > videoLastFrame) {
				if (asynchronous && worker != null) {
					synchronized (worker) {
						worker.notify();
					}
				} else {
					capture();
				}
			}
			videoLastFrame = frame;
		}
	}	
	
	public void stop() {
		try {
			videoFrameRecorder.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		videoFname = null;
	}
	
	public boolean isRecording() {
		return videoFname != null;
	}

	// ====================================
	
	void recordFrame(IplImage iplTargetBgr) {}

	// private int[] buffIds = new int[2];
	// private int currentBuf;
	//
	// public void initPBO(int width, int height){
	//
	// //GL2 mGl = PGL.gl.getGL2();
	// GL mGl = ((PGraphicsOpenGL)g).beginPGL().gl.getGL2();
	//
	// mGl.glGenBuffers(buffIds.length, buffIds, 0); //amount, ids, offset
	//
	// for (int i = 0; i < 2; ++i){
	// mGl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, buffIds[i]);
	// mGl.glBufferData(GL2.GL_PIXEL_PACK_BUFFER, width*height*4, null,
	// GL2.GL_STREAM_READ); //null to only reserve memory
	// }
	//
	// mGl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, 0);
	// currentBuf = 0;
	// }
	//
	//
	//
	// public void getScreen(ByteBuffer imageBuffer, int width, int height){
	// int curr = currentBuf;
	// int next = 1-currentBuf;
	//
	// //GL2 mGl = PGL.gl.getGL2();
	//
	// //PGraphicsOpenGL pgl = (PGraphicsOpenGL) g; // g may change
	// GL2 mGl = ((PGraphicsOpenGL)g).beginPGL().gl.getGL2();
	//
	// // Read Frame back into our ByteBuffer
	// mGl.glReadBuffer( GL.GL_FRONT ); // crashes when called from a different
	// thread
	// mGl.glPixelStorei( GL.GL_PACK_ALIGNMENT, 1 );
	//
	// synchronized (imageBuffer) {
	//
	// mGl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, buffIds[curr]);
	// mGl.glReadPixels(0, 0, width, height, GL2.GL_BGRA,
	// GL2.GL_UNSIGNED_INT_8_8_8_8_REV, 0);
	//
	// mGl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, buffIds[next]);
	//
	// ByteBuffer tmp = mGl.glMapBuffer(GL2.GL_PIXEL_PACK_BUFFER,
	// GL2.GL_READ_ONLY);
	//
	// if (tmp != null) {
	// imageBuffer.rewind();
	// imageBuffer.put(tmp);
	// imageBuffer.rewind();
	//
	// mGl.glUnmapBuffer(GL2.GL_PIXEL_PACK_BUFFER);
	// }
	//
	// mGl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, 0);
	//
	// currentBuf = 1 - currentBuf;
	//
	//
	// }
	//
	// // pgl.endPGL();
	// }

}
