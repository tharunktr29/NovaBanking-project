package com.novabank.engagement.service;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class DocumentCryptoServiceTest {
  @Test void encryptsWithRandomNoncesAndRoundTrips() {
    var service=new DocumentCryptoService("test-key-with-enough-entropy"); var clear="fictional statement".getBytes(StandardCharsets.UTF_8);
    var first=service.encrypt(clear); var second=service.encrypt(clear);
    assertFalse(java.util.Arrays.equals(clear,first.content())); assertFalse(java.util.Arrays.equals(first.content(),second.content())); assertArrayEquals(clear,service.decrypt(first.nonce(),first.content()));
  }
}
