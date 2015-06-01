package de.tub.tlabs.qu.mpi.nui;

import javax.vecmath.Vector2f;

public interface PointTrackerListener {
	void tracked(Vector2fTrackable a, Vector2fTrackable b);
	void lost(Vector2fTrackable a);
	void added(Vector2fTrackable b);
}
