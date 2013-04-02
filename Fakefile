# This is a configuration file for Fiji mAKE ("fake")
#
# The syntax of a Fakefile is meant to be very simple.
#
# The first rule is the default rule.
#
# All rules are of the form
#
#	target <- prerequisites
#
# before making "target", all prerequisites will be made (i.e. if there
# exists a rule, for an item on the right side, it will be executed before
# the current rule).
#
# Most rules have implicit actions: if the target is a .jar file, the items
# on the right side are packaged into the target, compiling them first, if
# they are .java files.
#
# If the last item on the right side is a .c file, the GNU C++ compiler
# will be invoked to make the target from it.
#
# If an item on the right side is a directory, and a Fakefile or a Makefile
# exists in that directory, "fake" or "make" will be called in that directory.
# The target will be simply copied from that directory after handling all
# dependencies.
#
# There is a special type of rule when "fake" does not know how to produce
# the target from the prerequisites: you can call a program with
#
#	target[program] <- items
#
# This will check if the target is up-to-date, by checking the timestamps of the
# items (if there is no item, the target is deemed _not_ up-to-date). If the target
# is not up-to-date, "fake" will execute the program with all items as parameters.
#
# Variables are defined like this:
#
#	VARIABLE=VALUE
#
# and their values can be accessed with "$VARIABLE" in most places.
#
# You can define variables depending on the platform, the target, and in some
# cases the prerequisite, by adding a tag in parentheses to the name:
#
#	VARIABLE(target)=xyz
#	VARIABLE(platform)=abc

# These variables are special, as they will be interpreted by "fake".

# Do not be verbose
verbose=false

# Usually not necessary
debug=false

# Compile .java files for this Java version
javaVersion=1.5

# Whether to show use of deprecated entities
showDeprecation=false

# If false, skips rebuilds triggered from newer Fakefile or fake.jar
# (see issues 40 & 45)
rebuildIfFakeIsNewer=true

# When building a .jar file, and a .config file of the same name is found in
# this directory, it will be included as "plugins.config".
pluginsConfigDirectory=staged-plugins

# When a prerequisite is a directory, but contains neither Fakefile nor
# Makefile, just ignore it
ignoreMissingFakefiles=true

# When a submodule could not be made, fall back to copying from this directory
precompiledDirectory=precompiled/

# Copy the dependencies to jars/
copyDependencies=true

buildDir=build/

FIJI_JAVA_HOME(linux32)=java/linux/jdk1.6.0_24/jre
FIJI_JAVA_HOME(linux64)=java/linux-amd64/jdk1.6.0_24/jre
FIJI_JAVA_HOME(win32)=java/win32/jdk1.6.0_24/jre
FIJI_JAVA_HOME(win64)=java/win64/jdk1.6.0_24/jre
FIJI_JAVA_HOME(macosx)=java/macosx-java3d
FIJI_JAVA_HOME(freebsd)=/usr/local/jdk1.6.0/jre
JAVA_HOME=$FIJI_JAVA_HOME
ENVOVERRIDES(JAVA_HOME)=true

# Java 3D
FIJI_JAVA_EXT=$FIJI_JAVA_HOME/lib/ext
FIJI_JAVA_EXT(macosx)=$FIJI_JAVA_HOME/Home/lib/ext
JAVA3D_JARS=$FIJI_JAVA_EXT/j3dcore.jar:$FIJI_JAVA_EXT/j3dutils.jar:$FIJI_JAVA_EXT/vecmath.jar

# tools.jar
TOOLS_JAR=$JAVA_HOME/../lib/tools.jar
TOOLS_JAR(macosx)=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar
ENVOVERRIDES(TOOLS_JAR)=true

# the main target

SUBMODULE_TARGETS=\
	jars/VectorString.jar \
	jars/ij-app.jar \
	plugins/Image_5D.jar \
	plugins/TrakEM2_.jar \
	plugins/ij-ImageIO_.jar \
<<<<<<< HEAD
	jars/jacl.jar \
	jars/batik.jar \
	jars/junit.jar \
	jars/rsyntaxtextarea.jar \
	jars/autocomplete.jar \
	jars/weka.jar \
	jars/jython.jar \
	jars/imglib.jar \
	jars/imglib-algorithms.jar \
	jars/imglib-ij.jar \
	jars/imglib-io.jar \
	jars/imglib2.jar \
	jars/imglib2-algorithms.jar \
	jars/imglib2-algorithms-gpl.jar \
	jars/imglib2-ij.jar \
	jars/imglib2-io.jar \
	jars/imglib2-ui.jar \
	jars/mpicbg.jar \
	jars/commons-math.jar \
	jars/javassist.jar \
	jars/jsch.jar \
	jars/imglib-scripting.jar \
	plugins/Image_5D.jar \
	jars/ij-app.jar \

PLUGIN_TARGETS=plugins/Jython_Interpreter.jar \
	plugins/Clojure_Interpreter.jar \
	plugins/JRuby_Interpreter.jar \
	plugins/BeanShell_Interpreter.jar \
	plugins/blockmatching_.jar \
	plugins/bUnwarpJ_.jar \
	plugins/register_virtual_stack_slices.jar \
	plugins/Siox_Segmentation.jar \
	plugins/registration_3d.jar \
	plugins/IO_.jar \
	plugins/CLI_.jar \
	plugins/Javascript_.jar \
	plugins/LSM_Toolbox.jar \
	plugins/SplineDeformationGenerator_.jar \
	plugins/level_sets.jar \
	plugins/Analyze_Reader_Writer.jar \
	plugins/Color_Histogram.jar \
	plugins/Color_Inspector_3D.jar \
	plugins/M_I_P.jar \
	plugins/Interactive_3D_Surface_Plot.jar \
	plugins/View5D_.jar \
	plugins/Volume_Viewer.jar \
	plugins/IJ_Robot.jar \
	plugins/Fiji_Updater.jar \
	plugins/Stitching_.jar \
	plugins/LSM_Reader.jar \
	plugins/AnalyzeSkeleton_.jar \
	plugins/Skeletonize3D_.jar \
	plugins/TurboReg_.jar \
	plugins/Feature_Detection.jar \
	plugins/Bug_Submitter.jar \
	plugins/Fiji_Plugins.jar \
	plugins/ToAST_.jar \
	plugins/MTrack2_.jar \
	plugins/Time_Stamper.jar \
	plugins/Series_Labeler.jar \
	plugins/Statistical_Region_Merging.jar \
	plugins/Refresh_Javas.jar \
	plugins/Auto_Threshold.jar \
	plugins/Arrow_.jar \
	plugins/Stack_Manipulation.jar \
	plugins/FlowJ_.jar \
	plugins/PIV_analyser.jar \
	plugins/Record_Screen.jar \
	plugins/Video_Editing.jar \
	plugins/Sync_Win.jar \
	plugins/Gray_Morphology.jar \
	plugins/Colocalisation_Analysis.jar \
	plugins/LocalThickness_.jar \
	plugins/Fiji_Developer.jar \
	plugins/Script_Editor.jar \
	plugins/Manual_Tracking.jar \
	plugins/Calculator_Plus.jar \
	plugins/3D_Objects_Counter.jar \
	plugins/Trainable_Segmentation.jar \
	plugins/IsoData_Classifier.jar \
	plugins/RATS_.jar \
	plugins/Directionality_.jar \
	plugins/Image_Expression_Parser.jar \
	plugins/Algorithm_Launcher.jar \
	plugins/VIB_.jar \
	plugins/Anisotropic_Diffusion_2D.jar \
	plugins/Simple_Neurite_Tracer.jar \
	plugins/SPIM_Registration.jar \
	plugins/QuickPALM_.jar \
	plugins/3D_Viewer.jar \
	plugins/CPU_Meter.jar \
	plugins/Graph_Cut.jar \
	plugins/TopoJ_.jar \
	plugins/Differentials_.jar \
	plugins/MosaicJ_.jar \
	plugins/PointPicker_.jar \
	plugins/SheppLogan_.jar \
	plugins/StackReg_.jar \
	plugins/UnwarpJ_.jar \
	plugins/Snakuscule_.jar \
	jars/imagescience.jar \
	plugins/TransformJ_.jar \
	plugins/FeatureJ_.jar \
	plugins/RandomJ_.jar \
	plugins/Linear_Kuwahara.jar \
	plugins/Thread_Killer.jar \
	plugins/Samples_.jar \
	plugins/Lasso_and_Blow_Tool.jar \
	jars/mij.jar \
	jars/wavelets.jar \
	jars/imageware.jar \
	plugins/Extended_Depth_Field.jar \
	plugins/panorama_.jar \
	jars/weave_jy2java.jar \
	plugins/3D_Blob_Segmentation.jar \
	plugins/Kuwahara_Filter.jar \
	plugins/Action_Bar.jar \
	plugins/Multi_Kymograph.jar \
	plugins/Reconstruct_Reader.jar \
  plugins/TrackMate_.jar \
	plugins/Colour_Deconvolution.jar \
	plugins/Dichromacy_.jar \
	plugins/Threshold_Colour.jar \
  plugins/CWNT_.jar \
	plugins/Helmholtz_Analysis.jar \
	plugins/Descriptor_based_registration.jar \
	plugins/SPIM_Opener.jar \
	plugins/Reconstruct_Reader.jar \
	plugins/FS_Align_TrakEM2.jar \
	jars/fiji-compat.jar \
	plugins/Fiji_Package_Maker.jar \
	plugins/BalloonSegmentation_.jar \
	plugins/CorrectBleach_.jar
=======
	plugins/mpicbg_.jar \
>>>>>>> origin/master

LEGACYLAUNCHER=fiji

all <- ImageJ $LEGACYLAUNCHER pom-fiji-plugins

# The "run" rule just executes ./ImageJ (as long as the file "run" does not exist...)
# It has items on the right side, because these would be passed to the executable.

run[] <- all run-fiji
run-fiji[./ImageJ] <-
DEBUG_ARGS=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n
dev[./ImageJ $DEBUG_ARGS] <-

# From submodules
CLASSPATH(plugins/mpicbg_.jar)=jars/ij.jar:jars/mpicbg.jar
plugins/mpicbg_.jar <- modules/mpicbg/
CLASSPATH(jars/mpicbg.jar)=jars/ij.jar:jars/Jama.jar
jars/mpicbg.jar <- modules/mpicbg/
CLASSPATH(jars/VectorString.jar)=jars/ij.jar:jars/Jama.jar:$JAVA3D_JARS
jars/VectorString.jar <- modules/TrakEM2/
CLASSPATH(plugins/TrakEM2_.jar)=jars/ij.jar:jars/jai_core.jar:jars/jai_codec.jar:jars/VectorString.jar:jars/postgresql.jar:jars/jcommon.jar:jars/jfreechart.jar:jars/edu_mines_jtk.jar:jars/VIB-lib.jar:plugins/VIB_.jar:jars/mpicbg.jar:plugins/loci_tools.jar:plugins/bUnwarpJ_.jar:plugins/level_sets.jar:plugins/Fiji_Plugins.jar:jars/Jama.jar:jars/imglib.jar:jars/imglib-algorithms.jar:jars/imglib-ij.jar:plugins/Simple_Neurite_Tracer.jar:plugins/3D_Viewer.jar:plugins/Lasso_and_Blow_Tool.jar:plugins/mpicbg_.jar:$JAVA3D_JARS
plugins/TrakEM2_.jar <- modules/TrakEM2/
plugins/ij-ImageIO_.jar <- modules/ij-plugins/

# From submodules, using Maven/MiniMaven
KEEPVERSION=true
jars/ij-app.jar <- modules/imagej2/
plugins/Image_5D.jar <- modules/image5d/

# From source
libs[] <- jars/test-fiji.jar jars/zs.jar jars/VIB-lib.jar jars/Jama.jar \
	jars/fiji-scripting.jar jars/fiji-lib.jar jars/jep.jar \
	jars/pal-optimization.jar jars/Updater_Fix.jar plugins/JNI_Example.jar \

CLASSPATH(jars/zs.jar)=jars/Jama.jar
CLASSPATH(plugins/ij-ImageIO_.jar)=jars/ij.jar:jars/jai_core.jar:jars/jai_codec.jar
LIBS(plugins/JNI_Example.jar)=-lm
CLASSPATH(plugins/JNI_Example.jar)=jars/ij.jar:jars/fiji-lib.jar
<<<<<<< HEAD
CLASSPATH(plugins/Kuwahara_Filter.jar)=jars/ij.jar
CLASSPATH(plugins/Action_Bar.jar)=jars/ij.jar
CLASSPATH(plugins/Multi_Kymograph.jar)=jars/ij.jar
CLASSPATH(plugins/Reconstruct_Reader.jar)=jars/ij.jar:plugins/TrakEM2_.jar
CLASSPATH(plugins/FS_Align_TrakEM2.jar)=jars/ij.jar:plugins/TrakEM2_.jar
CLASSPATH(plugins/Colour_Deconvolution.jar)=jars/ij.jar
CLASSPATH(plugins/Dichromacy_.jar)=jars/ij.jar
CLASSPATH(plugins/Threshold_Colour.jar)=jars/ij.jar
CLASSPATH(plugins/CWNT_.jar)=jars/jdom.jar:jars/ij.jar:jars/imglib-ij.jar:jars/imglib.jar:jars/imglib-algorithms.jar:jars/commons-math.jar:plugins/TrackMate_.jar:jars/jgrapht-jdk1.6.jar:jars/fiji-lib.jar
CLASSPATH(plugins/Helmholtz_Analysis.jar)=jars/ij.jar
CLASSPATH(plugins/Fiji_Package_Maker.jar)=jars/ij.jar:plugins/Fiji_Updater.jar:jars/fiji-lib.jar
CLASSPATH(plugins/BalloonSegmentation_.jar)=jars/ij.jar:jars/Jama.jar
CLASSPATH(plugins/CorrectBleach_.jar)=jars/ij.jar
=======
>>>>>>> origin/master

# pom.xml sub-projects

COPYDEPENDENCIES=true
pom-fiji-plugins <- src-plugins/
KEEPVERSION(plugins/Fiji_Updater.jar)=false

# pre-Java5 generics ;-)

src-plugins/VIB-lib/vib/FloatMatrix.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- src-plugins/VIB-lib/vib/FastMatrix.java
src-plugins/VIB-lib/math3d/FloatMatrixN.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- src-plugins/VIB-lib/math3d/FastMatrixN.java
src-plugins/VIB-lib/math3d/JacobiFloat.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- src-plugins/VIB-lib/math3d/JacobiDouble.java
src-plugins/VIB-lib/math3d/Eigensystem3x3Float.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- \
	src-plugins/VIB-lib/math3d/Eigensystem3x3Double.java
src-plugins/VIB-lib/math3d/Eigensystem2x2Float.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- \
	src-plugins/VIB-lib/math3d/Eigensystem2x2Double.java

# headless.jar

misc/headless.jar[bin/make-headless-jar.bsh] <- jars/fiji-compat.jar jars/javassist.jar jars/ij.jar

# ImageJ launcher

# We re-use ImageJ2's launcher now, so let's use a shell script to download it from the Maven repository

ImageJ[sh bin/download-launchers.sh snapshot $PLATFORM] <-

# legacy launcher

fiji[bin/copy-file.bsh $PRE $TARGET] <- ImageJ

# Precompiled stuff

precompiled/javac.jar <- jars/javac.jar

# precompiled fall back

missingPrecompiledFallBack[fail to call sh ./bin/ImageJ.sh --update update $TARGET] <- \
	plugins/Fiji_Updater.jar jars/ij.jar jars/jsch.jar

# Portable application/.app

all-apps[] <- app-macosx app-linux32 app-linux64 app-win32 app-win64
MACOSX_TIGER_LAUNCHER(macosx)=ImageJ-tiger
app-*[bin/make-app.py * $PLATFORM] <- all $MACOSX_TIGER_LAUNCHER

app-all[bin/make-app.py all $PLATFORM] <- all
app-nojre[bin/make-app.py nojre $PLATFORM] <- all

all-dmgs[] <- fiji-macosx.dmg
fiji-*.dmg[bin/make-dmg.py] <- app-* Fiji.app \
	resources/install-fiji.jpg
dmg[] <- fiji-macosx.dmg

resources/install-fiji.jpg[./fiji bin/generate-finder-background.py] <- \
	bin/generate-finder-background.py

all-tars[] <- fiji-linux32.tar.bz2 fiji-linux64.tar.bz2 \
	fiji-all.tar.bz2 fiji-nojre.tar.bz2
fiji-*.tar.bz2[bin/make-tar.py $TARGET Fiji.app] <- app-* Fiji.app
tar[] <- fiji-$PLATFORM.tar.bz2

all-zips[] <- fiji-linux32.zip fiji-linux64.zip fiji-win32.zip fiji-win64.zip \
	fiji-all.zip fiji-nojre.zip
fiji-*.zip[bin/make-zip.py $TARGET Fiji.app] <- app-* Fiji.app
zip[] <- fiji-$PLATFORM.zip

all-isos[] <- fiji-linux32.iso fiji-linux64.iso fiji-win32.iso fiji-win64.iso \
	fiji-macosx.iso fiji-all.iso fiji-nojre.iso
fiji-*.iso[genisoimage -J -V Fiji -o $TARGET Fiji.app] <- app-*

all-7zs[] <- fiji-linux32.7z fiji-linux64.7z fiji-win32.7z fiji-win64.7z \
	fiji-macosx.7z fiji-all.7z fiji-nojre.7z
fiji-*.7z[bin/make-7z.py $TARGET Fiji.app] <- app-*
