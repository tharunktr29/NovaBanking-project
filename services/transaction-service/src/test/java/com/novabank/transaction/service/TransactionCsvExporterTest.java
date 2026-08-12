package com.novabank.transaction.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCsvExporterTest {
    @Test
    void escapesCsvAndProtectsSpreadsheetFormulaInjection() {
        assertThat(TransactionCsvExporter.csv("plain")).isEqualTo("\"plain\"");
        assertThat(TransactionCsvExporter.csv("A, B \"quoted\"")).isEqualTo("\"A, B \"\"quoted\"\"\"");
        assertThat(TransactionCsvExporter.csv("=SUM(1,2)")).isEqualTo("\"'=SUM(1,2)\"");
        assertThat(TransactionCsvExporter.csv("+123")).isEqualTo("\"'+123\"");
        assertThat(TransactionCsvExporter.csv("-123")).isEqualTo("\"'-123\"");
        assertThat(TransactionCsvExporter.csv("@cmd")).isEqualTo("\"'@cmd\"");
    }
}
