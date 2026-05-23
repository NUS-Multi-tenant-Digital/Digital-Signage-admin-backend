package com.digitalsignage.admin.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void sanitize_null_returnsNull() {
        assertThat(LogSanitizer.sanitize(null)).isNull();
    }

    @Test
    void sanitize_plainText_unchanged() {
        assertThat(LogSanitizer.sanitize("user@example.com")).isEqualTo("user@example.com");
    }

    @Test
    void sanitize_replacesControlCharacters() {
        assertThat(LogSanitizer.sanitize("a\nb\rc\t")).isEqualTo("a_b_c_");
        assertThat(LogSanitizer.sanitize("x\u0001y")).isEqualTo("x_y");
    }
}
