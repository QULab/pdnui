package de.tub.tlabs.qu.mpi.nui;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.OpenNI.DepthMap;
import org.OpenNI.ImageMap;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

import processing.core.PApplet;
import processing.core.PImage;

public class Image {

	// source
	ImageMap imageMap;
	
	// properties
	int width, height;
	ByteBuffer byteBuffer;
	
	// targets
	PImage pImage;
	IplImage iplImage8uc3;
	
	public int getWidth() {
		if (width == 0) {
			width = imageMap.getXRes();
		}
		return width;
	}
	
	public int getHeight() {
		if (height == 0) {
			height = imageMap.getYRes();
		}
		return height;
	}	
	
	public IplImage getIplImage8uc3() {
		final int size = 3*width*height;
		
		if (iplImage8uc3 == null) {
			iplImage8uc3 = IplImage.create(width, height, IPL_DEPTH_8U, 3);
		}		
		
		if (byteBuffer == null) {
			byteBuffer = iplImage8uc3.getByteBuffer(); // TODO check if this interferes with getPImage()
		}
		
		byteBuffer.rewind();
		imageMap.copyToBuffer(byteBuffer, size);
				
		return iplImage8uc3;
	}
	
	public PImage getPImage() {
		if (width == 0) {
			width = imageMap.getXRes();
			height = imageMap.getYRes();
		}
		
		final int size = 3*width*height;
		
		if (byteBuffer == null) {
			byteBuffer = ByteBuffer.allocateDirect(size);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		if (pImage == null) {
			pImage = new PImage(width, height, PApplet.RGB);
		}
		
		final int[] pixels = pImage.pixels;
		
		imageMap.copyToBuffer(byteBuffer, size);
		
		byteBuffer.rewind();
		
		for (int i=0; i<width*height; i++) {
			final int b = byteBuffer.get() & (0xff);
			final int g = byteBuffer.get() & (0xff);
			final int r = byteBuffer.get() & (0xff);
			final int a = 255;
			pixels[i] = (r << 0) | (g << 8) | (b << 16) | (a << 24);
		}

		pImage.updatePixels(); // <1ms, but allocates tons of new memory! (GC torture)		
		
		return pImage;
	}	
	
	public ImageMap getImageMap() {
		return imageMap;
	}
}
