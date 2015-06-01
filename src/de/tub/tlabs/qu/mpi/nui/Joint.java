package de.tub.tlabs.qu.mpi.nui;

import javax.vecmath.*;

import org.OpenNI.Point3D;
import org.OpenNI.SkeletonJoint;
import org.OpenNI.SkeletonJointPosition;
import org.OpenNI.SkeletonJointTransformation;
import org.OpenNI.StatusException;

/**
 * represents a skeleton joint
 * @author robbe
 *
 */
public class Joint {

	/**
	 * 3d position of the joint in euclidean coordinates. point of origin is the sensor 
	 */
	Vector3f position3d;
	
	/**
	 * 2d position of the joint in depth image coordinates
	 */
	Vector2f position2d;
	
	/**
	 * confidence value about correctness of the data, between 0 and 1
	 */
	float confidence;
	

	final SkeletonJoint skeletonJoint;
	
	/**
	 * create a new empty Joint of a given type
	 * @param type
	 */
	public Joint (SkeletonJoint skeletonJoint) {
		this.skeletonJoint = skeletonJoint;
		position2d = new Vector2f();
		position3d = new Vector3f();
		confidence = 0;
	}
	
	void fetch(Nui nui, int userId) {
		try {
			SkeletonJointPosition skeletonJointPosition = nui.skeletonCapability.getSkeletonJointPosition(userId, skeletonJoint);
			
			Point3D point3d = skeletonJointPosition.getPosition();
			Point3D point2d = nui.depthGenerator.convertRealWorldToProjective(point3d);			
			
			confidence = skeletonJointPosition.getConfidence();
			Nui.copy(point3d, position3d);
			Nui.copy(point2d, position2d);
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("joint:").append(skeletonJoint).append("\n");
		sb.append("position:").append(position3d).append("\n");
		sb.append("confidence:").append(confidence).append("\n");
		sb.append("}\n");
		return sb.toString();
	}

	public Vector3f getPosition3d() {
		return position3d;
	}

	public Vector2f getPosition2d() {
		return position2d;
	}

	public float getConfidence() {
		return confidence;
	}

		
}
