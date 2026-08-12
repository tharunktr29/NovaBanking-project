package com.novabank.auth.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {
    @Test
    void hashesAreDeterministicAndDoNotExposeRawToken() {
        var first = TokenHasher.sha256Base64Url("sample-refresh-token");
        var second = TokenHasher.sha256Base64Url("sample-refresh-token");

        assertThat(first).isEqualTo(second);
        assertThat(first).doesNotContain("sample-refresh-token");
    }
}
