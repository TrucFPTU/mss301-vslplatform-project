package com.vsl.learning.service;

import com.vsl.common.text.VietnameseText;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class VietnameseTextNormalizeTest {

    @ParameterizedTest
    @DisplayName("Chuẩn hóa tên từ vựng: khoảng trắng, ngoặc, dấu /")
    @CsvSource(delimiter = '|', value = {
            "'  con   chuột  '|con chuột",
            "nghé ( trâu con )|nghé (trâu con)",
            "miếng/ bún|miếng / bún",
            "thân thiết /bạn thân|thân thiết / bạn thân",
            "hiểu biết / học hỏi / kiến thức|hiểu biết / học hỏi / kiến thức",
            "Sợ Hãi|Sợ Hãi"
    })
    void normalize(String input, String expected) {
        assertThat(VietnameseText.normalize(input)).isEqualTo(expected);
    }
}
