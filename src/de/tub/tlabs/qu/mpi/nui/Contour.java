package de.tub.tlabs.qu.mpi.nui;

import java.util.LinkedList;
import java.util.List;

import javax.vecmath.Vector2f;

public class Contour {
	
	List<Vector2fTrackable> points;
	List<Contour> childs;
	double area;
	Vector2f center;	
	
	public Vector2f getCenter() {
		return center;
	}

	public List<Vector2fTrackable> getPoints() {
		return points;
	}
	
	public List<Contour> getChilds() {
		return childs;
	}
	
	public double getArea() {
		return area;
	}
	
	public Contour() {
		points = new LinkedList<Vector2fTrackable>();
		childs = new LinkedList<Contour>();
		area = 0;
		center = new Vector2f(0, 0);
	}

}
