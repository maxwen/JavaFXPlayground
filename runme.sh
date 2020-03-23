#/bin/sh

JAVA_FX_HOME=/home/maxl/Downloads/javafx-sdk-13.0.2
OSM_HOME=`pwd`

/usr/java/jdk-13.0.2/bin/java --module-path $JAVA_FX_HOME/lib --add-modules javafx.controls,javafx.fxml --add-modules javafx.base,javafx.graphics \
--add-reads javafx.base=ALL-UNNAMED --add-reads javafx.graphics=ALL-UNNAMED -Djava.library.path=$JAVA_FX_HOME/lib -Dfile.encoding=UTF-8 \
-classpath $OSM_HOME/out/production/OSMViewer:$JAVA_FX_HOME/lib/src.zip:$JAVA_FX_HOME/lib/javafx-swt.jar:$JAVA_FX_HOME/lib/javafx.web.jar:$JAVA_FX_HOME/lib/javafx.base.jar:$JAVA_FX_HOME/lib/javafx.fxml.jar:$JAVA_FX_HOME/lib/javafx.media.jar:$JAVA_FX_HOME/lib/javafx.swing.jar:$JAVA_FX_HOME/lib/javafx.controls.jar:$JAVA_FX_HOME/lib/javafx.graphics.jar:$OSM_HOME/lib/json-simple-3.1.1.jar:$OSM_HOME/lib/sqlite-jdbc-3.30.1.jar:$OSM_HOME/lib/jSerialComm-2.6.0.jar com.maxwen.osmviewer.Main