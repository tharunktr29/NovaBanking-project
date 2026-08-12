package com.novabank.transaction.service;

import com.novabank.transaction.config.TransactionProperties;
import com.novabank.transaction.dto.TransactionQuery;
import com.novabank.transaction.exception.TransactionException;
import com.novabank.transaction.repository.BankTransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionCsvExporter {
    private final BankTransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final TransactionProperties properties;
    private final TransactionTemplate transactionTemplate;

    public TransactionCsvExporter(
            BankTransactionRepository transactionRepository,
            TransactionService transactionService,
            TransactionProperties properties,
            TransactionTemplate transactionTemplate
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    public String filename(UUID customerId) {
        return "novabank-transactions-" + customerId + "-" + LocalDate.now(ZoneOffset.UTC) + ".csv";
    }

    public StreamingResponseBody export(UUID customerId, TransactionQuery query) {
        validateExport(query);
        transactionService.validateOwnedAccountFilter(customerId, query.accountId());
        var sort = transactionService.sort(query.sort());
        return outputStream -> {
            try (var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writer.write("date,account,status,direction,type,category,description,merchant,amount,currency\n");
                transactionTemplate.executeWithoutResult(status -> {
                    var rows = transactionRepository.findAll(
                            TransactionSpecifications.byCustomerAndQuery(customerId, query),
                            PageRequest.of(0, properties.exportRowLimit(), sort)
                    );
                    try {
                        for (var transaction : rows) {
                            var merchant = transaction.getMerchant() == null ? "" : transaction.getMerchant().getName();
                            writer.write(String.join(",",
                                    csv(DateTimeFormatter.ISO_INSTANT.format(transaction.getAuthorizedAt())),
                                    csv(transaction.getAccountId().toString()),
                                    csv(transaction.getStatus().name()),
                                    csv(transaction.getDirection().name()),
                                    csv(transaction.getType().name()),
                                    csv(transaction.getCategory().name()),
                                    csv(transaction.getDescription()),
                                    csv(merchant),
                                    csv(transaction.getAmount().toPlainString()),
                                    csv(transaction.getCurrency())
                            ));
                            writer.write("\n");
                        }
                    } catch (java.io.IOException ex) {
                        throw new java.io.UncheckedIOException(ex);
                    }
                });
            }
        };
    }

    private void validateExport(TransactionQuery query) {
        if (query.dateFrom() != null && query.dateTo() != null) {
            var days = ChronoUnit.DAYS.between(query.dateFrom(), query.dateTo()) + 1;
            if (days > properties.exportMaxDays()) {
                throw new TransactionException(HttpStatus.BAD_REQUEST, "EXPORT_RANGE_TOO_LARGE",
                        "CSV export date range cannot exceed " + properties.exportMaxDays() + " days");
            }
        }
    }

    static String csv(String value) {
        var safe = value == null ? "" : value;
        if (List.of("=", "+", "-", "@").stream().anyMatch(safe::startsWith)) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }
}
