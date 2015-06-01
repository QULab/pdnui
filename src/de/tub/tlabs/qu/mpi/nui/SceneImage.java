package de.tub.tlabs.qu.mpi.nui;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_16U;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.OpenNI.SceneMetaData;
import org.OpenNI.UserGenerator;

import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import processing.core.PApplet;
import processing.core.PImage;

public class SceneImage {

	// source
	SceneMetaData sceneMetaData;
	
	// buffer
	int width, height;
	ByteBuffer byteBuffer;
	ShortBuffer shortBuffer;
	
	// targets
	PImage pImage;
	IplImage iplImage;
	
	// keep track of data timestamp to avoid multiple copy cycles for getter calls on the same data
	long timeStamp;
	boolean iplImageUpdated = true;
	
	private void copyToByteBuffer() {
//		synchronized (this) {
			if (width == 0) {
				width = sceneMetaData.getXRes();
				height = sceneMetaData.getYRes();
			}
			
			if (byteBuffer == null) {
				byteBuffer = ByteBuffer.allocateDirect(width*height*2);
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				shortBuffer = byteBuffer.asShortBuffer();
			}
	
			final long sceneMetaDataTimestamp = sceneMetaData.getTimestamp();		
			if (sceneMetaDataTimestamp > timeStamp) {
				sceneMetaData.getData().copyToBuffer(byteBuffer, 2*width*height);
				timeStamp = sceneMetaDataTimestamp;
				iplImageUpdated = false;
			}
//		}
	}
	
	private void copyToIplImage() {
//		synchronized (this) {
			if (!iplImageUpdated) {
				//shortBuffer.rewind();
				//iplImage.getShortBuffer().put(shortBuffer);
				byteBuffer.rewind();
				iplImage.getByteBuffer().put(byteBuffer);
				
				iplImageUpdated = true;
			}
//		}
	}
	
	public IplImage getIplImage16u1() {
		copyToByteBuffer();
		
		if (iplImage == null) {
			iplImage = IplImage.create(width, height, IPL_DEPTH_16U, 1); // TODO remove ipl image? memleak?
		}
		
		copyToIplImage();

		//iplImage.getByteBuffer().put(byteBuffer); // should be the same as above!
		
		return iplImage;
	}
	
	public PImage getPImage() {
		copyToByteBuffer();
		
		if (pImage == null) {
			pImage = new PImage(width, height, PApplet.RGB);
		}
		
		final int[] pixels = pImage.pixels;
		final int n = width*height;

		shortBuffer.rewind();
		for (int i=0; i<n; i++) {
			final int label = shortBuffer.get();
			final int shade = label > 0 ? 255 : 0;
			pixels[i] = (shade << 0) | (shade << 8) | (shade << 16); // render as gray scale
		}

		pImage.updatePixels();		
		
		return pImage;
	}
	
	public SceneImage() {
		sceneMetaData = new SceneMetaData();
	}
	
	public SceneMetaData getSceneMetaImage() {
		return sceneMetaData;
	}
	
	void update(UserGenerator userGenerator, int userId) {
		userGenerator.getUserPixels(userId, sceneMetaData);
	}
	

}
