pdnui
==
Basic platform for kinect-based public display installations.

Author: [Robert Walter](https://github.com/robbeofficial) / [www.rwalter.de](http://www.rwalter.de)

The code was developed between 2013 and 2015 in the framework of the [Software Campus](http://www.softwarecampus.de/start/) project *IUPD*, funded by the [Federal Ministry of Education and Research](http://www.bmbf.de/en/) (grant 01IS12056). It was used as a basis for various public installations and user studies related to the project:

Cuenesics: Using Mid-Air Gestures to Select Items on Interactive Public Displays
<a href="http://www.youtube.com/watch?feature=player_embedded&v=xdusehtrXzI"><img src="http://img.youtube.com/vi/xdusehtrXzI/0.jpg" alt="Cuenesics: Using Mid-Air Gestures to Select Items on Interactive Public Displays" width="480" height="360" border="10" /></a>

Analyzing Visual Attention During Whole Body Interaction with Public Displays
<a href="http://www.youtube.com/watch?feature=player_embedded&v=W4D6tUzQRDU"><img src="http://img.youtube.com/vi/W4D6tUzQRDU/0.jpg" alt="Cuenesics: Using Mid-Air Gestures to Select Items on Interactive Public Displays" width="480" height="360" border="10" /></a>

The code contains a small feature demonstration application (PdNuiDemo). Implementations of specific installations, or project specific annotation- and analysis tools will be provided upon request.

Features
==
- Vector-based user contour rendering
- Tracking of contour points (motion measurement)
- Interception detection of geometric shapes with user contour
- Event logging framework
- Synchronized audio and video recording for screen contents, kinect depth stream, kinect rgb camera stream, as well as external webcams

Requirements
==
- javax.vecmath Package
- [OpenNI Framework](https://code.google.com/p/simple-openni/downloads/detail?name=OpenNI_NITE_Installer-Linux64-0.27.zip&can=1&q=) (v1.5.4.0)
- [NiTE Middleware](https://code.google.com/p/simple-openni/downloads/detail?name=OpenNI_NITE_Installer-Linux64-0.27.zip&can=1&q=) (v1.5.2.21)
- [OpenCV Library](http://opencv.org/) (v2.4.8)
- [JavaCV Wrapper](https://github.com/bytedeco/javacv) (v0.7)
- [Processing Library](https://processing.org/) (v2.2.1)
