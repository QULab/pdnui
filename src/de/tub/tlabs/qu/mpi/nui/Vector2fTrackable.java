package de.tub.tlabs.qu.mpi.nui;

import javax.vecmath.Tuple2d;
import javax.vecmath.Tuple2f;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector2f;

public class Vector2fTrackable extends Vector2f {
	public Object userObject;
	public Vector2fTrackable previous;
	public float xOffset, yOffset;

	public Vector2fTrackable() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Vector2fTrackable(float x, float y, float xOffset, float yOffset) {
		super(x, y);
		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}

	public Vector2fTrackable(float[] arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public Vector2fTrackable(Tuple2d arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public Vector2fTrackable(Tuple2f arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public Vector2fTrackable(Vector2d arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public Vector2fTrackable(Vector2f arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
	
	
}
