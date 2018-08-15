package fxbrowser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/**JavaFX Browser
 * 
 * @author Daniel Brenot
 * @version 2018-08-15
 *
 */
public class MyGui extends Application
{
	
	/** The scene for the browser*/
	protected Scene scene;
	/** Stores all of the visual components for the browser*/
	protected BorderPane root;
	/** Stores all the menu's*/
	MenuBar menuBar = new MenuBar();
	/** Makes the browser go back one page in the history*/
	protected Button backButton;
	/** Makes the browser go forward one page in the history*/
	protected Button forwardButton;
	/** Displays a dialog to add a bookmark when clicked*/
	protected Button bookmarkButton;
	/** Stores the intended address that the user wishes to go to*/
	protected TextField addressBar;
	
	/**The pane that holds all the browser tabs*/
	protected TabPane tabs;
	/** The MenuItem that stores the bookmarks*/
	Menu bookmarkMenu;
	/** The view that shows all of the history of the browser*/
	ListView<Entry> historyView = new ListView<>();
	/**The file location where the bookmarks are stored*/
	File bookmarkFile;
	/** The location where the downloads are stored*/
	private static String downloadDirectory;
	/** The location where the homepage is*/
	private static String homepage;
	/** The default width of the scene*/
	private double width;
	/** The default height of the scene*/
	private double height;
	/** The default x location of the stage*/
	private double x;
	/** The default y location of the stage*/
	private double y;
	/** Stores the stage of the browser*/
	private Stage stage;
	
	/**Calls all the methods in the class
	 * 
	 * @param args The parameters that define how the program launches
	 */
	public static void main(String args[])
	{
		launch(args);
	}//main()
	
	
	@Override
	/** Starts the GUI with the given stage
	 * @param primaryStage The stage used 
	 */
	public void start(Stage primaryStage) throws Exception
	{
		
		stage = primaryStage;
		root = new BorderPane();
		tabs = new TabPane();
		

		
		
		VBox topContainer = new VBox();
		HBox addressContainer = new HBox();
		addressContainer.setMaxWidth(Double.MAX_VALUE);
		menuBar = new MenuBar();
		root.setTop(topContainer);
		root.setCenter(tabs);
		
		// Main Menus
		Menu fileMenu = new Menu("File");
		bookmarkMenu = new Menu("Bookmarks");
		Menu helpMenu = new Menu("Help");
		Menu settingsMenu = new Menu("Setings");
		Menu javascriptMenu = new Menu("JavaScript");
		
		//File Menu
		MenuItem quit = new MenuItem("Quit");quit.setOnAction(evt -> {quitBrowser();});
		MenuItem nTab = new MenuItem("New Tab");nTab.setOnAction(evt -> {addTab();});
		
		fileMenu.getItems().addAll(nTab, quit);
		
		//Help Menu
		MenuItem getHelp = new MenuItem("Get help for java class");getHelp.setOnAction(evt->{javaClassSearch();});
		MenuItem showHistory = new CheckMenuItem("Show History");showHistory.setOnAction(evt->{hideOrShow();});
		MenuItem about = new MenuItem("About");about.setOnAction(evt->{displayAbout();});
		helpMenu.getItems().addAll(getHelp, showHistory, about);	
		
		//Settings Menu
		MenuItem homepageItem = new MenuItem("Homepage");homepageItem.setOnAction(evt->{setHomepage();});
		MenuItem downloadItem = new MenuItem("Downloads");downloadItem.setOnAction(evt->{setDownloads();});
		settingsMenu.getItems().addAll(homepageItem, downloadItem);
			
		//JavaScript menu
		MenuItem executeScript = new MenuItem("Execute Code");executeScript.setOnAction(evt->{executeScript();});
		javascriptMenu.getItems().add(executeScript);
		
		//Adds all of items to the top of the browser
		menuBar.getMenus().addAll(fileMenu, bookmarkMenu, helpMenu, settingsMenu, javascriptMenu);
		topContainer.getChildren().addAll(menuBar, addressContainer);
		
		readSettings();
		scene = new Scene(root, width, height);
		primaryStage.setScene(scene);
		primaryStage.setX(x);
		primaryStage.setY(y);
		
		
		
		EventHandler<MouseEvent> responder = new EventHandler<MouseEvent>()
				{
					public void handle(MouseEvent evt)
					{
						if(evt.getSource() == backButton){goBack();}
						else if(evt.getSource() == bookmarkButton){addBookmark();}
						else if(evt.getSource() == forwardButton){goForward();}
						else if(evt.getSource() == addressBar){if(evt.getClickCount() == 2){addressBar.setText("");}}
				}};
		
		//Address bar and tools
		backButton = new Button("Back");backButton.setOnMouseClicked(responder);backButton.setTooltip(new Tooltip("Goes back 1 page in the browser history"));
		bookmarkButton = new Button("Add Bookmark");bookmarkButton.setOnMouseClicked(responder);backButton.setTooltip(new Tooltip("Adds a bookmarked page to the browser"));
		forwardButton = new Button("Forward");forwardButton.setOnMouseClicked(responder);forwardButton.setTooltip(new Tooltip("Goes forward 1 page in the browser history"));
		addressBar = new TextField("https://www.google.ca/");addressBar.setOnMouseClicked(responder);
		
		//Causes the address bar to take up the maximum width it can
		addressBar.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(addressBar, Priority.ALWAYS);
		addressBar.setOnKeyPressed(evt -> {if(evt.getCode().equals(KeyCode.ENTER)){getCurrentWebView().getEngine().load(addressBar.getText());}});
		addressContainer.getChildren().addAll(backButton, addressBar, bookmarkButton, forwardButton);
		addTab();
		tabs.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) ->
		{
	        addressBar.setText(this.getCurrentWebView().getEngine().getLocation());
	    });
		//Creates the history view and sets its properties
		createHistoryView();
		
		//Creates the file where the bookmarks are stored
		bookmarkFile = new File("bookmarks.txt");
		
		//Sets the keyboard shortcuts for each browser item
		quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
		about.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN));
		getHelp.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
		showHistory.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
		nTab.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
		
		
		//Shows the program on the screen
		primaryStage.show();
		primaryStage.setOnCloseRequest(evt ->
		{
			saveSettings();
		});
	}//start()
	
	/**
	 * Creates a List View that displays all the webpages visited
	 */
	public void createHistoryView()
	{
		historyView = new ListView<>(getCurrentWebView().getEngine().getHistory().getEntries());
		historyView.setPrefWidth(450);
		historyView.setOnMouseClicked(evt->
		{getCurrentWebView().getEngine().getHistory().go(historyView.getSelectionModel().getSelectedIndex() - getCurrentWebView().getEngine().getHistory().getCurrentIndex());});
		historyView.setMaxWidth(0);
		historyView.setVisible(false);
		root.setRight(historyView);
	}//createHistoryView()
	
	/**Checks if a bookmark with the given url exists in the arraylist
	 * 
	 * @param url The URL of the bookmark being found
	 * @return true if the bookmark exists; false otherwise
	 */
	public boolean isBookmarkPresent(String url)
	{
		boolean found = false;
		int i = 0;
		while(i < bookmarkMenu.getItems().size() && found == false)
		{
			if(bookmarkMenu.getItems().get(i).getText().equals(url))
			{
				found = true;
			}
			i++;
		}
		return found;
	}//isBookmarkPresent()
	
	/** 
	 * Displays the information about the current version of the software
	 */
	public void displayAbout()
	{
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Information Dialog");
		alert.setHeaderText("About");
		alert.setContentText(
				"Author: Daniel Brenot\n"
				+ "Version: 1.0\n"
				+ "Date: Mar 11, 2016");
		alert.show();
	}//displayAbout()
	
	/** 
	 * Displays a dialog that will search google for the specified String
	 */
	public void javaClassSearch()
	{
		TextInputDialog dialog = new TextInputDialog("Type Here");
		dialog.setTitle("Find help for java class");
		dialog.setHeaderText("Search for Java Class Documentation");
		dialog.setContentText("Which Java class do you want to research?");
		
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent())
		{
			getCurrentWebView().getEngine().load(getSearchURL("www.google.ca/", "java " + result.get()));
		}

	}//javaClassSearch
	
	/** 
	 * Displays a dialog that will change the homepage of the browser
	 */
	public void setHomepage()
	{
		TextInputDialog dialog = new TextInputDialog(getCurrentWebView().getEngine().getLocation());
		dialog.setTitle("Set Homepage");
		dialog.setHeaderText("Set the default browser homepage");
		dialog.setContentText("What is the new homepage?");
		
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent())
		{
			homepage = result.get();
		}

	}//javaClassSearch
	
	/** 
	 * Displays a dialog that will prompt the user for their preferred download directory
	 */
	public void setDownloads()
	{
		final DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setInitialDirectory(new File(downloadDirectory));
		fileChooser.setTitle("Set download Directory");
		File file = fileChooser.showDialog(stage);
        if (file != null && file.isDirectory()) {
            downloadDirectory = file.getAbsolutePath();
        }
	}//javaClassSearch
	
	/**
	 * Creates a search query URL for the desired engine
	 * @param address The address of the engine(ex. www.google.ca/)
	 * @param query The string being searched
	 * @return The URL needed to make the query
	 */
	public static String getSearchURL(String address, String query)
	{
		String url = "https://"+address+"search?q=";
		String newQuery = "";
		for(int i = 0;i<query.length();i++)
		{
			char c = query.charAt(i);
			switch(c)
			{
			case ' ':newQuery += "+";break;
			case '+':newQuery += "%2B";break;
			case '=':newQuery += "%3D";break;
			case '/':newQuery += "%2F";break;
			case '?':newQuery += "%3F";break;
			case '\\':newQuery += "%5C";break;
			default:newQuery += c;
			}
		}
		url += newQuery;
		return url;
	}//getSearchUrl()
	
	/**
	 * Creates a dialog that asks if the user wants to quit the program
	 */
	public void quitBrowser()
	{
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Quit Confirmation");
		alert.setHeaderText("Are you sure you want to exit Java Browser");
		alert.setContentText("Any webpages you have open will be lost.");
		alert.setResultConverter(dialogButton ->
		{
		    if (dialogButton == ButtonType.OK)
		    {
		        Platform.exit();
		        
		    }
		    return null;
		});
		
		alert.show();
	}//quitBrowser()
	
	/**
	 * Tells the engine to go back one page in the history
	 */
	public void goBack()
	{    
		final WebHistory history=getCurrentWebView().getEngine().getHistory();
		int currentIndex=history.getCurrentIndex();
		if(currentIndex > 0)
		{
			Platform.runLater( () -> { 
	    			history.go(-1); 
			} );
			
	    }
	 }//goBack()
	
	/**
	 * Tells the engine to go forward 1 page
	 */
	public void goForward()
	{    
	    final WebHistory history=getCurrentWebView().getEngine().getHistory();
	    ObservableList<WebHistory.Entry> entryList=history.getEntries();
	    int currentIndex=history.getCurrentIndex();
	    
	    if(currentIndex + 1 < entryList.size())
	    {
	    		Platform.runLater(() ->
	    		{ 
	    			history.go(1); 
	    		});
	    }    
	}//goForward()
	
	/**
	 * Adds a bookmark of the current page to the bookmarks array list
	 */
	public void addBookmark()
	{
		MenuItem item = new MenuItem(getCurrentWebView().getEngine().getLocation());
		item.setOnAction(evt -> {getCurrentWebView().getEngine().load(item.getText());});
		bookmarkMenu.getItems().add(item);
		bookmarkButton.setDisable(true);
	}//addBookmark()
	
	/**Shows or hides the History view
	 * If the view is showing, it will hide it using a set of transitions
	 * If the view is hiding, it will show it using a opposite set of transitions
	 */
	public void hideOrShow()
	{
		Duration d = new Duration(500);
		if(historyView.isVisible())
		{
			historyView.setVisible(true);
			FadeTransition fad = new FadeTransition(d, historyView);
			ScaleTransition scal1 = new ScaleTransition(d, historyView);
			ScaleTransition scal2 = new ScaleTransition(d, historyView);
			ParallelTransition trans = new ParallelTransition(fad, scal1);
			SequentialTransition trans2 = new SequentialTransition(scal2, trans);
			fad.setFromValue(1.0);
			fad.setToValue(0.0);
			scal1.setFromX(0.5);
			scal1.setFromY(1.0);
			scal1.setToX(0.5);
			scal1.setToY(0.0);
			scal2.setFromX(1.0);
			scal2.setFromY(1.0);
			scal2.setToX(0.5);
			scal2.setToY(1.0);
			trans2.setOnFinished(evt->{historyView.setMaxWidth(0);historyView.setVisible(false);});
			trans2.play();

		}
		else
		{
			historyView.setVisible(true);
			historyView.setMaxWidth(450);
			FadeTransition fad = new FadeTransition(d, historyView);
			ScaleTransition scal1 = new ScaleTransition(d, historyView);
			ScaleTransition scal2 = new ScaleTransition(d, historyView);
			ParallelTransition trans = new ParallelTransition(fad, scal1);
			SequentialTransition trans2 = new SequentialTransition(trans, scal2);
			fad.setFromValue(0.0);
			fad.setToValue(1.0);
			scal1.setFromX(0.5);
			scal1.setFromY(0.0);
			scal1.setToX(0.5);
			scal1.setToY(1.0);
			scal2.setFromX(0.5);
			scal2.setFromY(1.0);
			scal2.setToX(1.0);
			scal2.setToY(1.0);
			trans2.play();
			
		}
	}//hideOrShow()
	
	/**
	 * Saves the settings stored to the browser
	 */
	public void saveSettings()
	{
		ArrayList<String> bookmarks = new ArrayList<String>();
		for(MenuItem item:bookmarkMenu.getItems())
		{
			bookmarks.add(item.getText());
		}
		try(ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get("bookmarks.txt")));)
		{
			oos.writeObject(bookmarks);
			oos.close();
		}
		catch (IOException e)
		{e.printStackTrace();}
		try(BufferedWriter oos = Files.newBufferedWriter(Paths.get("settings.txt"));)
		{
			oos.write(new NameValuePair("screenX", String.valueOf(stage.getX())).toString());
			oos.newLine();
			oos.write(new NameValuePair("screenY", String.valueOf(stage.getY())).toString());
			oos.newLine();
			oos.write(new NameValuePair("height", String.valueOf(scene.getHeight())).toString());
			oos.newLine();
			oos.write(new NameValuePair("width", String.valueOf(scene.getWidth())).toString());
			oos.newLine();
			oos.write(new NameValuePair("downloadDirectory", downloadDirectory).toString());
			oos.newLine();
			oos.write(new NameValuePair("homepage", homepage).toString());
		}
		catch (IOException e)
		{e.printStackTrace();}
	}//saveSettings()
	
	/**
	 * Gets the download directory currently being used
	 * @return the directory where downloaded files are stored
	 */
	public static String getDownloadDirectory()
	{
		return downloadDirectory;
	}//getDownloadDirectory()
	
	/**
	 * Reads the settings from a file into the browser
	 */
	@SuppressWarnings("unchecked")
	public void readSettings()
	{
		width=800;
		height=600;
		x=50;
		y=50;
		downloadDirectory="downloads";
		homepage="https://google.ca/";
		if(new File("bookmarks.txt").exists())
		{
		ArrayList<String> bookmarks = new ArrayList<String>();
		try(ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get("bookmarks.txt")));)
		{
			try
			{
				Object o = ois.readObject();
				if(o instanceof ArrayList<?>)
				{
					bookmarks = (ArrayList<String>)o;
				}
			}
			catch (ClassNotFoundException e) {e.printStackTrace();}
			for(String str:bookmarks)
			{
				MenuItem item = new MenuItem(str);
				item.setOnAction(evt -> {getCurrentWebView().getEngine().load(str);});
				bookmarkMenu.getItems().add(item);
			}
		}
		catch (IOException e)
		{e.printStackTrace();}
		}	
		if(new File("settings.txt").exists())
		{
			try(BufferedReader reader = Files.newBufferedReader(Paths.get("settings.txt"));)
			{
				String str = reader.readLine();
				x = Double.parseDouble(str.substring(str.indexOf('=')+1));
				str = reader.readLine();
				y = Double.parseDouble(str.substring(str.indexOf('=')+1));
				str = reader.readLine();
				height = Double.parseDouble(str.substring(str.indexOf('=')+1));
				str = reader.readLine();
				width = Double.parseDouble(str.substring(str.indexOf('=')+1));
				str = reader.readLine();
				downloadDirectory = str.substring(str.indexOf('=')+1);
				str = reader.readLine();
				homepage = str.substring(str.indexOf('=')+1);
			}
			catch(Exception e)
			{e.printStackTrace();}
		}
	}//readSettings()
	
	/**
	 * Executes user defined javascript
	 */
	public void executeScript()
	{
			TextInputDialog dialog = new TextInputDialog(getCurrentWebView().getEngine().getLocation());
			dialog.setTitle("Execute Script");
			dialog.setHeaderText("Execute Specified Javascript");
			dialog.setContentText("What is the javascript you would like to run?");
			
			Optional<String> result = dialog.showAndWait();
			if (result.isPresent())
			{
				getCurrentWebView().getEngine().executeScript(result.get());
			}
	}//executeScript()
	
	/**
	 * Gets the currently used web view
	 * @return the web view currently in use by the user
	 */
	public WebView getCurrentWebView()
	{
		return (WebView)(tabs.getSelectionModel().getSelectedItem().getContent());
	}//getCurrentWebView()
	
	/**
	 * Adds a webview tab to the browser
	 */
	public void addTab()
	{
		Tab tab = new Tab();
		tabs.getTabs().add(tab);
		tab.setText("Tab "+ tabs.getTabs().size());
		/** The view that displays the html of the current loaded page*/
		WebView browserView;
		browserView = new WebView();
		WebEngine engine = browserView.getEngine();
		engine.setOnAlert(evt->{
			
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Alert");
			alert.setContentText(evt.getData());
			alert.show();
		});
		
		engine.getLoadWorker().stateProperty().addListener(( ov, oldState,  newState)->
		{
			bookmarkButton.setDisable(true);
			backButton.setDisable(true);
			forwardButton.setDisable(true);
			addressBar.setText(engine.getLocation());
			if (newState == State.SUCCEEDED)
			{
				tab.setText(getCurrentWebView().getEngine().getTitle());
				if(engine.getHistory().getCurrentIndex() != 0)
				{
					backButton.setDisable(false);
				}
				if(engine.getHistory().getCurrentIndex()+1 < engine.getHistory().getEntries().size())
				{
					forwardButton.setDisable(false);
				}
				if(!isBookmarkPresent(engine.getLocation()))
				{
					bookmarkButton.setDisable(false);
				}
			}
			
		});
		
		// monitor the location url, and if newLoc ends with one of the download file endings, create a new DownloadTask.
			engine.locationProperty().addListener(new ChangeListener<String>() {
				@Override public void changed(ObservableValue<? extends String> observableValue, String oldLoc, String newLocation) {
					if(		   newLocation.endsWith(".exe") || newLocation.endsWith(".pdf") || newLocation.endsWith(".zip") 
							|| newLocation.endsWith(".doc") || newLocation.endsWith(".docx") || newLocation.endsWith(".xls")
							|| newLocation.endsWith(".xlsx") || newLocation.endsWith(".iso") || newLocation.endsWith(".img") 
							|| newLocation.endsWith(".dmg") || newLocation.endsWith(".tar")  || newLocation.endsWith(".tgz")
							|| newLocation.endsWith(".jar"))
					{
						new DownloadBar(newLocation);
						goBack();
					}
				}
			});	
			browserView.setOnKeyPressed(evt->{if(evt.isControlDown()){if(evt.getCode().equals(KeyCode.LEFT)){goBack();}else if(evt.getCode().equals(KeyCode.RIGHT)){goForward();}}});
		
		tab.setContent(browserView);
		engine.load(homepage);
		
	}//addTab()
	
	private class NameValuePair implements Serializable
	{
		static final long serialVersionUID =  58930520L; 
		
		private String name;
		private String value;
		
		public NameValuePair(String name, String value)
		{
			setName(name);
			setValue(value);
		}//NameValuePair()
		
		public void setName(String name){this.name=name;}
		public void setValue(String value){this.value=value;}
		public String getName(){return name;}
		public String getValue(){return value;}
		public String toString(){return getName()+"="+getValue();}
	}
}
