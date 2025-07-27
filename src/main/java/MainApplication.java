/**
 * Income File Import System (IFIS)  - Main JavaFX Application
 * Government Tax Department Digital Initiative
 */

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainApplication extends Application {

    // Core components
    private Stage primaryStage;
    private FileProcessor fileProcessor;
    private ChecksumValidator checksumValidator;
    private TaxCalculator taxCalculator;

    // UI Components
    private TextField filePathField;
    private TableView<IncomeRecord> recordsTable;
    private ObservableList<IncomeRecord> recordsList;
    private Label totalRecordsLabel;
    private Label validRecordsLabel;
    private Label invalidRecordsLabel;
    private Label taxPayableLabel;
    private Button loadFileButton;
    private Button validateButton;
    private Button deleteInvalidButton;
    private Button calculateTaxButton;
    private Button saveChangesButton;
    private TextArea logArea;

    // Data
    private List<IncomeRecord> allRecords;
    private List<IncomeRecord> validRecords;
    private List<IncomeRecord> invalidRecords;

    // Formatters
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        initializeComponents();

        // Create the main layout (this creates all UI components)
        VBox mainLayout = createMainLayout();

        // Now setup UI state and bind event handlers
        setupUI();
        bindEventHandlers();

        primaryStage.setTitle("Income File Import System (IFIS)  - Tax Department");
        primaryStage.setScene(new Scene(mainLayout, 1200, 800));
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        logMessage("IFIS  started successfully. Ready to import income files.");
    }

    private void initializeComponents() {
        fileProcessor = new FileProcessor();
        checksumValidator = new ChecksumValidator();
        taxCalculator = new TaxCalculator();

        recordsList = FXCollections.observableArrayList();
        allRecords = new ArrayList<>();
        validRecords = new ArrayList<>();
        invalidRecords = new ArrayList<>();
    }

    private VBox createMainLayout() {
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        // Header
        mainLayout.getChildren().add(createHeader());

        // File input section
        mainLayout.getChildren().add(createFileInputSection());

        // Main content area
        HBox contentArea = new HBox(10);
        VBox tableSection = createTableSection();
        VBox controlsSection = createControlsSection();

        contentArea.getChildren().addAll(tableSection, controlsSection);
        HBox.setHgrow(tableSection, Priority.ALWAYS);

        mainLayout.getChildren().add(contentArea);

        // Statistics section
        mainLayout.getChildren().add(createStatisticsSection());

        // Log section
        mainLayout.getChildren().add(createLogSection());

        VBox.setVgrow(contentArea, Priority.ALWAYS);

        return mainLayout;
    }

    private VBox createHeader() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Income File Import System (IFIS) ");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c5f8a;");

        Label subtitleLabel = new Label("Government Tax Department - Digitizing Initiative");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #5a5a5a;");

        header.getChildren().addAll(titleLabel, subtitleLabel);

        Separator separator = new Separator();
        header.getChildren().add(separator);

        return header;
    }

    private HBox createFileInputSection() {
        HBox fileSection = new HBox(10);
        fileSection.setAlignment(Pos.CENTER_LEFT);
        fileSection.setPadding(new Insets(10));
        fileSection.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-radius: 5;");

        Label fileLabel = new Label("Income File Path:");
        fileLabel.setMinWidth(120);

        filePathField = new TextField();
        filePathField.setPromptText("Select income sheet CSV file...");
        filePathField.setPrefWidth(400);
        HBox.setHgrow(filePathField, Priority.ALWAYS);

        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> browseForFile());

        loadFileButton = new Button("Load File");
        loadFileButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        loadFileButton.setOnAction(e -> loadFile());

        fileSection.getChildren().addAll(fileLabel, filePathField, browseButton, loadFileButton);

        return fileSection;
    }

    private VBox createTableSection() {
        VBox tableSection = new VBox(5);

        Label tableLabel = new Label("Imported Income Records");
        tableLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        recordsTable = createRecordsTable();

        tableSection.getChildren().addAll(tableLabel, recordsTable);
        VBox.setVgrow(recordsTable, Priority.ALWAYS);

        return tableSection;
    }

    private TableView<IncomeRecord> createRecordsTable() {
        TableView<IncomeRecord> table = new TableView<>();
        table.setEditable(true);
        table.setItems(recordsList);

        // Income Code Column
        TableColumn<IncomeRecord, String> codeColumn = new TableColumn<>("Income Code");
        codeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIncomeCode()));
        codeColumn.setPrefWidth(100);

        // Description Column
        TableColumn<IncomeRecord, String> descColumn = new TableColumn<>("Description");
        descColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription()));
        descColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        descColumn.setOnEditCommit(e -> {
            IncomeRecord record = e.getRowValue();
            record.setDescription(e.getNewValue());
            updateRecordChecksum(record);
            refreshValidation();
        });
        descColumn.setPrefWidth(150);

        // Date Column
        TableColumn<IncomeRecord, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDate()));
        dateColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        dateColumn.setOnEditCommit(e -> {
            IncomeRecord record = e.getRowValue();
            record.setDate(e.getNewValue());
            updateRecordChecksum(record);
            refreshValidation();
        });
        dateColumn.setPrefWidth(100);

        // Income Amount Column
        TableColumn<IncomeRecord, String> incomeColumn = new TableColumn<>("Income Amount");
        incomeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(currencyFormat.format(cellData.getValue().getIncomeAmount())));
        incomeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        incomeColumn.setOnEditCommit(e -> {
            IncomeRecord record = e.getRowValue();
            try {
                double newValue = Double.parseDouble(e.getNewValue().replace(",", ""));
                record.setIncomeAmount(newValue);
                updateRecordChecksum(record);
                refreshValidation();
                table.refresh();
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid number format: " + e.getNewValue(), Alert.AlertType.ERROR);
                table.refresh();
            }
        });
        incomeColumn.setPrefWidth(120);

        // WHT Amount Column
        TableColumn<IncomeRecord, String> whtColumn = new TableColumn<>("WHT Amount");
        whtColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(currencyFormat.format(cellData.getValue().getWhtAmount())));
        whtColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        whtColumn.setOnEditCommit(e -> {
            IncomeRecord record = e.getRowValue();
            try {
                double newValue = Double.parseDouble(e.getNewValue().replace(",", ""));
                record.setWhtAmount(newValue);
                updateRecordChecksum(record);
                refreshValidation();
                table.refresh();
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid number format: " + e.getNewValue(), Alert.AlertType.ERROR);
                table.refresh();
            }
        });
        whtColumn.setPrefWidth(120);

        // Original Checksum Column
        TableColumn<IncomeRecord, String> originalChecksumColumn = new TableColumn<>("Original Checksum");
        originalChecksumColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getOriginalChecksum())));
        originalChecksumColumn.setPrefWidth(120);

        // Calculated Checksum Column
        TableColumn<IncomeRecord, String> calculatedChecksumColumn = new TableColumn<>("Calculated Checksum");
        calculatedChecksumColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getCalculatedChecksum())));
        calculatedChecksumColumn.setPrefWidth(130);

        // Valid Column
        TableColumn<IncomeRecord, String> validColumn = new TableColumn<>("Valid");
        validColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isValid() ? "✓" : "✗"));
        validColumn.setCellFactory(column -> {
            return new TableCell<IncomeRecord, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if ("✓".equals(item)) {
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        }
                    }
                }
            };
        });
        validColumn.setPrefWidth(60);

        // Add all columns to table
        table.getColumns().add(codeColumn);
        table.getColumns().add(descColumn);
        table.getColumns().add(dateColumn);
        table.getColumns().add(incomeColumn);
        table.getColumns().add(whtColumn);
        table.getColumns().add(originalChecksumColumn);
        table.getColumns().add(calculatedChecksumColumn);
        table.getColumns().add(validColumn);

        return table;
    }

    private VBox createControlsSection() {
        VBox controlsSection = new VBox(10);
        controlsSection.setPrefWidth(200);
        controlsSection.setPadding(new Insets(10));
        controlsSection.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #d0d0d0; -fx-border-radius: 5;");

        Label controlsLabel = new Label("Actions");
        controlsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Initialize all buttons and assign to class fields
        validateButton = new Button("Validate Records");
        validateButton.setPrefWidth(180);
        validateButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        validateButton.setOnAction(e -> validateRecords());

        deleteInvalidButton = new Button("Delete Invalid Records");
        deleteInvalidButton.setPrefWidth(180);
        deleteInvalidButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        deleteInvalidButton.setOnAction(e -> deleteInvalidRecords());

        calculateTaxButton = new Button("Calculate Tax Payable");
        calculateTaxButton.setPrefWidth(180);
        calculateTaxButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        calculateTaxButton.setOnAction(e -> calculateTaxPayable());

        saveChangesButton = new Button("Save Changes");
        saveChangesButton.setPrefWidth(180);
        saveChangesButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        saveChangesButton.setOnAction(e -> saveChanges());

        Button clearAllButton = new Button("Clear All");
        clearAllButton.setPrefWidth(180);
        clearAllButton.setOnAction(e -> clearAllData());

        Button aboutButton = new Button("About IFIS");
        aboutButton.setPrefWidth(180);
        aboutButton.setOnAction(e -> showAboutDialog());

        controlsSection.getChildren().addAll(
                controlsLabel,
                new Separator(),
                validateButton,
                deleteInvalidButton,
                calculateTaxButton,
                saveChangesButton,
                new Separator(),
                clearAllButton,
                aboutButton
        );

        return controlsSection;
    }

    private HBox createStatisticsSection() {
        HBox statsSection = new HBox(20);
        statsSection.setPadding(new Insets(10));
        statsSection.setAlignment(Pos.CENTER);
        statsSection.setStyle("-fx-background-color: #e8f4f8; -fx-border-color: #b0d4e3; -fx-border-radius: 5;");

        totalRecordsLabel = new Label("Total Records: 0");
        totalRecordsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        validRecordsLabel = new Label("Valid: 0");
        validRecordsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: green;");

        invalidRecordsLabel = new Label("Invalid: 0");
        invalidRecordsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: red;");

        taxPayableLabel = new Label("Tax Payable: Rs 0.00");
        taxPayableLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c5f8a;");

        statsSection.getChildren().addAll(
                totalRecordsLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                validRecordsLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                invalidRecordsLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                taxPayableLabel
        );

        return statsSection;
    }

    private VBox createLogSection() {
        VBox logSection = new VBox(5);

        Label logLabel = new Label("System Log");
        logLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        logSection.getChildren().addAll(logLabel, logArea);

        return logSection;
    }

    private void setupUI() {
        // Initial UI state
        validateButton.setDisable(true);
        deleteInvalidButton.setDisable(true);
        calculateTaxButton.setDisable(true);
        saveChangesButton.setDisable(true);
    }

    private void bindEventHandlers() {
        // Table selection handler
        recordsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        logMessage("Selected record: " + newSelection.getIncomeCode());
                    }
                }
        );
    }

    private void browseForFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Income Sheet CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
            logMessage("Selected file: " + selectedFile.getName());
        }
    }

    private void loadFile() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            showAlert("Error", "Please select a file to load.", Alert.AlertType.ERROR);
            return;
        }

        try {
            logMessage("Loading file: " + filePath);

            List<IncomeRecord> records = fileProcessor.readIncomeFile(filePath);

            if (records != null && !records.isEmpty()) {
                allRecords.clear();
                allRecords.addAll(records);

                recordsList.clear();
                recordsList.addAll(records);

                logMessage("Successfully loaded " + records.size() + " records");

                // Update UI state
                validateButton.setDisable(false);
                saveChangesButton.setDisable(false);

                // Auto-validate after loading
                validateRecords();

                updateStatistics();

            } else {
                showAlert("Error", "No records found in the file or file format is invalid.", Alert.AlertType.ERROR);
                logMessage("ERROR: Failed to load records from file");
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to load file: " + e.getMessage(), Alert.AlertType.ERROR);
            logMessage("ERROR: " + e.getMessage());
        }
    }

    private void validateRecords() {
        if (allRecords.isEmpty()) {
            showAlert("Warning", "No records to validate.", Alert.AlertType.WARNING);
            return;
        }

        logMessage("Validating " + allRecords.size() + " records...");

        validRecords.clear();
        invalidRecords.clear();

        for (IncomeRecord record : allRecords) {
            boolean isValid = checksumValidator.validateRecord(record);
            record.setValid(isValid);

            if (isValid) {
                validRecords.add(record);
            } else {
                invalidRecords.add(record);
            }
        }

        logMessage("Validation complete: " + validRecords.size() + " valid, " + invalidRecords.size() + " invalid");

        // Update UI
        recordsTable.refresh();
        updateStatistics();

        deleteInvalidButton.setDisable(invalidRecords.isEmpty());
        calculateTaxButton.setDisable(validRecords.isEmpty());
    }

    private void deleteInvalidRecords() {
        if (invalidRecords.isEmpty()) {
            showAlert("Information", "No invalid records to delete.", Alert.AlertType.INFORMATION);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Invalid Records");
        confirmAlert.setContentText("Are you sure you want to delete " + invalidRecords.size() + " invalid records?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            // Remove invalid records from all collections
            allRecords.removeAll(invalidRecords);
            recordsList.removeAll(invalidRecords);

            logMessage("Deleted " + invalidRecords.size() + " invalid records");

            invalidRecords.clear();

            // Refresh UI
            recordsTable.refresh();
            updateStatistics();
            deleteInvalidButton.setDisable(true);
        }
    }

    private void calculateTaxPayable() {
        if (validRecords.isEmpty()) {
            showAlert("Warning", "No valid records to calculate tax.", Alert.AlertType.WARNING);
            return;
        }

        try {
            double taxPayable = taxCalculator.calculateTaxPayable(validRecords);

            taxPayableLabel.setText("Tax Payable: Rs " + currencyFormat.format(taxPayable));

            logMessage("Tax calculated: Rs " + currencyFormat.format(taxPayable) +
                    " for " + validRecords.size() + " valid records");

            // Show detailed calculation
            showTaxCalculationDialog(taxPayable);

        } catch (Exception e) {
            showAlert("Error", "Failed to calculate tax: " + e.getMessage(), Alert.AlertType.ERROR);
            logMessage("ERROR calculating tax: " + e.getMessage());
        }
    }

    private void showTaxCalculationDialog(double taxPayable) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tax Calculation Details");
        alert.setHeaderText("Tax Payable Calculation");

        double totalIncome = validRecords.stream().mapToDouble(IncomeRecord::getIncomeAmount).sum();
        double totalWHT = validRecords.stream().mapToDouble(IncomeRecord::getWhtAmount).sum();
        double taxableIncome = Math.max(0, totalIncome - 150000);
        double grossTax = taxableIncome * 0.12;

        String details = String.format(
                "Total Income: Rs %s\n" +
                        "Tax-Free Threshold: Rs 150,000.00\n" +
                        "Taxable Income: Rs %s\n" +
                        "Gross Tax (12%%): Rs %s\n" +
                        "Less: WHT Paid: Rs %s\n" +
                        "Net Tax Payable: Rs %s",
                currencyFormat.format(totalIncome),
                currencyFormat.format(taxableIncome),
                currencyFormat.format(grossTax),
                currencyFormat.format(totalWHT),
                currencyFormat.format(taxPayable)
        );

        alert.setContentText(details);
        alert.showAndWait();
    }

    private void saveChanges() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            showAlert("Error", "No file path specified.", Alert.AlertType.ERROR);
            return;
        }

        try {
            // Add _updated suffix to filename
            String newPath = filePath.replace(".csv", "_updated.csv");

            boolean success = fileProcessor.writeIncomeFile(newPath, allRecords);

            if (success) {
                logMessage("Changes saved to: " + newPath);
                showAlert("Success", "Changes saved successfully to:\n" + newPath, Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to save changes.", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to save changes: " + e.getMessage(), Alert.AlertType.ERROR);
            logMessage("ERROR saving changes: " + e.getMessage());
        }
    }

    private void clearAllData() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Clear");
        confirmAlert.setHeaderText("Clear All Data");
        confirmAlert.setContentText("Are you sure you want to clear all loaded data?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            allRecords.clear();
            validRecords.clear();
            invalidRecords.clear();
            recordsList.clear();

            filePathField.clear();

            updateStatistics();

            validateButton.setDisable(true);
            deleteInvalidButton.setDisable(true);
            calculateTaxButton.setDisable(true);
            saveChangesButton.setDisable(true);

            logMessage("All data cleared");
        }
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About IFIS ");
        alert.setHeaderText("Income File Import System ");
        alert.setContentText(
                "Government Tax Department - Digitizing Initiative\n\n" +
                        "Features:\n" +
                        "• Import income CSV files with checksum validation\n" +
                        "• Edit and validate income records\n" +
                        "• Calculate tax payable based on Sri Lankan tax law\n" +
                        "• Delete invalid records\n" +
                        "• Export updated files\n\n"
        );
        alert.showAndWait();
    }

    private void updateRecordChecksum(IncomeRecord record) {
        int newChecksum = checksumValidator.calculateChecksum(record);
        record.setCalculatedChecksum(newChecksum);
        logMessage("Updated checksum for " + record.getIncomeCode() + ": " + newChecksum);
    }

    private void refreshValidation() {
        validateRecords();
    }

    private void updateStatistics() {
        totalRecordsLabel.setText("Total Records: " + allRecords.size());
        validRecordsLabel.setText("Valid: " + validRecords.size());
        invalidRecordsLabel.setText("Invalid: " + invalidRecords.size());

        if (validRecords.isEmpty()) {
            taxPayableLabel.setText("Tax Payable: Rs 0.00");
        }
    }

    private void logMessage(String message) {
        String timestamp = java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        );
        String logEntry = "[" + timestamp + "] " + message + "\n";
        logArea.appendText(logEntry);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}