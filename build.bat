#builds the jarfile
mkdir build
javac -d build src/fxbrowser/MyGui.java src/fxbrowser/DownloadBar.java
cd build
jar cvfm ../JavaFXBrowser.jar manifest.mf *
