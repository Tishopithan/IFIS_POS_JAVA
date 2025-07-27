
/**
 * Checksum Validator Class 
 * Implements checksum calculation and validation for income records
 */

import java.util.List;

public class ChecksumValidator {

    // Constructor
    public ChecksumValidator() {
        // Initialize checksum validator
    }

    /**
     * Validate a single income record by comparing checksums
     * 
     * @param record The income record to validate
     * @return true if record is valid (checksums match), false otherwise
     */
    public boolean validateRecord(IncomeRecord record) {
        if (record == null) {
            System.err.println("Cannot validate null record");
            return false;
        }

        try {
            // Calculate the checksum for the current record data
            int calculatedChecksum = calculateChecksum(record);

            // Update the record with the calculated checksum
            record.setCalculatedChecksum(calculatedChecksum);

            // Compare with original checksum
            boolean isValid = (calculatedChecksum == record.getOriginalChecksum());

            // Update the record's validity status
            record.setValid(isValid);

            return isValid;

        } catch (Exception e) {
            System.err.println("Error validating record " + record.getIncomeCode() + ": " + e.getMessage());
            record.setValid(false);
            return false;
        }
    }

    /**
     * Calculate checksum for an income record
     * 
     * Algorithm:
     * 1. Create transaction line without checksum
     * 2. Count all capital letters only
     * 3. Count all numbers and decimal points
     * 4. Sum the two counts
     * 
     * @param record The income record
     * @return Calculated checksum value
     */
    public int calculateChecksum(IncomeRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Record cannot be null");
        }

        // Create the transaction line without checksum
        String transactionLine = record.toCsvLineWithoutChecksum();

        return calculateChecksumFromLine(transactionLine);
    }

    /**
     * Calculate checksum from a transaction line string
     * 
     * @param line The transaction line
     * @return Calculated checksum value
     */
    public int calculateChecksumFromLine(String line) {
        if (line == null) {
            throw new IllegalArgumentException("Line cannot be null");
        }

        int capitalLetterCount = 0;
        int numberDecimalCount = 0;

        // Process each character in the line
        for (char c : line.toCharArray()) {
            if (Character.isUpperCase(c)) {
                capitalLetterCount++;
            } else if (Character.isDigit(c) || c == '.') {
                numberDecimalCount++;
            }
        }

        // Return the sum of both counts
        return capitalLetterCount + numberDecimalCount;
    }

    /**
     * Validate multiple records
     * 
     * @param records List of records to validate
     * @return ValidationSummary with results
     */
    public ValidationSummary validateRecords(List<IncomeRecord> records) {
        ValidationSummary summary = new ValidationSummary();

        if (records == null || records.isEmpty()) {
            System.out.println("No records to validate");
            return summary;
        }

        System.out.println("Validating " + records.size() + " records...");

        for (IncomeRecord record : records) {
            try {
                boolean isValid = validateRecord(record);

                if (isValid) {
                    summary.addValidRecord(record);
                } else {
                    summary.addInvalidRecord(record);
                }

                summary.incrementTotalRecords();

            } catch (Exception e) {
                System.err.println("Error validating record " + record.getIncomeCode() + ": " + e.getMessage());
                summary.addInvalidRecord(record);
                summary.incrementTotalRecords();
            }
        }

        System.out.println("Validation completed:");
        System.out.println("  Total records: " + summary.getTotalRecords());
        System.out.println("  Valid records: " + summary.getValidRecords().size());
        System.out.println("  Invalid records: " + summary.getInvalidRecords().size());

        return summary;
    }

    /**
     * Recalculate and update checksums for all records
     * 
     * @param records List of records to update
     * @return Number of records updated
     */
    public int recalculateChecksums(List<IncomeRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;

        for (IncomeRecord record : records) {
            try {
                int newChecksum = calculateChecksum(record);
                record.setCalculatedChecksum(newChecksum);

                // Optionally update the original checksum with the calculated one
                // record.setOriginalChecksum(newChecksum);

                updatedCount++;

            } catch (Exception e) {
                System.err.println("Error recalculating checksum for record " +
                        record.getIncomeCode() + ": " + e.getMessage());
            }
        }

        System.out.println("Recalculated checksums for " + updatedCount + " records");
        return updatedCount;
    }

    /**
     * Fix invalid records by updating their original checksums
     * 
     * @param records List of records to fix
     * @return Number of records fixed
     */
    public int fixInvalidRecords(List<IncomeRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        int fixedCount = 0;

        for (IncomeRecord record : records) {
            try {
                // Calculate new checksum
                int calculatedChecksum = calculateChecksum(record);
                record.setCalculatedChecksum(calculatedChecksum);

                // If checksums don't match, update the original checksum
                if (calculatedChecksum != record.getOriginalChecksum()) {
                    record.setOriginalChecksum(calculatedChecksum);
                    record.setValid(true);
                    fixedCount++;
                }

            } catch (Exception e) {
                System.err.println("Error fixing record " + record.getIncomeCode() + ": " + e.getMessage());
            }
        }

        System.out.println("Fixed " + fixedCount + " invalid records");
        return fixedCount;
    }

    /**
     * Get detailed validation report for a record
     * 
     * @param record The record to analyze
     * @return Detailed validation report
     */
    public ValidationReport getValidationReport(IncomeRecord record) {
        ValidationReport report = new ValidationReport();

        if (record == null) {
            report.addError("Record is null");
            return report;
        }

        try {
            // Basic format validation
            if (!record.hasValidFormat()) {
                report.addError("Record has invalid format: " + record.getValidationErrors());
            }

            // Checksum calculation and validation
            String transactionLine = record.toCsvLineWithoutChecksum();
            int calculatedChecksum = calculateChecksumFromLine(transactionLine);

            report.setTransactionLine(transactionLine);
            report.setCalculatedChecksum(calculatedChecksum);
            report.setOriginalChecksum(record.getOriginalChecksum());

            // Detailed checksum analysis
            int capitalLetterCount = 0;
            int numberDecimalCount = 0;

            for (char c : transactionLine.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    capitalLetterCount++;
                } else if (Character.isDigit(c) || c == '.') {
                    numberDecimalCount++;
                }
            }

            report.setCapitalLetterCount(capitalLetterCount);
            report.setNumberDecimalCount(numberDecimalCount);

            // Determine if valid
            boolean isValid = (calculatedChecksum == record.getOriginalChecksum());
            report.setValid(isValid);

            if (!isValid) {
                report.addError("Checksum mismatch: expected " + record.getOriginalChecksum() +
                        ", calculated " + calculatedChecksum);
            }

        } catch (Exception e) {
            report.addError("Error during validation: " + e.getMessage());
        }

        return report;
    }

    /**
     * Test the checksum algorithm with known values
     * 
     * @return true if all tests pass
     */
    public boolean testChecksumAlgorithm() {
        System.out.println("Testing checksum algorithm...");

        // Test case 1: Known transaction line
        String testLine1 = "IN001,Freelance Work,25/07/2025,10000.00,1000.00";
        int expectedChecksum1 = 30; // Expected based on algorithm
        int calculatedChecksum1 = calculateChecksumFromLine(testLine1);

        boolean test1Pass = (calculatedChecksum1 == expectedChecksum1);
        System.out.println("Test 1: " + (test1Pass ? "PASS" : "FAIL") +
                " - Expected: " + expectedChecksum1 + ", Got: " + calculatedChecksum1);

        // Test case 2: Different line
        String testLine2 = "SA002,Consulting,26/07/2025,15000.00,1500.00";
        int calculatedChecksum2 = calculateChecksumFromLine(testLine2);
        System.out.println("Test 2: Calculated checksum for '" + testLine2 + "' = " + calculatedChecksum2);

        // Test case 3: Edge case - minimal data
        String testLine3 = "AB123,Test,01/01/2024,1.00,0.00";
        int calculatedChecksum3 = calculateChecksumFromLine(testLine3);
        System.out.println("Test 3: Calculated checksum for '" + testLine3 + "' = " + calculatedChecksum3);

        // Test case 4: Record object
        try {
            IncomeRecord testRecord = new IncomeRecord("IN001", "Freelance Work", "25/07/2025", 10000.00,
                    1000.00);
            int recordChecksum = calculateChecksum(testRecord);
            boolean test4Pass = (recordChecksum == expectedChecksum1);
            System.out.println("Test 4: " + (test4Pass ? "PASS" : "FAIL") +
                    " - Record checksum: " + recordChecksum);

            return test1Pass && test4Pass;

        } catch (Exception e) {
            System.err.println("Test 4 FAILED: " + e.getMessage());
            return false;
        }
    }

    // Helper classes
    public static class ValidationSummary {
        private int totalRecords = 0;
        private List<IncomeRecord> validRecords = new java.util.ArrayList<>();
        private List<IncomeRecord> invalidRecords = new java.util.ArrayList<>();

        public int getTotalRecords() {
            return totalRecords;
        }

        public void incrementTotalRecords() {
            this.totalRecords++;
        }

        public List<IncomeRecord> getValidRecords() {
            return validRecords;
        }

        public void addValidRecord(IncomeRecord record) {
            validRecords.add(record);
        }

        public List<IncomeRecord> getInvalidRecords() {
            return invalidRecords;
        }

        public void addInvalidRecord(IncomeRecord record) {
            invalidRecords.add(record);
        }

        public double getValidityPercentage() {
            if (totalRecords == 0)
                return 0.0;
            return (double) validRecords.size() / totalRecords * 100.0;
        }

        public boolean hasInvalidRecords() {
            return !invalidRecords.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("ValidationSummary{total=%d, valid=%d, invalid=%d, validity=%.1f%%}",
                    totalRecords, validRecords.size(), invalidRecords.size(), getValidityPercentage());
        }
    }

    public static class ValidationReport {
        private boolean isValid = false;
        private String transactionLine = "";
        private int originalChecksum = 0;
        private int calculatedChecksum = 0;
        private int capitalLetterCount = 0;
        private int numberDecimalCount = 0;
        private List<String> errors = new java.util.ArrayList<>();

        // Getters and setters
        public boolean isValid() {
            return isValid;
        }

        public void setValid(boolean valid) {
            this.isValid = valid;
        }

        public String getTransactionLine() {
            return transactionLine;
        }

        public void setTransactionLine(String transactionLine) {
            this.transactionLine = transactionLine;
        }

        public int getOriginalChecksum() {
            return originalChecksum;
        }

        public void setOriginalChecksum(int originalChecksum) {
            this.originalChecksum = originalChecksum;
        }

        public int getCalculatedChecksum() {
            return calculatedChecksum;
        }

        public void setCalculatedChecksum(int calculatedChecksum) {
            this.calculatedChecksum = calculatedChecksum;
        }

        public int getCapitalLetterCount() {
            return capitalLetterCount;
        }

        public void setCapitalLetterCount(int capitalLetterCount) {
            this.capitalLetterCount = capitalLetterCount;
        }

        public int getNumberDecimalCount() {
            return numberDecimalCount;
        }

        public void setNumberDecimalCount(int numberDecimalCount) {
            this.numberDecimalCount = numberDecimalCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void addError(String error) {
            errors.add(error);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ValidationReport{\n");
            sb.append("  Transaction Line: ").append(transactionLine).append("\n");
            sb.append("  Original Checksum: ").append(originalChecksum).append("\n");
            sb.append("  Calculated Checksum: ").append(calculatedChecksum).append("\n");
            sb.append("  Capital Letters: ").append(capitalLetterCount).append("\n");
            sb.append("  Numbers/Decimals: ").append(numberDecimalCount).append("\n");
            sb.append("  Valid: ").append(isValid).append("\n");

            if (hasErrors()) {
                sb.append("  Errors:\n");
                for (String error : errors) {
                    sb.append("    - ").append(error).append("\n");
                }
            }

            sb.append("}");
            return sb.toString();
        }
    }
}