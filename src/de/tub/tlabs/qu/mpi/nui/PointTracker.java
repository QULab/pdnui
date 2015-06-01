package de.tub.tlabs.qu.mpi.nui;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.vecmath.Vector2f;



public class PointTracker {
	
	private float maxDistance = 64;
	private PointTrackerListener listener;

	private class Pair implements Comparable<Pair> {
		Vector2fTrackable a,b;
		float distSq;
		
		public Pair(Vector2fTrackable a, Vector2fTrackable b, float distSq) {
			this.a = a; this.b = b;
			this.distSq = distSq;
		}

		@Override
		public int compareTo(Pair o) {
			return (int)(distSq - o.distSq);
		}
	}
	
	public PointTracker(PointTrackerListener listener) {
		this.listener = listener;
	}
	
	public float getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(float maxDistance) {
		this.maxDistance = maxDistance;
	}
	
	public void clear(List<Vector2fTrackable> A) {
		for (Vector2fTrackable vector2fTrackable : A) {
			listener.lost(vector2fTrackable);
		}
	}
	
	public void track(List<Vector2fTrackable> A, List<Vector2fTrackable> B) {
		final float maxDistSq = maxDistance*maxDistance;
		
		LinkedList<Pair> pairs = new LinkedList<Pair>();
		
		for (Vector2fTrackable a : A) {
			for (Vector2fTrackable b : B) {
				float dx = a.x - b.x; float dy = a.y - b.y;
				float distSq = dx*dx + dy*dy;
				if (distSq <= maxDistSq) {
					pairs.add(new Pair(a, b, distSq));
				}
			}
		}
		
		Collections.sort(pairs);
		
		HashSet<Vector2f> tracked = new HashSet<>();
		
		for (Pair pair : pairs) {
			if (!tracked.contains(pair.a) && !tracked.contains(pair.b)) {
				tracked.add(pair.a);
				tracked.add(pair.b);
				pair.b.userObject = pair.a.userObject;
				pair.b.previous = pair.a;
				//pair.a.userObject = null;
				listener.tracked(pair.a, pair.b);
			}
		}
		
		for (Vector2fTrackable a : A) {
			if (!tracked.contains(a)) {
				listener.lost(a);
			}
		}
		
		for (Vector2fTrackable b : B) {
			if (!tracked.contains(b)) {
				listener.added(b);
			}
		}
	}

}
