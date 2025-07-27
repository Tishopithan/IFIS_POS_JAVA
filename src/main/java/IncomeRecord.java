
/**
 * Income Record Data Model 
 * Represents a single income record with validation and checksum
 */

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

public class IncomeRecord {

    // Fields
    private String incomeCode;
    private String description;
    private String date;
    private double incomeAmount;
    private double whtAmount;
    private int originalChecksum;
    private int calculatedChecksum;
    private boolean isValid;

    // Constants for validation
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z]{2}\\d{3}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#0.00");

    // Constructors
    public IncomeRecord() {
        // Default constructor
        this.isValid = false;
    }

    public IncomeRecord(String incomeCode, String description, String date,
            double incomeAmount, double whtAmount, int originalChecksum) {
        this.incomeCode = validateAndSetIncomeCode(incomeCode);
        this.description = validateAndSetDescription(description);
        this.date = validateAndSetDate(date);
        this.incomeAmount = validateAndSetIncomeAmount(incomeAmount);
        this.whtAmount = validateAndSetWhtAmount(whtAmount);
        this.originalChecksum = originalChecksum;
        this.calculatedChecksum = 0; // Will be calculated later
        this.isValid = false; // Will be validated later
    }

    public IncomeRecord(String incomeCode, String description, String date,
            double incomeAmount, double whtAmount) {
        this(incomeCode, description, date, incomeAmount, whtAmount, 0);
    }

    // Validation methods
    private String validateAndSetIncomeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Income code cannot be null or empty");
        }

        String trimmedCode = code.trim().toUpperCase();

        if (!CODE_PATTERN.matcher(trimmedCode).matches()) {
            throw new IllegalArgumentException("Income code must be 2 letters followed by 3 digits (e.g., IN001)");
        }

        return trimmedCode;
    }

    private String validateAndSetDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }

        String trimmedDescription = description.trim();

        if (trimmedDescription.length() > 20) {
            throw new IllegalArgumentException("Description cannot exceed 20 characters");
        }

        return trimmedDescription;
    }

    private String validateAndSetDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Date cannot be null or empty");
        }

        String trimmedDate = date.trim();

        if (!DATE_PATTERN.matcher(trimmedDate).matches()) {
            throw new IllegalArgumentException("Date must be in DD/MM/YYYY format");
        }

        // Validate actual date
        try {
            LocalDate.parse(trimmedDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date values: " + trimmedDate);
        }

        return trimmedDate;
    }

    private double validateAndSetIncomeAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Income amount must be positive");
        }

        return Math.round(amount * 100.0) / 100.0; // Round to 2 decimal places
    }

    private double validateAndSetWhtAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("WHT amount cannot be negative");
        }

        return Math.round(amount * 100.0) / 100.0; // Round to 2 decimal places
    }

    // Getters
    public String getIncomeCode() {
        return incomeCode;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public double getIncomeAmount() {
        return incomeAmount;
    }

    public double getWhtAmount() {
        return whtAmount;
    }

    public int getOriginalChecksum() {
        return originalChecksum;
    }

    public int getCalculatedChecksum() {
        return calculatedChecksum;
    }

    public boolean isValid() {
        return isValid;
    }

    public double getNetAmount() {
        return Math.round((incomeAmount - whtAmount) * 100.0) / 100.0;
    }

    // Setters with validation
    public void setIncomeCode(String incomeCode) {
        this.incomeCode = validateAndSetIncomeCode(incomeCode);
    }

    public void setDescription(String description) {
        this.description = validateAndSetDescription(description);
    }

    public void setDate(String date) {
        this.date = validateAndSetDate(date);
    }

    public void setIncomeAmount(double incomeAmount) {
        this.incomeAmount = validateAndSetIncomeAmount(incomeAmount);
    }

    public void setWhtAmount(double whtAmount) {
        this.whtAmount = validateAndSetWhtAmount(whtAmount);
    }

    public void setOriginalChecksum(int originalChecksum) {
        this.originalChecksum = originalChecksum;
    }

    public void setCalculatedChecksum(int calculatedChecksum) {
        this.calculatedChecksum = calculatedChecksum;
    }

    public void setValid(boolean valid) {
        this.isValid = valid;
    }

    // Business logic methods
    public void updateRecord(String description, String date, double incomeAmount, double whtAmount) {
        setDescription(description);
        setDate(date);
        setIncomeAmount(incomeAmount);
        setWhtAmount(whtAmount);

        // Mark as potentially invalid since data changed
        this.isValid = false;
    }

    public String toCsvLine() {
        return String.format("%s,%s,%s,%.2f,%.2f,%d",
                incomeCode, description, date, incomeAmount, whtAmount, calculatedChecksum);
    }

    public String toCsvLineWithoutChecksum() {
        return String.format("%s,%s,%s,%.2f,%.2f",
                incomeCode, description, date, incomeAmount, whtAmount);
    }

    public String toFileStorageLine() {
        return String.format("%s|%s|%s|%.2f|%.2f",
                incomeCode, description, date, incomeAmount, whtAmount);
    }

    // Static factory methods
    public static IncomeRecord fromCsvLine(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV line cannot be null or empty");
        }

        String[] parts = csvLine.trim().split(",");

        if (parts.length < 5) {
            throw new IllegalArgumentException("CSV line must have at least 5 parts");
        }

        try {
            String code = parts[0].trim();
            String description = parts[1].trim();
            String date = parts[2].trim();
            double incomeAmount = Double.parseDouble(parts[3].trim());
            double whtAmount = Double.parseDouble(parts[4].trim());

            int originalChecksum = 0;
            if (parts.length >= 6 && !parts[5].trim().isEmpty()) {
                originalChecksum = Integer.parseInt(parts[5].trim());
            }

            return new IncomeRecord(code, description, date, incomeAmount, whtAmount, originalChecksum);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in CSV line: " + e.getMessage());
        }
    }

    public static IncomeRecord fromFileStorageLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("File line cannot be null or empty");
        }

        String[] parts = line.trim().split("\\|");

        if (parts.length != 5) {
            throw new IllegalArgumentException("File line must have exactly 5 parts");
        }

        try {
            String code = parts[0].trim();
            String description = parts[1].trim();
            String date = parts[2].trim();
            double incomeAmount = Double.parseDouble(parts[3].trim());
            double whtAmount = Double.parseDouble(parts[4].trim());

            return new IncomeRecord(code, description, date, incomeAmount, whtAmount);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in file line: " + e.getMessage());
        }
    }

    // Utility methods
    public boolean hasValidFormat() {
        try {
            validateAndSetIncomeCode(incomeCode);
            validateAndSetDescription(description);
            validateAndSetDate(date);
            validateAndSetIncomeAmount(incomeAmount);
            validateAndSetWhtAmount(whtAmount);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getValidationErrors() {
        StringBuilder errors = new StringBuilder();

        try {
            validateAndSetIncomeCode(incomeCode);
        } catch (IllegalArgumentException e) {
            errors.append("Income Code: ").append(e.getMessage()).append("; ");
        }

        try {
            validateAndSetDescription(description);
        } catch (IllegalArgumentException e) {
            errors.append("Description: ").append(e.getMessage()).append("; ");
        }

        try {
            validateAndSetDate(date);
        } catch (IllegalArgumentException e) {
            errors.append("Date: ").append(e.getMessage()).append("; ");
        }

        try {
            validateAndSetIncomeAmount(incomeAmount);
        } catch (IllegalArgumentException e) {
            errors.append("Income Amount: ").append(e.getMessage()).append("; ");
        }

        try {
            validateAndSetWhtAmount(whtAmount);
        } catch (IllegalArgumentException e) {
            errors.append("WHT Amount: ").append(e.getMessage()).append("; ");
        }

        return errors.toString();
    }

    public IncomeRecord copy() {
        IncomeRecord copy = new IncomeRecord(incomeCode, description, date, incomeAmount, whtAmount,
                originalChecksum);
        copy.setCalculatedChecksum(calculatedChecksum);
        copy.setValid(isValid);
        return copy;
    }

    // Object methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        IncomeRecord that = (IncomeRecord) obj;
        return Objects.equals(incomeCode, that.incomeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incomeCode);
    }

    @Override
    public String toString() {
        return String.format("IncomeRecord{code='%s', description='%s', date='%s', " +
                "income=%.2f, wht=%.2f, net=%.2f, originalChecksum=%d, " +
                "calculatedChecksum=%d, valid=%b}",
                incomeCode, description, date, incomeAmount, whtAmount,
                getNetAmount(), originalChecksum, calculatedChecksum, isValid);
    }

    public String toDisplayString() {
        return String.format("%s - %s (%s) - Income: Rs %.2f, WHT: Rs %.2f, Net: Rs %.2f",
                incomeCode, description, date, incomeAmount, whtAmount, getNetAmount());
    }

    public String toFormattedString() {
        return String.format("%-8s %-20s %-12s %12.2f %12.2f %12.2f %8d %8d %5s",
                incomeCode,
                description.length() > 20 ? description.substring(0, 17) + "..." : description,
                date, incomeAmount, whtAmount, getNetAmount(),
                originalChecksum, calculatedChecksum,
                isValid ? "✓" : "✗");
    }

    // Validation helper methods
    public static boolean isValidIncomeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return CODE_PATTERN.matcher(code.trim().toUpperCase()).matches();
    }

    public static boolean isValidDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return false;
        }

        if (!DATE_PATTERN.matcher(date.trim()).matches()) {
            return false;
        }

        try {
            LocalDate.parse(date.trim(), DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isValidAmount(double amount) {
        return amount >= 0;
    }

    public static boolean isValidIncomeAmount(double amount) {
        return amount > 0;
    }

    // Sorting helper methods
    public int compareByCode(IncomeRecord other) {
        return this.incomeCode.compareTo(other.incomeCode);
    }

    public int compareByDate(IncomeRecord other) {
        return this.date.compareTo(other.date);
    }

    public int compareByIncomeAmount(IncomeRecord other) {
        return Double.compare(this.incomeAmount, other.incomeAmount);
    }
}