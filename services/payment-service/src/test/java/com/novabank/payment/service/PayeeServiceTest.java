package com.novabank.payment.service;

import com.novabank.payment.domain.ExternalAccountType;
import com.novabank.payment.domain.ExternalPayee;
import com.novabank.payment.domain.PayeeStatus;
import com.novabank.payment.dto.request.CreatePayeeRequest;
import com.novabank.payment.mapper.PaymentMapper;
import com.novabank.payment.repository.ExternalPayeeRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayeeServiceTest {
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final ExternalPayeeRepository repository = mock(ExternalPayeeRepository.class);
    private final PayeeService service = new PayeeService(repository, new PaymentMapper());

    @Test
    void createPayeeStoresTokenButDoesNotExposeIt() {
        when(repository.save(any(ExternalPayee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(CUSTOMER_ID, new CreatePayeeRequest("Rent", "Demo Bank", ExternalAccountType.CHECKING, "**** 1234"));

        assertThat(response.maskedAccountNumber()).isEqualTo("**** 1234");
        assertThat(response.status()).isEqualTo(PayeeStatus.VERIFIED);
        assertThat(response.toString()).doesNotContain("demo-ext-token");
        verify(repository).save(argThat(payee -> payee.getExternalAccountToken().startsWith("demo-ext-token-")));
    }
}
