package com.akbori.dgtrainapp.service;

import com.akbori.dgtrainapp.dto.AdItem;
import com.akbori.dgtrainapp.dto.GameStartResponse;
import com.akbori.dgtrainapp.dto.ReputationResponse;
import com.akbori.dgtrainapp.dto.SolutionResponse;
import com.akbori.dgtrainapp.dto.TradeResponse;
import com.akbori.dgtrainapp.service.api.MugloarApiException;
import com.akbori.dgtrainapp.service.api.MugloarError;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(MugloarService.class)
class MugloarServiceTest {

    private static final String BASE_URL = "https://dragonsofmugloar.com/api/v2";

    @Autowired
    private MugloarService mugloarService;

    @Autowired
    private MockRestServiceServer server;

    // Runs the call and checks it failed with the given reason and a message containing the given text
    private void assertFails(ThrowingCallable call, MugloarError error, String expectedMessage) {
        assertThatThrownBy(call)
                .isInstanceOf(MugloarApiException.class)
                .hasMessageContaining(expectedMessage)
                .satisfies(e -> assertThat(((MugloarApiException) e).getError()).isEqualTo(error));
        server.verify();
    }

    @Test
    void startNewGameReturnsParsedGameSession() {
        server.expect(requestTo(BASE_URL + "/game/start"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {"gameId":"abc123","lives":3,"gold":0,"level":0,
                         "score":0,"highScore":0,"turn":0}
                        """, MediaType.APPLICATION_JSON));

        GameStartResponse response = mugloarService.startNewGame();

        assertThat(response.gameId()).isEqualTo("abc123");
        assertThat(response.lives()).isEqualTo(3);
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({
            "500, SERVER_ERROR, Mugloar server error (500) while starting a new game.",
            "503, SERVER_ERROR, Mugloar server error (503) while starting a new game.",
            "400, BAD_REQUEST, Bad request while starting a new game.",
            "418, UNEXPECTED_STATUS, Unexpected response (418) while starting a new game."
    })
    void startNewGameThrowsOnErrorStatus(int status, MugloarError error, String expectedMessage) {
        server.expect(requestTo(BASE_URL + "/game/start"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertFails(() -> mugloarService.startNewGame(), error, expectedMessage);
    }

    @Test
    void startNewGameThrowsWhenBodyHasNoGameId() {
        server.expect(requestTo(BASE_URL + "/game/start"))
                .andRespond(withSuccess("""
                        {"lives":3,"gold":0,"level":0,"score":0,"highScore":0,"turn":0}
                        """, MediaType.APPLICATION_JSON));

        assertFails(() -> mugloarService.startNewGame(), MugloarError.MISSING_GAME_ID, "did not return a game ID");
    }

    @Test
    void networkFailureIsReportedAsNetworkUnavailable() {
        server.expect(requestTo(BASE_URL + "/game/start"))
                .andRespond(withException(new IOException("Connection refused")));

        assertFails(() -> mugloarService.startNewGame(), MugloarError.NETWORK_UNAVAILABLE,
                "Could not reach the Mugloar server while starting a new game");
    }

    @Test
    void unreadableBodyIsReportedAsUnreadableResponse() {
        server.expect(requestTo(BASE_URL + "/abc123/solve/XFyAyerI"))
                .andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        assertFails(() -> mugloarService.solveAd("abc123", "XFyAyerI"), MugloarError.UNREADABLE_RESPONSE,
                "could not be read while solving ad XFyAyerI");
    }

    @Test
    void errorKeepsTheServerStatus() {
        server.expect(requestTo(BASE_URL + "/abc123/shop"))
                .andRespond(withStatus(HttpStatus.GONE));

        assertThatThrownBy(() -> mugloarService.getMarketItems("abc123"))
                .isInstanceOfSatisfying(MugloarApiException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(410);
                    assertThat(e.getError().getResponseStatus()).isEqualTo(HttpStatus.GONE);
                });
    }

    @ParameterizedTest
    @CsvSource({
            "400, BAD_REQUEST, Bad request while fetching the shop items for game abc123",
            "404, GAME_NOT_FOUND, Game abc123 was not found or is already over",
            "410, GAME_EXPIRED, Game abc123 has expired",
            "500, SERVER_ERROR, Mugloar server error (500) while fetching the shop items",
            "418, UNEXPECTED_STATUS, Unexpected response (418) while fetching the shop items"
    })
    void getMarketItemsThrowsOnErrorStatus(int status, MugloarError error, String expectedMessage) {
        server.expect(requestTo(BASE_URL + "/abc123/shop"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertFails(() -> mugloarService.getMarketItems("abc123"), error, expectedMessage);
    }

    @Test
    void getMessageBoardReturnsParsedBoardOfMessages() {
        String gameId = "abc123";

        server.expect(requestTo(BASE_URL + "/" + gameId + "/messages"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"adId":"XFyAyerI",
                          "message":"Create an advertisement campaign for Florian Milford to promote their house based business",
                          "reward":3,"expiresIn":7,"encrypted":null,"probability":"Sure thing"}]
                        """, MediaType.APPLICATION_JSON));

        List<AdItem> messages = mugloarService.getMessageBoard(gameId);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).adId()).isEqualTo("XFyAyerI");
        server.verify();
    }

    @Test
    void getMessageBoardReturnsEmptyListForAnEmptyBody() {
        server.expect(requestTo(BASE_URL + "/abc123/messages"))
                .andRespond(withSuccess());

        assertThat(mugloarService.getMessageBoard("abc123")).isEmpty();
        server.verify();
    }

    @Test
    void getMessageBoardDecodesBase64AndRot13Ads() {
        String gameId = "abc123";

        // encrypted arrives as a JSON number: 1 = Base64, 2 = ROT13
        server.expect(requestTo(BASE_URL + "/" + gameId + "/messages"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"adId":"WEZ5QXllckk=","message":"U3RlYWwgc3VwZXIgYXdlc29tZSBkaWFtb25k",
                          "reward":90,"expiresIn":5,"encrypted":1,"probability":"U3VpY2lkZSBtaXNzaW9u"},
                         {"adId":"KSlNlreV","message":"Fgrny fhcre njrfbzr qvnzbaq",
                          "reward":80,"expiresIn":4,"encrypted":2,"probability":"Fhvpvqr zvffvba"}]
                        """, MediaType.APPLICATION_JSON));

        List<AdItem> messages = mugloarService.getMessageBoard(gameId);

        assertThat(messages).hasSize(2).allSatisfy(ad -> {
            assertThat(ad.adId()).isEqualTo("XFyAyerI");
            assertThat(ad.message()).isEqualTo("Steal super awesome diamond");
            assertThat(ad.probability()).isEqualTo("Suicide mission");
            assertThat(ad.encrypted()).isNull();
        });
        server.verify();
    }

    @Test
    void decodeAdFallsBackToRot13WhenMarkerIsUnknownAndBase64Fails() {
        AdItem ad = new AdItem("KSlNlreV", "Fgrny fhcre njrfbzr qvnzbaq", 80, 4, "3", "Fhvpvqr zvffvba");

        AdItem decoded = MugloarService.decodeAd(ad);

        assertThat(decoded.message()).isEqualTo("Steal super awesome diamond");
        assertThat(decoded.probability()).isEqualTo("Suicide mission");
    }

    @ParameterizedTest
    @CsvSource({
            "400, BAD_REQUEST, Bad request while fetching the message board for game abc123",
            "404, GAME_NOT_FOUND, Game abc123 was not found or is already over",
            "410, GAME_EXPIRED, Game abc123 has expired",
            "500, SERVER_ERROR, Mugloar server error (500) while fetching the message board",
            "503, SERVER_ERROR, Mugloar server error (503) while fetching the message board",
            "418, UNEXPECTED_STATUS, Unexpected response (418) while fetching the message board"
    })
    void getMessageBoardThrowsOnErrorStatus(int status, MugloarError error, String expectedMessage) {
        server.expect(requestTo(BASE_URL + "/abc123/messages"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertFails(() -> mugloarService.getMessageBoard("abc123"), error, expectedMessage);
    }

    @Test
    void solveAdReturnsParsedSolutionResponse() {
        String gameId = "abc123";
        String adId = "XFyAyerI";

        server.expect(requestTo(BASE_URL + "/" + gameId + "/solve/" + adId))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"success":true,"lives":3,"gold":13,"score":10,
                         "highScore":0,"turn":1,"message":"You successfully solved the mission!"}
                        """, MediaType.APPLICATION_JSON));

        SolutionResponse response = mugloarService.solveAd(gameId, adId);

        assertThat(response.success()).isTrue();
        assertThat(response.gold()).isEqualTo(13);
        assertThat(response.score()).isEqualTo(10);
        assertThat(response.turn()).isEqualTo(1);
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({
            "400, BAD_REQUEST, Ad XFyAyerI could not be solved (unknown or already expired) for game abc123",
            "404, GAME_NOT_FOUND, Game abc123 was not found or is already over",
            "410, GAME_EXPIRED, Game abc123 has expired",
            "500, SERVER_ERROR, Mugloar server error (500) while solving ad XFyAyerI",
            "503, SERVER_ERROR, Mugloar server error (503) while solving ad XFyAyerI",
            "418, UNEXPECTED_STATUS, Unexpected response (418) while solving ad XFyAyerI"
    })
    void solveAdThrowsOnErrorStatus(int status, MugloarError error, String expectedMessage) {
        server.expect(requestTo(BASE_URL + "/abc123/solve/XFyAyerI"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertFails(() -> mugloarService.solveAd("abc123", "XFyAyerI"), error, expectedMessage);
    }

    @Test
    void investigateReputationReturnsParsedReputationResponse() {
        String gameId = "abc123";

        server.expect(requestTo(BASE_URL + "/" + gameId + "/investigate/reputation"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"people":1.5,"state":-2.0,"underworld":0.25}
                        """, MediaType.APPLICATION_JSON));

        ReputationResponse response = mugloarService.investigateReputation(gameId);

        assertThat(response.people()).isEqualTo(1.5);
        assertThat(response.state()).isEqualTo(-2.0);
        assertThat(response.underworld()).isEqualTo(0.25);
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({
            "400, BAD_REQUEST, Bad request while investigating reputation for game abc123",
            "404, GAME_NOT_FOUND, Game abc123 was not found or is already over",
            "410, GAME_EXPIRED, Game abc123 has expired",
            "500, SERVER_ERROR, Mugloar server error (500) while investigating reputation",
            "503, SERVER_ERROR, Mugloar server error (503) while investigating reputation",
            "418, UNEXPECTED_STATUS, Unexpected response (418) while investigating reputation"
    })
    void investigateReputationThrowsOnErrorStatus(int status, MugloarError error, String expectedMessage) {
        server.expect(requestTo(BASE_URL + "/abc123/investigate/reputation"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertFails(() -> mugloarService.investigateReputation("abc123"), error, expectedMessage);
    }

    @Test
    void buyItemReturnsParsedTradeResponse() {
        server.expect(requestTo(BASE_URL + "/abc123/shop/buy/hpot"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"shoppingSuccess":true,"gold":50,"lives":4,"level":1,"turn":3}
                        """, MediaType.APPLICATION_JSON));

        TradeResponse response = mugloarService.buyItem("abc123", "hpot");

        assertThat(response.shoppingSuccess()).isTrue();
        assertThat(response.lives()).isEqualTo(4);
        assertThat(response.level()).isEqualTo(1);
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({
            "400, BAD_REQUEST, Item hpot could not be bought for game abc123",
            "404, GAME_NOT_FOUND, Game abc123 was not found or is already over",
            "410, GAME_EXPIRED, Game abc123 has expired",
            "502, SERVER_ERROR, Mugloar server error (502) while buying item hpot",
            "418, UNEXPECTED_STATUS, Unexpected response (418) while buying item hpot"
    })
    void buyItemThrowsOnErrorStatus(int status, MugloarError error, String expectedMessage) {
        server.expect(requestTo(BASE_URL + "/abc123/shop/buy/hpot"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertFails(() -> mugloarService.buyItem("abc123", "hpot"), error, expectedMessage);
    }
}
