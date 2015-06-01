package de.tub.tlabs.qu.mpi.nui;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_16U;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.OpenNI.DepthMap;

import processing.core.PApplet;
import processing.core.PImage;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * Container class for OpenNI DepthMap images. Converts to various formats
 * @author robbe
 *
 */
public class DepthImage {
	// source
	DepthMap depthMap;
	
	// properties
	int width, height;
	ByteBuffer byteBuffer;
	ShortBuffer shortBuffer;
	
	// targets
	PImage pImage;
	IplImage iplImage16u1;
	ShortBuffer iplImage16u1Buffer;
	IplImage iplImage8u1;
	ByteBuffer iplImage8u1Buffer;
	
	public int getWidth() {
		if (width == 0) {
			width = depthMap.getXRes();
		}
		return width;
	}
	
	public int getHeight() {
		if (height == 0) {
			height = depthMap.getYRes();
		}
		return height;
	}
	
	public IplImage getIplImage16u1() {
		if (width == 0) {
			width = depthMap.getXRes();
			height = depthMap.getYRes();
		}		
		
		if (iplImage16u1 == null) {
			iplImage16u1 = IplImage.create(width, height, IPL_DEPTH_16U, 1);
			iplImage16u1Buffer = iplImage16u1.getShortBuffer();
		}
		
		if (byteBuffer == null) {
			byteBuffer = ByteBuffer.allocateDirect(width*height*2);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			shortBuffer = byteBuffer.asShortBuffer();
		}
		
		final int n = width*height;
		depthMap.copyToBuffer(byteBuffer, n*2);
		
		shortBuffer.rewind();
		iplImage16u1Buffer.rewind();
		iplImage16u1Buffer.put(shortBuffer);
		
		return iplImage16u1;
	}
	
	public IplImage getIplImage8u1(float quantizeMaxDepth) {
		getIplImage16u1();
		
		if (iplImage8u1 == null) {
			iplImage8u1 = IplImage.create(width, height, IPL_DEPTH_8U, 1);
			iplImage8u1Buffer = iplImage8u1.getByteBuffer();
		}		
		
		final int depthMax = (int)(quantizeMaxDepth * 1000.0f) ; // maximum depth in millimeters
		int n = width * height;
		int depth, shade;
		
		iplImage8u1Buffer.rewind();
		for (int i=0; i<n; i++) {
			depth = iplImage16u1Buffer.get(i);
			if (depth > depthMax) depth = depthMax; // cutoff
			shade = (int)(depth * (255.0f / depthMax)); // scale to byte range
			iplImage8u1Buffer.put((byte)shade);
		}		
		
		return iplImage8u1;
	}
	
	public PImage getPImage(final int depthRangeStart, final int depthRangeEnd) {
		if (width == 0) {
			width = depthMap.getXRes();
			height = depthMap.getYRes();
		}
		
		if (byteBuffer == null) {
			byteBuffer = ByteBuffer.allocateDirect(width*height*2);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			shortBuffer = byteBuffer.asShortBuffer();
		}
		
		if (pImage == null) {
			pImage = new PImage(width, height, PApplet.RGB);
		}
		
		int depth, shade;
		int[] pixels = pImage.pixels;
		
		final int n = width*height;
		depthMap.copyToBuffer(byteBuffer, n*2);
				
		for (int i=0; i<n; i++) {
			depth = shortBuffer.get(i); // raw depth value in millimeters
			if (depth > depthRangeEnd) {
				depth = depthRangeEnd; // cutoff
			} else if (depth < depthRangeStart) {
				depth = depthRangeStart;
			}
			shade = (int)((depth - depthRangeStart) * (255.0f / (depthRangeEnd - depthRangeStart))); // scale to byte range
			pixels[i] = (shade << 0) | (shade << 8) | (shade << 16); // render as gray scale
		}

		pImage.updatePixels(); // <1ms, but allocates tons of new memory! (GC torture)		
		
		return pImage;
	}
	
	public DepthMap getDepthMap() {
		return depthMap;
	}

}
