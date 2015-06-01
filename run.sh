# path to the project
PROJECTPATH=.
MAINCLASS=de.tub.tlabs.qu.mpi.pdnuidemo.PdNuiDemo

# path to used java runtime environment
#  default (OpenJDK): java
#  oracle (e.g): /usr/local/bin/jre1.6.0_31/bin/java
JRE=java

# navigate to the project
cd $PROJECTPATH

# create classpath var
export CLASSPATH=\
bin:\
lib/processing-2.2.1/core.jar:\
lib/processing-2.2.1/gluegen-rt.jar:\
lib/processing-2.2.1/jogl-all.jar:\
lib/openni/org.OpenNI.jar:\
lib/javacv-0.7/javacv.jar:\
lib/vecmath/vecmath.jar:\

# start the class with the main
$JRE $MAINCLASS $*
