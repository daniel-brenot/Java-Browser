package assignment2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.Optional;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.util.Duration;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.*;
import javafx.event.*;

/**Assignment 2 Web Browser
 * 
 * @author Daniel Brenot
 * @version 2016-03-15
 *
 */
public class MyGui extends Application
{
	
	/** The webengine used to load home pages */
	protected WebEngine engine;
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
	/** The view that displays the html of the current loaded page*/
	protected WebView browserView;
	/** The MenuItem that stores the bookmarks*/
	Menu bookmarkMenu;
	/** The view that shows all of the history of the browser*/
	ListView<Entry> historyView = new ListView<>();
	
	
	/**Calls all the methods in the class
	 * 
	 * @param args The parameters that define how the program launches
	 */
	public static void main(String args[])
	{
		launch(args);
	}//main()
	
	
	@Override
	/**Starts the GUI with the given stage
	 * @param primaryStage The stage used 
	 */
	public void start(Stage primaryStage) throws Exception
	{
		browserView = new WebView();
		engine = browserView.getEngine();
		
		engine.getLoadWorker().stateProperty().addListener(( ov, oldState,  newState)->
		{
			bookmarkButton.setDisable(true);
			backButton.setDisable(true);
			forwardButton.setDisable(true);
			addressBar.setText(engine.getLocation());
			if (newState == State.SUCCEEDED)
			{
				primaryStage.setTitle(engine.getTitle());
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
		
		root = new BorderPane();
		scene = new Scene(root, 800, 600);
		primaryStage.setScene(scene);
		
		VBox topContainer = new VBox();
		HBox addressContainer = new HBox();
		addressContainer.setMaxWidth(Double.MAX_VALUE);
		menuBar = new MenuBar();
		root.setTop(topContainer);
		root.setCenter(browserView);
		
		// Main Menus
		Menu fileMenu = new Menu("File");
		bookmarkMenu = new Menu("Bookmarks");
		Menu helpMenu = new Menu("Help");
		
		//File Menu
		MenuItem quit = new MenuItem("Quit");
		quit.setOnAction(evt -> {quitBrowser();});
		fileMenu.getItems().addAll(quit);
		
		//Help Menu
		MenuItem getHelp = new MenuItem("Get help for java class");getHelp.setOnAction(evt->{javaClassSearch();});
		MenuItem showHistory = new CheckMenuItem("Show History");showHistory.setOnAction(evt->{hideOrShow();});
		MenuItem about = new MenuItem("About");about.setOnAction(evt->{displayAbout();});
		helpMenu.getItems().addAll(getHelp, showHistory, about);			
			
		//Adds all of items to the top of the browser
		menuBar.getMenus().addAll(fileMenu, bookmarkMenu, helpMenu);
		topContainer.getChildren().addAll(menuBar, addressContainer);
		
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
		backButton = new Button("Back");backButton.setOnMouseClicked(responder);
		bookmarkButton = new Button("Add Bookmark");bookmarkButton.setOnMouseClicked(responder);
		forwardButton = new Button("Forward");forwardButton.setOnMouseClicked(responder);
		addressBar = new TextField("https://www.google.ca/");addressBar.setOnMouseClicked(responder);
		
		//Causes the address bar to take up the maximum width it can
		addressBar.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(addressBar, Priority.ALWAYS);
		addressBar.setOnKeyPressed(evt -> {if(evt.getCode().equals(KeyCode.ENTER)){engine.load(addressBar.getText());}});
		addressContainer.getChildren().addAll(backButton, addressBar, bookmarkButton, forwardButton);
		//Creates the history view and sets its properties
		createHistoryView();
		
		//Shows the program on the screen
		primaryStage.show();
		
		//Loads the defined home page
		engine.load("https://google.ca/");
	}
	
	/**Creates a List View that displays all the webpages visited
	 * 
	 */
	public void createHistoryView()
	{
		historyView = new ListView<>(engine.getHistory().getEntries());
		historyView.setPrefWidth(450);
		historyView.setOnMouseClicked(evt->
		{engine.getHistory().go(historyView.getSelectionModel().getSelectedIndex() - engine.getHistory().getCurrentIndex());});
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
	}//isBookmarkPresent
	
	/** Displays the information about the current version of the software
	 * 
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
	
	/** Displays a dialog that will search google for the specified String
	 * 
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
			engine.load(getSearchURL("www.google.ca/", "java " + result.get()));
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
	
	/**Creates a dialog that asks if the user wants to quit the program
	 * 
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
		final WebHistory history=engine.getHistory();
		ObservableList<WebHistory.Entry> entryList=history.getEntries();
		int currentIndex=history.getCurrentIndex();
		if(currentIndex > 0)
		{
			Platform.runLater( () -> { 
	    			history.go(-1); 
	    			final String nextAddress = history.getEntries().get(currentIndex - 1).getUrl();
			} );
			
	    }
	 }//goBack()
	
	/**
	 * Tells the engine to go forward 1 page
	 */
	public void goForward()
	{    
	    final WebHistory history=engine.getHistory();
	    ObservableList<WebHistory.Entry> entryList=history.getEntries();
	    int currentIndex=history.getCurrentIndex();
	    
	    if(currentIndex + 1 < entryList.size())
	    {
	    		Platform.runLater(() ->
	    		{ 
	    			history.go(1); 
	    			final String nextAddress = history.getEntries().get(currentIndex + 1).getUrl();
	    		});
	    }    
	}//goForward()
	
	/**Adds a bookmark of the current page to the bookmarks array list
	 * 
	 */
	public void addBookmark()
	{
		MenuItem item = new MenuItem(engine.getLocation());
		item.setOnAction(evt -> {engine.load(item.getText());});
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

}