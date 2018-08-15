package fxbrowser;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.text.Text;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

public class DownloadBar extends HBox {

	private static Stage downloadWindow = null;
	private static VBox downloadTasks;
	private static TextArea messageArea;
	private static final int BUFFER_SIZE = 4096;
	private String url;
	private ProgressBar progress;
	private Button button;
	
	
	
	/** Calling this function will guarantee that the downloadTasks VBox is created and visible.
	 * @return A Stage that will show each downloadTask's progress
	 */
	public Stage getDownloadWindow()
	{
		if(downloadWindow == null)
		{
			//Create a new borderPane for the download window
			BorderPane downloadRoot = new BorderPane();
			downloadTasks = new VBox();
			//downloadTasks will contain rows of DownloadTask objects, which are HBoxes
			downloadRoot.setCenter(		 downloadTasks		);
			
			//The bottom of the window will be the message box for download tasks
			downloadRoot.setBottom(		messageArea = new TextArea() 		);
			messageArea.setEditable(false);
			downloadWindow = new Stage();
			downloadWindow.setScene( new Scene(downloadRoot, 400, 600)  );
			
			//When closing the window, set the variable downloadWindow to null
			downloadWindow.setOnCloseRequest(		event -> downloadWindow = null		);
		}
		return downloadWindow;
	}
	
	/**The constructor for a DownloadTask
	 * 
	 * @param newLocation  The String URL of a file to download
	 */
	public DownloadBar(String newLocation)
	{
		//See if the filename at the end of newLocation exists on your hard drive.
		// If the file already exists, then add (1), (2), ... (n) until you find a new filename that doesn't exist.
		getDownloadWindow().show();

		
		HBox bar = new HBox();
		
		url=newLocation;
		
		
		progress=new ProgressBar();
		
		DownloadTask aFileDownload = new DownloadTask(newLocation);
		progress.progressProperty().bind(aFileDownload.progressProperty());
		button = new Button("Cancel");button.setOnMouseClicked(evt->{aFileDownload.cancel();aFileDownload.cancel();downloadTasks.getChildren().remove(bar);});
		
		bar.getChildren().addAll(new Text(DownloadTask.getDownloadName(url)), progress, button);
		
		downloadTasks.getChildren().add(bar);
		progress.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(progress, Priority.ALWAYS);
		
		aFileDownload.hbox=bar;
		new Thread( aFileDownload ).start();
		  
		
	}//DownloadBar()
	
	/** This class represents a task that will be run in a separate thread. It will run call(), 
	 *  and then call succeeded, cancelled, or failed depending on whether the task was cancelled
	 *  or failed. If it was not, then it will call succeeded() after call() finishes.
	 */
	private static class DownloadTask extends Task<Integer>
	{
		String fileURL;
		String fileName;
		HBox hbox;
		
		public DownloadTask(String fileURL)
		{
			this.fileURL=fileURL;
		}
		
		// This should start the download. Look at the downloadFile() function at:
		//  http://www.codejava.net/java-se/networking/use-httpurlconnection-to-download-file-from-an-http-url
		//Take that function but change it so that it updates the progress bar as it iterates through the while loop.
		//Here is a tutorial on how to upgrade a progress bar:
		//	https://docs.oracle.com/javase/8/javafx/user-interface-tutorial/progress.htm
		@Override
		protected Integer call() throws Exception {
			URL url = new URL(fileURL);
	        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
	        int responseCode = httpConn.getResponseCode();
	 
	        // always check HTTP response code first
	        if (responseCode == HttpURLConnection.HTTP_OK) {
	            fileName = "";
	            String disposition = httpConn.getHeaderField("Content-Disposition");
	            int contentLength = httpConn.getContentLength();
	 
	            if (disposition != null) {
	                // extracts file name from header field
	                int index = disposition.indexOf("filename=");
	                if (index > 0) {
	                    fileName = disposition.substring(index + 10,
	                            disposition.length() - 1);
	                }
	            }
	            else 
	            {
	                // extracts file name from URL 
	                fileName=getDirectoryName(fileURL);
	            }
	 
	            // opens input stream from the HTTP connection
	            InputStream inputStream = httpConn.getInputStream();
	            String saveFilePath = fileName;
	            System.out.println("File save path: "+fileName);
	             
	            // opens an output stream to save into file
	            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
	 
	            int bytesRead = -1;
	            int total = 0;
	            byte[] buffer = new byte[BUFFER_SIZE];
	            while ((bytesRead = inputStream.read(buffer)) != -1)
	            {
	            	if(this.isCancelled())
	            	{
	            		outputStream.close();
	    	            inputStream.close();
	    	            httpConn.disconnect();
	    	            deleteFile();
	    	            return 1;
	            	}
	                outputStream.write(buffer, 0, bytesRead);
	                total += bytesRead;
	                this.updateProgress(total, contentLength);
	            }
	 
	            outputStream.close();
	            inputStream.close();
	 
	            System.out.println("File downloaded");
	        } else {
	            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
	            this.failed();
	        }
	        httpConn.disconnect();
			return 1;
		}
		
		
		//Write the code here to handle a successful completion of the call() function.
		@Override
		protected void succeeded()
		{
			super.succeeded();	
			messageArea.appendText(getDownloadName(fileURL) + " was successfully downloaded\n");
			removeHboxWithStyle();
			requestOpenFile(fileName);
		}
		
		@Override
		protected void cancelled()
		{
			super.cancelled();
			messageArea.appendText(getDownloadName(fileName) + " Cancelled!\n");
			downloadTasks.getChildren().remove(hbox);
		}
		
		@Override
		protected void failed()
		{
			super.failed();
			messageArea.appendText(getDownloadName(fileName) + " download failed.\n");
			downloadTasks.getChildren().remove(hbox);
			deleteFile();
		}
		
		protected void removeHboxWithStyle()
		{
			Duration d = new Duration(1000);
			TranslateTransition t1 = new TranslateTransition(d,hbox);t1.setByX(350);t1.setByY(0);
			RotateTransition t2 = new RotateTransition(d, hbox);t2.setToAngle(360);t2.setFromAngle(0);
			FadeTransition t3 = new FadeTransition(d, hbox);t3.setFromValue(1);t3.setToValue(0);
			ParallelTransition t4 = new ParallelTransition(t1, t2, t3);
			t4.playFromStart();
			//downloadTasks.getChildren().remove(hbox);
		}
		
		/**
		 * Deletes the tasks file if it failed
		 */
		protected void deleteFile()
		{
			System.out.println("Deleting file " + fileName);
			Path path = Paths.get(fileName);
			try {
				Files.delete(path);
			}
			catch(IOException e){}
		}
		
		protected static String getFileNameFromURL(String url)
		{
			return url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
		}//getFileNameFromURL()
		
		protected static String getFileExtensionFromURL(String url)
		{
			return url.substring(url.lastIndexOf("."), url.length());
		}//getFileExtensionFromURL()
		
		protected static String getDirectoryName(String fileURL)
		{
			String fileName;
			fileName=MyGui.getDownloadDirectory()+fileURL.substring(fileURL.lastIndexOf("/"), fileURL.length());
			int i = 1;
			while(Files.isRegularFile(Paths.get(fileName)))
			{
				
				if(Files.isRegularFile(Paths.get(MyGui.getDownloadDirectory()+"/"+getFileNameFromURL(fileName)+String.format("(%s)", i)+getFileExtensionFromURL(fileName))))
				{
					i++;
				}
				else
				{
					fileName=MyGui.getDownloadDirectory()+"/"+getFileNameFromURL(fileName)+String.format("(%s)", i)+getFileExtensionFromURL(fileName);
				}
			}
			return fileName;
		}//getDirectoryName()
		
		protected static String getDownloadName(String fileURL)
		{
			String downName = fileURL.substring(fileURL.lastIndexOf('/')+1);
			return downName;
		}//getDownloadName()
		
		public void requestOpenFile(String fileLoc)
		{
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Run Confirmation");
			alert.setHeaderText("Would you like to run the file?");
			alert.setContentText("File Named " + getDownloadName(fileLoc) + " wants to run.");
			alert.setResultConverter(dialogButton ->
			{
			    if (dialogButton == ButtonType.OK)
			    {
			    	try {
						Desktop.getDesktop().open(new File(fileLoc));
					} catch (Exception e) {
						e.printStackTrace();
					}
			        
			    }
			    return null;
			});
			
			alert.show();
		}
	}//requestOpenFile()
}
