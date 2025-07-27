/**
 * File Processor Class 
 * Handles reading and writing income CSV files
 */

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileProcessor {

    // Constants
    private static final String CSV_HEADER = "Income_Code,Description,Date,Income_Amount,WHT_Amount,Checksum";
    private static final String BACKUP_SUFFIX = "_backup";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Constructor
    public FileProcessor() {
        // Initialize file processor
    }

    /**
     * Read income records from CSV file
     *
     * @param filePath Path to the CSV file
     * @return List of IncomeRecord objects
     * @throws IOException If file reading fails
     */
    public List<IncomeRecord> readIncomeFile(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        if (!isValidCsvFile(filePath)) {
            throw new IllegalArgumentException("File must be a CSV file");
        }

        List<IncomeRecord> records = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            boolean headerProcessed = false;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Skip header line
                if (!headerProcessed) {
                    if (isHeaderLine(line)) {
                        headerProcessed = true;
                        continue;
                    } else {
                        // No header found, treat first line as data
                        headerProcessed = true;
                    }
                }

                try {
                    IncomeRecord record = parseRecordFromCsvLine(line);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    String error = String.format("Line %d: %s - %s", lineNumber, e.getMessage(), line);
                    errorMessages.add(error);
                    System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        // Log parsing results
        System.out.println("File parsing completed:");
        System.out.println("  Records loaded: " + records.size());
        System.out.println("  Errors encountered: " + errorMessages.size());

        if (!errorMessages.isEmpty()) {
            System.err.println("Parsing errors:");
            for (String error : errorMessages) {
                System.err.println("  " + error);
            }
        }

        return records;
    }

    /**
     * Write income records to CSV file
     *
     * @param filePath Path to write the CSV file
     * @param records List of IncomeRecord objects to write
     * @return true if successful, false otherwise
     */
    public boolean writeIncomeFile(String filePath, List<IncomeRecord> records) {
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("File path cannot be null or empty");
            return false;
        }

        if (records == null) {
            System.err.println("Records list cannot be null");
            return false;
        }

        try {
            Path path = Paths.get(filePath);

            // Create directories if they don't exist
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            // Create backup if file exists
            if (Files.exists(path)) {
                createBackup(filePath);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                // Write header
                writer.write(CSV_HEADER);
                writer.newLine();

                // Write records
                for (IncomeRecord record : records) {
                    writer.write(record.toCsvLine());
                    writer.newLine();
                }
            }

            System.out.println("Successfully wrote " + records.size() + " records to: " + filePath);
            return true;

        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error writing file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Parse a single CSV line into an IncomeRecord
     *
     * @param csvLine CSV line to parse
     * @return IncomeRecord object or null if parsing fails
     * @throws IllegalArgumentException If line format is invalid
     */
    private IncomeRecord parseRecordFromCsvLine(String csvLine) throws IllegalArgumentException {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV line cannot be null or empty");
        }

        // Split by comma, but handle commas in quoted fields
        String[] parts = splitCsvLine(csvLine);

        if (parts.length < 5) {
            throw new IllegalArgumentException("CSV line must have at least 5 fields");
        }

        try {
            String incomeCode = cleanCsvValue(parts[0]);
            String description = cleanCsvValue(parts[1]);
            String date = cleanCsvValue(parts[2]);
            double incomeAmount = parseDouble(parts[3]);
            double whtAmount = parseDouble(parts[4]);

            int originalChecksum = 0;
            if (parts.length >= 6 && !parts[5].trim().isEmpty()) {
                originalChecksum = parseInt(parts[5]);
            }

            return new IncomeRecord(incomeCode, description, date, incomeAmount, whtAmount, originalChecksum);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Validation error: " + e.getMessage());
        }
    }

    /**
     * Split CSV line handling quoted fields
     *
     * @param line CSV line to split
     * @return Array of field values
     */
    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // Add the last field
        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }

    /**
     * Clean CSV value by removing quotes and trimming
     *
     * @param value Raw CSV field value
     * @return Cleaned value
     */
    private String cleanCsvValue(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        // Remove surrounding quotes if present
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned.trim();
    }

    /**
     * Parse double value from string
     *
     * @param value String value to parse
     * @return Parsed double value
     * @throws NumberFormatException If parsing fails
     */
    private double parseDouble(String value) throws NumberFormatException {
        if (value == null || value.trim().isEmpty()) {
            throw new NumberFormatException("Empty value cannot be parsed as double");
        }

        String cleaned = cleanCsvValue(value);
        return Double.parseDouble(cleaned);
    }

    /**
     * Parse integer value from string
     *
     * @param value String value to parse
     * @return Parsed integer value
     * @throws NumberFormatException If parsing fails
     */
    private int parseInt(String value) throws NumberFormatException {
        if (value == null || value.trim().isEmpty()) {
            throw new NumberFormatException("Empty value cannot be parsed as integer");
        }

        String cleaned = cleanCsvValue(value);
        return Integer.parseInt(cleaned);
    }

    /**
     * Check if a line is a header line
     *
     * @param line Line to check
     * @return true if it's a header line
     */
    private boolean isHeaderLine(String line) {
        if (line == null) {
            return false;
        }

        String lowerLine = line.toLowerCase();
        return lowerLine.contains("income_code") ||
                lowerLine.contains("description") ||
                lowerLine.contains("checksum");
    }

    /**
     * Validate if file is a valid CSV file
     *
     * @param filePath File path to validate
     * @return true if valid CSV file
     */
    private boolean isValidCsvFile(String filePath) {
        if (filePath == null) {
            return false;
        }

        return filePath.toLowerCase().endsWith(".csv");
    }

    /**
     * Create backup of existing file
     *
     * @param filePath Path to the file to backup
     * @return Path to backup file or null if backup failed
     */
    public String createBackup(String filePath) {
        try {
            Path sourcePath = Paths.get(filePath);

            if (!Files.exists(sourcePath)) {
                System.out.println("Source file does not exist, no backup needed");
                return null;
            }

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String backupPath = filePath.replace(".csv", BACKUP_SUFFIX + "_" + timestamp + ".csv");

            Path backupFilePath = Paths.get(backupPath);
            Files.copy(sourcePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Backup created: " + backupPath);
            return backupPath;

        } catch (IOException e) {
            System.err.println("Failed to create backup: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validate file format and content
     *
     * @param filePath Path to file to validate
     * @return ValidationResult with details
     */
    public ValidationResult validateFile(String filePath) {
        ValidationResult result = new ValidationResult();

        try {
            if (!isValidCsvFile(filePath)) {
                result.addError("File must be a CSV file");
                return result;
            }

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                result.addError("File does not exist: " + filePath);
                return result;
            }

            if (!Files.isReadable(path)) {
                result.addError("File is not readable: " + filePath);
                return result;
            }

            // Check file size
            long fileSize = Files.size(path);
            if (fileSize == 0) {
                result.addError("File is empty");
                return result;
            }

            if (fileSize > 10 * 1024 * 1024) { // 10MB limit
                result.addWarning("File is very large (" + (fileSize / 1024 / 1024) + "MB)");
            }

            // Try to read first few lines
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String firstLine = reader.readLine();
                if (firstLine == null) {
                    result.addError("File appears to be empty");
                    return result;
                }

                // Check if first line looks like CSV
                if (!firstLine.contains(",")) {
                    result.addWarning("File does not appear to be comma-separated");
                }

                result.setValid(true);
            }

        } catch (IOException e) {
            result.addError("Error accessing file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get file information
     *
     * @param filePath Path to file
     * @return FileInfo object with details
     */
    public FileInfo getFileInfo(String filePath) {
        FileInfo info = new FileInfo();

        try {
            Path path = Paths.get(filePath);

            if (Files.exists(path)) {
                info.setExists(true);
                info.setSize(Files.size(path));
                info.setLastModified(Files.getLastModifiedTime(path).toMillis());
                info.setReadable(Files.isReadable(path));
                info.setWritable(Files.isWritable(path));

                // Count lines
                try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    long lineCount = reader.lines().count();
                    info.setLineCount(lineCount);
                }
            }

        } catch (IOException e) {
            System.err.println("Error getting file info: " + e.getMessage());
        }

        return info;
    }

    /**
     * Export records to different formats
     *
     * @param records List of records to export
     * @param filePath Output file path
     * @param format Export format (CSV, TXT, etc.)
     * @return true if successful
     */
    public boolean exportRecords(List<IncomeRecord> records, String filePath, ExportFormat format) {
        try {
            switch (format) {
                case CSV:
                    return writeIncomeFile(filePath, records);

                case TXT:
                    return writeTextFile(filePath, records);

                case PIPE_DELIMITED:
                    return writePipeDelimitedFile(filePath, records);

                default:
                    System.err.println("Unsupported export format: " + format);
                    return false;
            }
        } catch (Exception e) {
            System.err.println("Error exporting records: " + e.getMessage());
            return false;
        }
    }

    /**
     * Write records to text file
     *
     * @param filePath Output file path
     * @param records List of records
     * @return true if successful
     */
    private boolean writeTextFile(String filePath, List<IncomeRecord> records) {
        try {
            Path path = Paths.get(filePath);

            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("Income Records Report");
                writer.newLine();
                writer.write("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.newLine();
                writer.write("================================================================================");
                writer.newLine();
                writer.newLine();

                writer.write(String.format("%-8s %-20s %-12s %12s %12s %12s %8s %8s %5s",
                        "Code", "Description", "Date", "Income", "WHT", "Net", "Orig.CS", "Calc.CS", "Valid"));
                writer.newLine();
                writer.write("--------------------------------------------------------------------------------");
                writer.newLine();

                for (IncomeRecord record : records) {
                    writer.write(record.toFormattedString());
                    writer.newLine();
                }

                writer.newLine();
                writer.write("Total Records: " + records.size());
                writer.newLine();
            }

            return true;

        } catch (IOException e) {
            System.err.println("Error writing text file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Write records to pipe-delimited file
     *
     * @param filePath Output file path
     * @param records List of records
     * @return true if successful
     */
    private boolean writePipeDelimitedFile(String filePath, List<IncomeRecord> records) {
        try {
            Path path = Paths.get(filePath);

            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                for (IncomeRecord record : records) {
                    writer.write(record.toFileStorageLine());
                    writer.newLine();
                }
            }

            return true;

        } catch (IOException e) {
            System.err.println("Error writing pipe-delimited file: " + e.getMessage());
            return false;
        }
    }

    // Enums and helper classes
    public enum ExportFormat {
        CSV, TXT, PIPE_DELIMITED
    }

    public static class ValidationResult {
        private boolean isValid = false;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { this.isValid = valid; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { errors.add(error); }

        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { warnings.add(warning); }

        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
    }

    public static class FileInfo {
        private boolean exists = false;
        private long size = 0;
        private long lastModified = 0;
        private boolean readable = false;
        private boolean writable = false;
        private long lineCount = 0;

        // Getters and setters
        public boolean exists() { return exists; }
        public void setExists(boolean exists) { this.exists = exists; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }

        public boolean isReadable() { return readable; }
        public void setReadable(boolean readable) { this.readable = readable; }

        public boolean isWritable() { return writable; }
        public void setWritable(boolean writable) { this.writable = writable; }

        public long getLineCount() { return lineCount; }
        public void setLineCount(long lineCount) { this.lineCount = lineCount; }
    }
}