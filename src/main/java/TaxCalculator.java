
/**
 * Tax Calculator Class 
 * Implements tax calculation based on Sri Lankan tax law
 * Formula: Tax Payable = [(Total Income - 150,000) * 12%] - Total WHT Paid
 */

import java.text.DecimalFormat;
import java.util.List;

public class TaxCalculator {

    // Tax calculation constants
    private static final double TAX_FREE_THRESHOLD = 150000.00; // Rs 150,000
    private static final double TAX_RATE = 0.12; // 12%
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    // Constructor
    public TaxCalculator() {
        // Initialize tax calculator
    }

    /**
     * Calculate tax payable for a list of valid income records
     * 
     * @param validRecords List of valid income records
     * @return Tax payable amount
     * @throws IllegalArgumentException If records list is invalid
     */
    public double calculateTaxPayable(List<IncomeRecord> validRecords) {
        if (validRecords == null) {
            throw new IllegalArgumentException("Records list cannot be null");
        }

        if (validRecords.isEmpty()) {
            System.out.println("No records provided for tax calculation");
            return 0.0;
        }

        try {
            // Calculate total income
            double totalIncome = calculateTotalIncome(validRecords);

            // Calculate total WHT paid
            double totalWHT = calculateTotalWHT(validRecords);

            // Calculate tax payable
            double taxPayable = calculateTax(totalIncome, totalWHT);

            // Log calculation details
            logCalculationDetails(validRecords.size(), totalIncome, totalWHT, taxPayable);

            return taxPayable;

        } catch (Exception e) {
            System.err.println("Error calculating tax: " + e.getMessage());
            throw new RuntimeException("Tax calculation failed", e);
        }
    }

    /**
     * Calculate total income from records
     * 
     * @param records List of income records
     * @return Total income amount
     */
    public double calculateTotalIncome(List<IncomeRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;

        for (IncomeRecord record : records) {
            if (record != null) {
                total += record.getIncomeAmount();
            }
        }

        return Math.round(total * 100.0) / 100.0; // Round to 2 decimal places
    }

    /**
     * Calculate total WHT paid from records
     * 
     * @param records List of income records
     * @return Total WHT amount
     */
    public double calculateTotalWHT(List<IncomeRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;

        for (IncomeRecord record : records) {
            if (record != null) {
                total += record.getWhtAmount();
            }
        }

        return Math.round(total * 100.0) / 100.0; // Round to 2 decimal places
    }

    /**
     * Calculate tax based on total income and WHT
     * 
     * @param totalIncome Total income amount
     * @param totalWHT    Total WHT paid
     * @return Tax payable amount
     */
    public double calculateTax(double totalIncome, double totalWHT) {
        // Calculate taxable income (income above tax-free threshold)
        double taxableIncome = Math.max(0.0, totalIncome - TAX_FREE_THRESHOLD);

        // Calculate gross tax (before deducting WHT)
        double grossTax = taxableIncome * TAX_RATE;

        // Calculate net tax payable (after deducting WHT)
        double netTaxPayable = grossTax - totalWHT;

        // Tax payable cannot be negative (no refunds in this system)
        return Math.max(0.0, Math.round(netTaxPayable * 100.0) / 100.0);
    }

    /**
     * Get detailed tax calculation breakdown
     * 
     * @param validRecords List of valid income records
     * @return TaxCalculationDetails object with breakdown
     */
    public TaxCalculationDetails getCalculationDetails(List<IncomeRecord> validRecords) {
        TaxCalculationDetails details = new TaxCalculationDetails();

        if (validRecords == null || validRecords.isEmpty()) {
            return details;
        }

        try {
            // Calculate components
            double totalIncome = calculateTotalIncome(validRecords);
            double totalWHT = calculateTotalWHT(validRecords);
            double taxableIncome = Math.max(0.0, totalIncome - TAX_FREE_THRESHOLD);
            double grossTax = taxableIncome * TAX_RATE;
            double netTaxPayable = Math.max(0.0, grossTax - totalWHT);

            // Populate details
            details.setRecordCount(validRecords.size());
            details.setTotalIncome(totalIncome);
            details.setTaxFreeThreshold(TAX_FREE_THRESHOLD);
            details.setTaxableIncome(taxableIncome);
            details.setTaxRate(TAX_RATE);
            details.setGrossTax(grossTax);
            details.setTotalWHT(totalWHT);
            details.setNetTaxPayable(netTaxPayable);

            // Calculate additional statistics
            details.setAverageIncome(totalIncome / validRecords.size());
            details.setAverageWHT(totalWHT / validRecords.size());
            details.setEffectiveTaxRate(totalIncome > 0 ? (netTaxPayable / totalIncome) * 100.0 : 0.0);
            details.setWHTCoveragePercentage(grossTax > 0 ? (totalWHT / grossTax) * 100.0 : 0.0);

        } catch (Exception e) {
            System.err.println("Error calculating tax details: " + e.getMessage());
        }

        return details;
    }

    /**
     * Calculate tax for individual record
     * 
     * @param record Single income record
     * @return Tax payable for this record
     */
    public double calculateTaxForRecord(IncomeRecord record) {
        if (record == null) {
            return 0.0;
        }

        List<IncomeRecord> singleRecordList = List.of(record);
        return calculateTaxPayable(singleRecordList);
    }

    /**
     * Check if income is above tax threshold
     * 
     * @param income Income amount to check
     * @return true if taxable, false otherwise
     */
    public boolean isIncomeAboveThreshold(double income) {
        return income > TAX_FREE_THRESHOLD;
    }

    /**
     * Calculate required WHT to fully cover tax liability
     * 
     * @param totalIncome Total income amount
     * @return Required WHT amount
     */
    public double calculateRequiredWHT(double totalIncome) {
        double taxableIncome = Math.max(0.0, totalIncome - TAX_FREE_THRESHOLD);
        return taxableIncome * TAX_RATE;
    }

    /**
     * Simulate tax calculation with different income scenarios
     * 
     * @param baseIncome Base income amount
     * @param increments Array of income increments to test
     * @return List of tax scenarios
     */
    public List<TaxScenario> simulateTaxScenarios(double baseIncome, double[] increments) {
        List<TaxScenario> scenarios = new java.util.ArrayList<>();

        for (double increment : increments) {
            double totalIncome = baseIncome + increment;
            double taxPayable = calculateTax(totalIncome, 0.0); // No WHT for simulation

            TaxScenario scenario = new TaxScenario();
            scenario.setIncome(totalIncome);
            scenario.setTaxPayable(taxPayable);
            scenario.setTaxableIncome(Math.max(0.0, totalIncome - TAX_FREE_THRESHOLD));
            scenario.setEffectiveRate(totalIncome > 0 ? (taxPayable / totalIncome) * 100.0 : 0.0);

            scenarios.add(scenario);
        }

        return scenarios;
    }

    /**
     * Get tax calculation summary for reporting
     * 
     * @param validRecords List of valid income records
     * @return Formatted summary string
     */
    public String getTaxSummaryReport(List<IncomeRecord> validRecords) {
        if (validRecords == null || validRecords.isEmpty()) {
            return "No records available for tax calculation.";
        }

        TaxCalculationDetails details = getCalculationDetails(validRecords);

        StringBuilder report = new StringBuilder();
        report.append("TAX CALCULATION SUMMARY\n");
        report.append("=======================\n\n");

        report.append("Input Data:\n");
        report.append(String.format("  Number of Records: %d\n", details.getRecordCount()));
        report.append(String.format("  Total Income: Rs %s\n", CURRENCY_FORMAT.format(details.getTotalIncome())));
        report.append(String.format("  Total WHT Paid: Rs %s\n", CURRENCY_FORMAT.format(details.getTotalWHT())));
        report.append("\n");

        report.append("Tax Calculation:\n");
        report.append(
                String.format("  Tax-Free Threshold: Rs %s\n", CURRENCY_FORMAT.format(details.getTaxFreeThreshold())));
        report.append(String.format("  Taxable Income: Rs %s\n", CURRENCY_FORMAT.format(details.getTaxableIncome())));
        report.append(String.format("  Tax Rate: %.1f%%\n", details.getTaxRate() * 100));
        report.append(String.format("  Gross Tax: Rs %s\n", CURRENCY_FORMAT.format(details.getGrossTax())));
        report.append(String.format("  Less: WHT Paid: Rs %s\n", CURRENCY_FORMAT.format(details.getTotalWHT())));
        report.append(String.format("  NET TAX PAYABLE: Rs %s\n", CURRENCY_FORMAT.format(details.getNetTaxPayable())));
        report.append("\n");

        report.append("Statistics:\n");
        report.append(String.format("  Average Income per Record: Rs %s\n",
                CURRENCY_FORMAT.format(details.getAverageIncome())));
        report.append(
                String.format("  Average WHT per Record: Rs %s\n", CURRENCY_FORMAT.format(details.getAverageWHT())));
        report.append(String.format("  Effective Tax Rate: %.2f%%\n", details.getEffectiveTaxRate()));
        report.append(String.format("  WHT Coverage: %.1f%%\n", details.getWHTCoveragePercentage()));

        return report.toString();
    }

    /**
     * Log calculation details to console
     * 
     * @param recordCount Number of records
     * @param totalIncome Total income amount
     * @param totalWHT    Total WHT amount
     * @param taxPayable  Tax payable amount
     */
    private void logCalculationDetails(int recordCount, double totalIncome, double totalWHT, double taxPayable) {
        System.out.println("Tax Calculation Completed:");
        System.out.println("  Records processed: " + recordCount);
        System.out.println("  Total Income: Rs " + CURRENCY_FORMAT.format(totalIncome));
        System.out.println("  Total WHT: Rs " + CURRENCY_FORMAT.format(totalWHT));
        System.out.println("  Tax Payable: Rs " + CURRENCY_FORMAT.format(taxPayable));
    }

    /**
     * Validate tax calculation inputs
     * 
     * @param records List of records to validate
     * @return true if inputs are valid for tax calculation
     */
    public boolean validateCalculationInputs(List<IncomeRecord> records) {
        if (records == null) {
            System.err.println("Records list is null");
            return false;
        }

        if (records.isEmpty()) {
            System.err.println("Records list is empty");
            return false;
        }

        for (IncomeRecord record : records) {
            if (record == null) {
                System.err.println("Found null record in list");
                return false;
            }

            if (!record.isValid()) {
                System.err.println("Found invalid record: " + record.getIncomeCode());
                return false;
            }

            if (record.getIncomeAmount() < 0) {
                System.err.println("Found negative income amount in record: " + record.getIncomeCode());
                return false;
            }

            if (record.getWhtAmount() < 0) {
                System.err.println("Found negative WHT amount in record: " + record.getIncomeCode());
                return false;
            }
        }

        return true;
    }

    // Helper classes
    public static class TaxCalculationDetails {
        private int recordCount = 0;
        private double totalIncome = 0.0;
        private double taxFreeThreshold = 0.0;
        private double taxableIncome = 0.0;
        private double taxRate = 0.0;
        private double grossTax = 0.0;
        private double totalWHT = 0.0;
        private double netTaxPayable = 0.0;
        private double averageIncome = 0.0;
        private double averageWHT = 0.0;
        private double effectiveTaxRate = 0.0;
        private double whtCoveragePercentage = 0.0;

        // Getters and setters
        public int getRecordCount() {
            return recordCount;
        }

        public void setRecordCount(int recordCount) {
            this.recordCount = recordCount;
        }

        public double getTotalIncome() {
            return totalIncome;
        }

        public void setTotalIncome(double totalIncome) {
            this.totalIncome = totalIncome;
        }

        public double getTaxFreeThreshold() {
            return taxFreeThreshold;
        }

        public void setTaxFreeThreshold(double taxFreeThreshold) {
            this.taxFreeThreshold = taxFreeThreshold;
        }

        public double getTaxableIncome() {
            return taxableIncome;
        }

        public void setTaxableIncome(double taxableIncome) {
            this.taxableIncome = taxableIncome;
        }

        public double getTaxRate() {
            return taxRate;
        }

        public void setTaxRate(double taxRate) {
            this.taxRate = taxRate;
        }

        public double getGrossTax() {
            return grossTax;
        }

        public void setGrossTax(double grossTax) {
            this.grossTax = grossTax;
        }

        public double getTotalWHT() {
            return totalWHT;
        }

        public void setTotalWHT(double totalWHT) {
            this.totalWHT = totalWHT;
        }

        public double getNetTaxPayable() {
            return netTaxPayable;
        }

        public void setNetTaxPayable(double netTaxPayable) {
            this.netTaxPayable = netTaxPayable;
        }

        public double getAverageIncome() {
            return averageIncome;
        }

        public void setAverageIncome(double averageIncome) {
            this.averageIncome = averageIncome;
        }

        public double getAverageWHT() {
            return averageWHT;
        }

        public void setAverageWHT(double averageWHT) {
            this.averageWHT = averageWHT;
        }

        public double getEffectiveTaxRate() {
            return effectiveTaxRate;
        }

        public void setEffectiveTaxRate(double effectiveTaxRate) {
            this.effectiveTaxRate = effectiveTaxRate;
        }

        public double getWHTCoveragePercentage() {
            return whtCoveragePercentage;
        }

        public void setWHTCoveragePercentage(double whtCoveragePercentage) {
            this.whtCoveragePercentage = whtCoveragePercentage;
        }
    }

    public static class TaxScenario {
        private double income = 0.0;
        private double taxableIncome = 0.0;
        private double taxPayable = 0.0;
        private double effectiveRate = 0.0;

        // Getters and setters
        public double getIncome() {
            return income;
        }

        public void setIncome(double income) {
            this.income = income;
        }

        public double getTaxableIncome() {
            return taxableIncome;
        }

        public void setTaxableIncome(double taxableIncome) {
            this.taxableIncome = taxableIncome;
        }

        public double getTaxPayable() {
            return taxPayable;
        }

        public void setTaxPayable(double taxPayable) {
            this.taxPayable = taxPayable;
        }

        public double getEffectiveRate() {
            return effectiveRate;
        }

        public void setEffectiveRate(double effectiveRate) {
            this.effectiveRate = effectiveRate;
        }

        @Override
        public String toString() {
            return String.format("TaxScenario{income=%.2f, taxable=%.2f, tax=%.2f, rate=%.2f%%}",
                    income, taxableIncome, taxPayable, effectiveRate);
        }
    }
}