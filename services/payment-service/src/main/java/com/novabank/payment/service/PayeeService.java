package com.novabank.payment.service;

import com.novabank.payment.domain.ExternalPayee;
import com.novabank.payment.domain.PayeeStatus;
import com.novabank.payment.dto.request.CreatePayeeRequest;
import com.novabank.payment.dto.request.UpdatePayeeRequest;
import com.novabank.payment.dto.response.PayeeResponse;
import com.novabank.payment.exception.PaymentException;
import com.novabank.payment.mapper.PaymentMapper;
import com.novabank.payment.repository.ExternalPayeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
public class PayeeService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExternalPayeeRepository payeeRepository;
    private final PaymentMapper mapper;

    public PayeeService(ExternalPayeeRepository payeeRepository, PaymentMapper mapper) {
        this.payeeRepository = payeeRepository;
        this.mapper = mapper;
    }

    public List<PayeeResponse> list(UUID customerId) {
        return payeeRepository.findByCustomerIdOrderByNicknameAsc(customerId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public PayeeResponse create(UUID customerId, CreatePayeeRequest request) {
        var payee = new ExternalPayee();
        payee.setCustomerId(customerId);
        payee.setPayeeReference("PAYEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payee.setNickname(clean(request.nickname()));
        payee.setBankName(clean(request.bankName()));
        payee.setAccountType(request.accountType());
        payee.setMaskedAccountNumber(request.maskedAccountNumber().replace("••••", "****").trim());
        payee.setExternalAccountToken("demo-ext-token-" + Long.toUnsignedString(RANDOM.nextLong(), 36));
        payee.setStatus(PayeeStatus.VERIFIED);
        return mapper.toResponse(payeeRepository.save(payee));
    }

    @Transactional
    public PayeeResponse update(UUID customerId, UUID payeeId, UpdatePayeeRequest request) {
        var payee = payeeRepository.findByIdAndCustomerId(payeeId, customerId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYEE_NOT_FOUND", "Payee was not found"));
        if (request.nickname() != null && !request.nickname().isBlank()) {
            payee.setNickname(clean(request.nickname()));
        }
        return mapper.toResponse(payee);
    }

    @Transactional
    public void disable(UUID customerId, UUID payeeId) {
        var payee = payeeRepository.findByIdAndCustomerId(payeeId, customerId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYEE_NOT_FOUND", "Payee was not found"));
        payee.setStatus(PayeeStatus.DISABLED);
    }

    ExternalPayee requireVerified(UUID customerId, UUID payeeId) {
        var payee = payeeRepository.findByIdAndCustomerId(payeeId, customerId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYEE_NOT_FOUND", "Payee was not found"));
        if (payee.getStatus() != PayeeStatus.VERIFIED) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "PAYEE_NOT_VERIFIED", "Payee is not verified for payments");
        }
        return payee;
    }

    private String clean(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }
}
