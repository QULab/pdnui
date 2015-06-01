package de.tub.tlabs.qu.mpi.record;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class AudioRecorder {
	
	AudioFormat format;

	public AudioRecorder(AudioFormat format) {
		this.format = format;
	}
	
	public void start(final String fname) {
		new Thread() {

			@Override
			public void run() {
				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				TargetDataLine line;
				try {
					line = (TargetDataLine) AudioSystem.getLine(info);
			        line.open(format);
			        line.start();
			        
			        AudioInputStream ais = new AudioInputStream(line);
			        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(fname));
				} catch (LineUnavailableException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
		}.start();
	}

}
