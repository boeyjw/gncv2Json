package fx.main;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import sql.tojson.RunApp;

public class Controller implements Initializable{
	
	private List<String> cliargs;
	
	//Main anchor to entire application
    @FXML
    private AnchorPane mainAnchor;
    
    //Splitter for interface
    @FXML
    private AnchorPane argsSplit;
    @FXML
    private SplitPane argsInputSplit;
    @FXML
    private AnchorPane reqargsAnchor;
    @FXML
    private SplitPane optargsSplit;
    
    //All required arguments
    @FXML
    private GridPane reqargsGrid;
    @FXML
    private TextField sqlus;
    @FXML
    private TextField sqldb;
    @FXML
    private PasswordField sqlpw;
    @FXML
    private ComboBox<String> parsedt;
    
    //All optional arguments
    @FXML
    private AnchorPane optargsAnchor;
    @FXML
    private Accordion optAccordion;
    @FXML
    private TitledPane procTitled;
    @FXML
    private TextField sqlpr;
    @FXML
    private TextField sqlsn;
    @FXML
    private TextField sqlba;
    @FXML
    private TextField docbr;
    @FXML
    private CheckBox sernull;
    @FXML
    private TitledPane outputTitled;
    @FXML
    private CheckBox dmdb;
    @FXML
    private TextField outputfn;
    @FXML
    private TextField muri;
    @FXML
    private TextField mpw;
    @FXML
    private TextField mdb;
    @FXML
    private TextField mcol;
    
    //Execution buttons
    @FXML
    private Button exit;
    @FXML
    private Button reset;
    @FXML
    private Button execute;
    
    //Execution time
    @FXML
    private StackPane execStack;
    @FXML
    private ProgressIndicator execpi;
    @FXML
    private Label batchval;
    
    private CLIService backApp;
    
    //Integer only validation regex
    private Pattern digitPat;
	private Matcher digitmat;
	
	//Specific validity checks
	private boolean validsqldb;
	private boolean validparsedt;
	private boolean validsqlpr;
	private boolean validsqlba;
	private boolean validdocbr;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		execStack.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
		hideOverlay();
		if(!Main.param.isEmpty())
			executeRunApp(Main.param.toArray(new String[0]));
		else {
			cliargs = new ArrayList<String>();
			backApp = new CLIService();
			
			digitPat = Pattern.compile("\\d+");
			digitmat = digitPat.matcher("");
			
			validsqldb = false;
			validparsedt = false;
			validdocbr = true;
			validsqlba = true;
			validsqlpr = true;
			
			initReqArgs();
			initOptArgsProcessing();
			initOptArgsOutput();
			execute.setOnAction((event) -> {
				execApp();
			});
		}
	}
	
	private void hideOverlay() {
		execStack.setDisable(true);
		execStack.setVisible(false);
	}
	
	private void showOverlay() {
		execStack.setDisable(false);
		execStack.setVisible(true);
	}
	
	private void initReqArgs() {
		sqlus.setPromptText("root");
		sqlus.setTooltip(new Tooltip("Enter SQL username. Defaults to root."));
		
		sqlpw.setTooltip(new Tooltip("Enter SQL password. Defaults to no password."));
		
		sqldb.setTooltip(new Tooltip("Enter SQL database to access. Must have an entry."));
		sqldb.focusedProperty().addListener((arg0, oldValue, newValue) -> {
			if(!newValue) {
				if(sqldb.getText() == null || sqldb.getText().trim().isEmpty()) {
					validsqldb = false;
					if(!sqldb.getStyleClass().contains("error"))
						sqldb.getStyleClass().add("error");
				}
				else {
					validsqldb = true;
					sqldb.getStyleClass().remove("error");
					mdb.setPromptText(sqldb.getText().trim());
				}
			}
		});
		
		parsedt.setValue("Parse?");
		parsedt.setItems(FXCollections.observableList(Arrays.asList((new String[]{"ncbi", "gbif", "acc <deprecated>", "merge"}))));
		parsedt.focusedProperty().addListener((arg0, oldValue, newValue) -> {
			outputfn.setPromptText(parsedt.getValue() + "-out");
			mcol.setPromptText(parsedt.getValue());
			if(newValue)
				validparsedt = true;
		});
	}
	
	private void initOptArgsProcessing() {
		sqlpr.setTooltip(new Tooltip("Enter SQL port. Defaults to 3306."));
		sqlpr.setPromptText("3306");
		sqlpr.focusedProperty().addListener((arg0, oldValue, newValue) -> {
			if(!newValue) {
				digitmat.reset(sqlpr.getText());
				if(!sqlpr.getText().trim().isEmpty() && !digitmat.matches()) {
					validsqlpr = false;
					if(!sqlpr.getStyleClass().contains("error"))
						sqlpr.getStyleClass().add("error");
				}
				else {
					validsqlpr = true;
					sqlpr.getStyleClass().remove("error");
				}
			}
		});
		
		sqlsn.setTooltip(new Tooltip("Enter SQL host. Defaults to localhost."));
		sqlsn.setPromptText("localhost");
		
		sqlba.setTooltip(new Tooltip("Enter batch size to process. Large batch size consumes more memory. "
				+ "Small batch size reduces performance. Defaults to 200000"));
		sqlba.setPromptText("200000");
		sqlba.focusedProperty().addListener((arg0, oldValue, newValue) -> {
			if(!newValue) {
				digitmat.reset(sqlba.getText());
				if(!sqlba.getText().trim().isEmpty() && !digitmat.matches()) {
					validsqlba = false;
					if(!sqlba.getStyleClass().contains("error"))
						sqlba.getStyleClass().add("error");
				}
				else {
					validsqlba = true;
					sqlba.getStyleClass().remove("error");
				}
			}
		});
		
		docbr.setTooltip(new Tooltip("Enter max rows to process. Defaults to ALL."));
		docbr.focusedProperty().addListener((arg0, oldValue, newValue) -> {
			if(!newValue) {
				digitmat.reset(docbr.getText());
				if(!docbr.getText().trim().isEmpty() && !digitmat.matches()) {
					validdocbr = false;
					if(!docbr.getStyleClass().contains("error"))
						docbr.getStyleClass().add("error");
				}
				else {
					validdocbr = true;
					docbr.getStyleClass().remove("error");
				}
			}
		});
		
		sernull.setSelected(false);
	}
	
	private void initOptArgsOutput() {
		dmdb.setSelected(false);
		muri.setDisable(true);
		mpw.setDisable(true);
		mdb.setDisable(true);
		mcol.setDisable(true);
		dmdb.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if(dmdb.isSelected()) {
					outputfn.setDisable(true);
					muri.setDisable(false);
					mdb.setDisable(false);
					mcol.setDisable(false);
				}
				else {
					outputfn.setDisable(false);
					muri.setDisable(true);
					mdb.setDisable(true);
					mcol.setDisable(true);
				}
			}
		});
		
		outputfn.setTooltip(new Tooltip("Enter output filename. Defaults to *databasetype*.json."));
		
		muri.setTooltip(new Tooltip("Enter or paste URI here. Defaults to mongodb://localhost:27017."));
		muri.setPromptText("mongodb://localhost:27017");
		
		muri.focusedProperty().addListener((arg0, oldValue, newValue) -> {
			if(!newValue) {
				if(muri.getText() == null || muri.getText().trim().isEmpty())
					mpw.setDisable(true);
				else
					mpw.setDisable(false);
			}
		});
		
		mpw.setTooltip(new Tooltip("Enter MongoDB password here. Only applicable if you use URI."));
		
		mdb.setTooltip(new Tooltip("Enter MongoDB database to insert into. Defaults to *database name*."));
		
		mcol.setTooltip(new Tooltip("Enter MongoDB collection to insert into. Defaults to *database type*."));
	}
	
	private void executeRunApp(String[] args) {
		if(args.length >= 4)
			RunApp.main(args);
		else
			System.err.println("Insufficient arguments.");
	}
    
    private void execApp() {
		if(validdocbr && validparsedt && validsqlba && validsqldb && validsqlpr) {
			//Required
			addCLIargs("-us", sqlus.getText() == null || sqlus.getText().trim().isEmpty() ? sqlus.getPromptText() : sqlus.getText().trim());
			addCLIargs("-pw", sqlpw.getText() == null || sqlpw.getText().trim().isEmpty() ? "" : sqlpw.getText().trim());
			addCLIargs("-db", sqldb.getText().trim());
			addCLIargs("-dt", parsedt.getValue());
			
			//Optional processing
			if(sqlpr.getText() != null && !sqlpr.getText().trim().isEmpty())
				addCLIargs("-pr", sqlpr.getText().trim());
			if(sqlsn.getText() != null && !sqlsn.getText().trim().isEmpty())
				addCLIargs("-sn", sqlsn.getText().trim());
			if(sqlba.getText() != null && !sqlba.getText().trim().isEmpty())
				addCLIargs("-ba", sqlba.getText().trim());
			if(docbr.getText() != null && !docbr.getText().trim().isEmpty())
				addCLIargs("-br", docbr.getText().trim());
			if(sernull.isSelected())
				cliargs.add("-sernull");
			
			//Optional output
			if(dmdb.isSelected()) {
				cliargs.add("-dmdb");
				if(muri.getText() != null && !muri.getText().trim().isEmpty() && !muri.isDisabled())
					addCLIargs("-muri", muri.getText().trim());
				if(mpw.getText() != null && !mpw.getText().trim().isEmpty() && !mpw.isDisabled())
					addCLIargs("-mpw", mpw.getText().trim());
				if(mdb.getText() != null && !mdb.getText().trim().isEmpty() && !mdb.isDisabled())
					addCLIargs("-mdb", mdb.getText().trim());
				if(mcol.getText() != null && !mcol.getText().trim().isEmpty() && !mcol.isDisabled())
					addCLIargs("-mcol", mcol.getText().trim());
			}
			
			showOverlay();
			launchConsole();
			backApp.setParams(cliargs.toArray(new String[0]));
			backApp.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
					hideOverlay();
					System.out.println("Success!");
				}
			});
			backApp.setOnRunning(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
					showOverlay();
					System.out.println("Running...");
				}
			});
			backApp.setOnFailed(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
					System.out.println("Failed!");
				}
			});
			backApp.start();
		}
		else {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Argument mismatch!");
			alert.setContentText("Please check your inputs for red boxes!"
					.concat(validsqldb ? "" : "\nMust enter *SQL database name*!")
					.concat(validparsedt ? "" : "\nMust select a *database type*!")
					.concat(validdocbr ? "" : "\nInvalid *break at* input! Only accepts numbers.")
					.concat(validsqlba ? "" : "\nInvalid *batch size* input! Only accepts numbers.")
					.concat(validsqlpr ? "" : "\nInvalid *SQL port* input! Only accepts numbers."));
			alert.showAndWait();
		}
    }
    
    private void launchConsole() {
    	Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource("gncv2Json_console.fxml"));
            Stage stage = new Stage();
            stage.setTitle("gncv2Json Console Log");
            stage.setScene(new Scene(root));
            stage.show();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	private void addCLIargs(String flag, String value) {
		cliargs.add(flag);
		cliargs.add(value);
	}

    @FXML
    private void quitApp() {
    	System.exit(0);
    }

    @FXML
    private void resetFields() {
    	sqlus.clear();
    	sqlpw.clear();
    	sqldb.clear();
    	sqlpr.clear();
    	sqlsn.clear();
    	sqlba.clear();
    	docbr.clear();
    	parsedt.setValue("Parse?");
    	sernull.setSelected(false);
    	dmdb.setSelected(false);
    	outputfn.clear();
    	muri.clear();
    	mdb.clear();
    	mcol.clear();
    	
    	cliargs.clear();
    	
    	initReqArgs();
    	initOptArgsProcessing();
    	initOptArgsOutput();
    }
}
