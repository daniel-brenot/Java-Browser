#builds the jarfile
javac -d build src/fxbrowser/MyGui.java
cd build
jar cvfm ../JavaFXBrowser.jar manifest.mf *
