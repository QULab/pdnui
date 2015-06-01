package de.tub.tlabs.qu.mpi.nui;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import org.OpenNI.CalibrationProgressEventArgs;
import org.OpenNI.CalibrationProgressStatus;
import org.OpenNI.Context;
import org.OpenNI.DepthGenerator;
import org.OpenNI.EventArgs;
import org.OpenNI.GeneralException;
import org.OpenNI.IObservable;
import org.OpenNI.IObserver;
import org.OpenNI.IRGenerator;
import org.OpenNI.ImageGenerator;
import org.OpenNI.NodeInfo;
import org.OpenNI.NodeInfoList;
import org.OpenNI.NodeType;
import org.OpenNI.OutArg;
import org.OpenNI.Point3D;
import org.OpenNI.ScriptNode;
import org.OpenNI.SkeletonCapability;
import org.OpenNI.SkeletonProfile;
import org.OpenNI.StatusException;
import org.OpenNI.UserEventArgs;
import org.OpenNI.UserGenerator;

public class Nui {
	// init logger
	static final Logger LOGGER = Logger.getLogger("Nui");
	static {
		LOGGER.setParent(Logger.getGlobal());
	}
	
	// estimated kinect parameters
	public static final float F = 580;
	public static final float SX = 320, SY = 240;		
	
	public PointTracker pointTracker;
	
	// TODO oni fallback
	
	// sensor properties (assumed to be static at runtime)
//	int depthWidth, depthHeight, fps;
	int numberOfDepthPixels;
	
	// only one context per process possible (otherwise: segfault)
	static Context context;
	
	// OpenNI
	OutArg<ScriptNode> scriptNode;
	DepthGenerator depthGenerator;
	UserGenerator userGenerator;
	ImageGenerator imageGenerator;
	IRGenerator irGenerator;
	SkeletonCapability skeletonCapability;
	
	// image buffers
	DepthImage depthImage;
	Image image;
	SceneImage sceneImage;
	
	// all users in the scene
	Set<User> users;
		
	////////////////////////////////////////////////////////////////////////////////
	// Getters / Setters
	////////////////////////////////////////////////////////////////////////////////
	
	public UserGenerator getUserGenerator() {
		return userGenerator;
	}
	
	public SceneImage getSceneImage() {
		if (sceneImage == null) {
			sceneImage = new SceneImage();
		}
		
		sceneImage.update(userGenerator, 0); // TODO each user individually
		
		return sceneImage;
	}
	
	public DepthImage getDepthImage() {
		if (depthImage == null) {
			depthImage = new DepthImage();
		}
		
		// update depthMap reference
		try {
			depthImage.depthMap = depthGenerator.getDepthMap();
			numberOfDepthPixels = depthImage.depthMap.getXRes() * depthImage.depthMap.getYRes();
		} catch (GeneralException e) {
			e.printStackTrace();
		}
		
		return depthImage;
	}
	
	public Image getImage() {
		if (image == null) {
			image = new Image();
		}
		
		// update imageMap reference
		try {
			image.imageMap = imageGenerator.getImageMap();
		} catch (GeneralException e) {
			e.printStackTrace();
		}
		
		return image;
	}
	
	public Set<User> getUsers() {
		return users;
	}

	////////////////////////////////////////////////////////////////////////////////
	// User Management
	////////////////////////////////////////////////////////////////////////////////
	
	void addUser(int id) {
		// add to set
		synchronized (users) {
			users.add(new User(this, id, pointTracker));
		}
		
		// try to calibration skeleton
		if (skeletonCapability != null) {
			try {
				if (!skeletonCapability.needPoseForCalibration()) {
					skeletonCapability.requestSkeletonCalibration(id, true);
				}
			} catch (StatusException e) {
				e.printStackTrace();
			}
		}
		
		LOGGER.info("addUser id:" + id);
	}
	
	User findUser(int id) {
		User user = null;
		for (User u : users) {
			if (u.id == id) {
				user = u;
			}
		}
		return user;
	}
	
	void removeUser(int id) {
		User user = findUser(id);
		if (user != null) {
			synchronized (users) {
				user.remove();
				users.remove(user);
			}
		}
		LOGGER.info("removeUser id:" + id);
	}
	
	void calibrationComplete(int userId, CalibrationProgressStatus status) {
		if (status == CalibrationProgressStatus.OK) {
			LOGGER.config("calibrationOk id:"+userId);
			try {
				skeletonCapability.startTracking(userId);
				User user = findUser(userId);
				if (user != null) {
					synchronized (users) {
						user.state = User.State.TRACKED;
					}
				}
			} catch (StatusException e) {
				e.printStackTrace();
			}
		} else {
			LOGGER.config("calibrationFailed id:"+userId);
			try {
				skeletonCapability.requestSkeletonCalibration(userId, true);
			} catch (StatusException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * updates users set, based on current data
	 */
	void fetchUsers() {
		synchronized (users) {
			for (User user : users) {
				user.fetch();
				
				LOGGER.info("fetchUser " + user);
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////
	// Constructor / Initialization
	////////////////////////////////////////////////////////////////////////////////
	
	private int getBus(String creationInfo) {
		String[] arr = creationInfo.split("@", 0);
		arr = arr[1].split("/", 0);
		return Integer.parseInt(arr[0]);
	}
	
	private int getDevice(String creationInfo) {
		String[] arr = creationInfo.split("@", 0);
		arr = arr[1].split("/", 0);
		return Integer.parseInt(arr[1]);
	}
	
	public void initFromXml(String fname, int bus) {
		initFromXml(fname, bus, 0);
	}
	
	public void initFromXml(String fname, int bus, int device) {
		try {
			if (context == null) {
				context = new Context();
			}
			
			NodeInfoList nodeInfoList = context.enumerateProductionTrees(NodeType.DEVICE);
			for (NodeInfo nodeInfo : nodeInfoList) {
				String creationInfo = nodeInfo.getCreationInfo();
				if ((bus == 0 || getBus(creationInfo) == bus) && (device == 0 || getDevice(creationInfo) == device)) {
					context.createProductionTree(nodeInfo);
					break;
				}				
			}
			
			context.runXmlScriptFromFile(fname);
			init();
		} catch (GeneralException e) {
			e.printStackTrace();
		}
	}
	
	public void initFromXml(String fname) {
		try {
			context = Context.createFromXmlFile(fname, scriptNode);
		} catch (GeneralException e) {
			e.printStackTrace();
		}
		init();
	}
	
	public void initFromOni(String fname) {
		try {
			context = new Context();
			context.openFileRecordingEx(fname);			
		} catch (GeneralException e) {
			e.printStackTrace();
		}
		init();
	}
	
	void init() {
		try {
			depthGenerator = DepthGenerator.create(context);
			imageGenerator = ImageGenerator.create(context);
			
			userGenerator = UserGenerator.create(context);
			userGenerator.getNewUserEvent().addObserver(new IObserver<UserEventArgs>() {
				@Override
				public void update(IObservable<UserEventArgs> arg0, UserEventArgs arg1) {
					addUser(arg1.getId());
				}
			});
			userGenerator.getUserExitEvent().addObserver(new IObserver<UserEventArgs>() {
				@Override
				public void update(IObservable<UserEventArgs> arg0, UserEventArgs arg1) {
					removeUser(arg1.getId());
				}
			});
			userGenerator.getUserReenterEvent().addObserver(new IObserver<UserEventArgs>() {
				@Override
				public void update(IObservable<UserEventArgs> arg0, UserEventArgs arg1) {
					addUser(arg1.getId());
				}
			});
			userGenerator.getLostUserEvent().addObserver(new IObserver<UserEventArgs>() {
				@Override
				public void update(IObservable<UserEventArgs> arg0, UserEventArgs arg1) {
					removeUser(arg1.getId());
				}
			});
			userGenerator.getNewDataAvailableEvent().addObserver(new IObserver<EventArgs>() {
				@Override
				public void update(IObservable<EventArgs> arg0, EventArgs arg1) {
					fetchUsers();
				}
			});
			
			skeletonCapability = userGenerator.getSkeletonCapability();
			skeletonCapability.setSkeletonProfile(SkeletonProfile.ALL);
			skeletonCapability.getCalibrationCompleteEvent().addObserver(new IObserver<CalibrationProgressEventArgs>() {
				@Override
				public void update(IObservable<CalibrationProgressEventArgs> observable, CalibrationProgressEventArgs args) {
					calibrationComplete(args.getUser(), args.getStatus());					
				}
			});
//			skeletonCapability.setSmoothing(0.99f);
			
			context.startGeneratingAll();
		} catch (GeneralException e) {
			e.printStackTrace();
		}		
	}
	
	public Nui(PointTracker pointTracker) {
		this.pointTracker = pointTracker;
		scriptNode = new OutArg<ScriptNode>();
		users = new HashSet<User>();
	}	
	
	public Nui() {
		this(null);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("users:");
		for (User user : users) {			
			sb.append(user).append(",");
		}
		sb.append("}\n");
		return sb.toString();
	}

	public void update() {
		try {
			context.waitAndUpdateAll();
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}
	
	public void updateAsync() {
		try {
			context.waitNoneUpdateAll();
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}	

	////////////////////////////////////////////////////////////////////////////////
	// Helpers
	////////////////////////////////////////////////////////////////////////////////
	
	public Vector2f convertRealWorldToProjective(Vector3f v) {
		Point3D p3d = new Point3D(v.x * 1000.0f, v.y * 1000.0f, v.z * 1000.0f);
		Vector2f v2d = null;
		try {
			Point3D p2d = depthGenerator.convertRealWorldToProjective(p3d);
			v2d = new Vector2f(p2d.getX(), p2d.getY());
		} catch (StatusException e) {
			e.printStackTrace();
		}
		return v2d;
	}
	
	public static void copy(Point3D src, Vector3f dst) {
		dst.set(src.getX() / 1000.0f, src.getY() / 1000.0f, src.getZ() / 1000.0f);
	}
	
	public static void copy(Point3D src, Vector2f dst) {
		dst.set(src.getX(), src.getY());
	}	
	

}