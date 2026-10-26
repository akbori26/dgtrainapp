package com.akbori.dgtrainapp.service.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;

class MugloarErrorTest {

    @ParameterizedTest
    @CsvSource({
            "400, BAD_REQUEST",
            "404, GAME_NOT_FOUND",
            "410, GAME_EXPIRED",
            "500, SERVER_ERROR",
            "503, SERVER_ERROR",
            "401, UNEXPECTED_STATUS",
            "418, UNEXPECTED_STATUS",
            "302, UNEXPECTED_STATUS"
    })
    void mapsEveryServerStatusToOneError(int status, MugloarError expected) {
        assertThat(MugloarError.fromStatus(HttpStatusCode.valueOf(status))).isEqualTo(expected);
    }
}
