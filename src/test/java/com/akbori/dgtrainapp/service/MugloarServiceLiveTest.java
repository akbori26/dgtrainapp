package com.akbori.dgtrainapp.service;

import com.akbori.dgtrainapp.dto.AdItem;
import com.akbori.dgtrainapp.dto.GameStartResponse;
import com.akbori.dgtrainapp.dto.ReputationResponse;
import com.akbori.dgtrainapp.dto.ShopItem;
import com.akbori.dgtrainapp.dto.SolutionResponse;
import com.akbori.dgtrainapp.dto.TradeResponse;
import com.akbori.dgtrainapp.service.api.MugloarApiException;
import com.akbori.dgtrainapp.service.api.MugloarError;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Calls the real Dragons of Mugloar server, so it needs network access.
 * Each test starts a fresh game, which has 0 gold, so no purchase can go through
 * and the tests never depend on a stored game ID.
 */
@Tag("live")
class MugloarServiceLiveTest {

    private static final String BASE_URL = "https://dragonsofmugloar.com/api/v2";
    // The server has been seen answering an unknown game ID with either 400 or 404
    private static final String UNKNOWN_GAME_ID = "noSuchGame";

    private MugloarService mugloarService;
    private RestClient rawClient;
    private String gameId;

    @BeforeEach
    void startGame() {
        mugloarService = new MugloarService(RestClient.builder());
        rawClient = RestClient.create(BASE_URL);
        gameId = mugloarService.startNewGame().gameId();
    }

    /**
     * Returns the ID of the first ad on the board that is not encrypted,
     * since encrypted ads need their ID decoded before they can be solved.
     */
    private String firstPlainAdId() {
        return mugloarService.getMessageBoard(gameId).stream()
                .filter(ad -> ad.encrypted() == null)
                .map(AdItem::adId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No unencrypted ad on the board for game " + gameId));
    }

    // --- Game start ---

    @Test
    void startEndpointRespondsWithOkAndGameJson() {
        ResponseEntity<String> response = rawClient.post()
                .uri("/game/start")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"gameId\"", "\"lives\"", "\"gold\"", "\"level\"", "\"score\"", "\"highScore\"", "\"turn\"");
    }

    @Test
    void startNewGameReturnsFreshGameState() {
        GameStartResponse res = mugloarService.startNewGame();

        assertThat(res.gameId()).isNotBlank().isNotEqualTo(gameId);
        assertThat(res.lives()).isEqualTo(3);
        assertThat(res.gold()).isZero();
        assertThat(res.level()).isZero();
        assertThat(res.score()).isZero();
        assertThat(res.turn()).isZero();
    }

    // --- Message board ---

    @Test
    void messagesEndpointRespondsWithOkAndAdJson() {
        ResponseEntity<String> response = rawClient.get()
                .uri("/{gameId}/messages", gameId)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"adId\"", "\"message\"", "\"reward\"", "\"expiresIn\"", "\"probability\"");
    }

    @Test
    void getMessageBoardReturnsAdsWithIds() {
        List<AdItem> ads = mugloarService.getMessageBoard(gameId);

        assertThat(ads).isNotEmpty();
        assertThat(ads).allSatisfy(ad -> assertThat(ad.adId()).isNotBlank());
    }

    // --- Solve ---

    @Test
    void solveEndpointRespondsWithOkAndSolutionJson() {
        String adId = firstPlainAdId();

        ResponseEntity<String> response = rawClient.post()
                .uri("/{gameId}/solve/{adId}", gameId, adId)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"success\"", "\"lives\"", "\"gold\"", "\"score\"", "\"turn\"", "\"message\"");
    }

    @Test
    void solveAdMapsServerResponseToSolutionResponse() {
        SolutionResponse res = mugloarService.solveAd(gameId, firstPlainAdId());

        assertThat(res).isNotNull();
        // Solving always uses up a turn, whether it succeeds or not
        assertThat(res.turn()).isGreaterThan(0);
        assertThat(res.message()).isNotBlank();
    }

    // --- Reputation ---

    @Test
    void reputationEndpointRespondsWithOkAndReputationJson() {
        ResponseEntity<String> response = rawClient.post()
                .uri("/{gameId}/investigate/reputation", gameId)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"people\"", "\"state\"", "\"underworld\"");
    }

    @Test
    void investigateReputationMapsServerResponseToReputationResponse() {
        ReputationResponse res = mugloarService.investigateReputation(gameId);

        assertThat(res).isNotNull();
        // A new game starts with neutral standing
        assertThat(res.people()).isZero();
        assertThat(res.state()).isZero();
        assertThat(res.underworld()).isZero();
    }

    // --- Shop ---

    @Test
    void shopEndpointRespondsWithOkAndItemJson() {
        ResponseEntity<String> response = rawClient.get()
                .uri("/{gameId}/shop", gameId)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"id\"", "\"name\"", "\"cost\"");
    }

    @Test
    void buyEndpointRespondsWithOkAndTradeJson() {
        ResponseEntity<String> response = rawClient.post()
                .uri("/{gameId}/shop/buy/{itemId}", gameId, "hpot")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"shoppingSuccess\"", "\"gold\"", "\"lives\"", "\"level\"", "\"turn\"");
    }

    @Test
    void buyItemMapsServerResponseToTradeResponse() {
        TradeResponse res = mugloarService.buyItem(gameId, "hpot");

        assertThat(res).isNotNull();
        // A new game cannot afford anything
        assertThat(res.shoppingSuccess()).isFalse();
        assertThat(res.lives()).isEqualTo(3);
    }

    @Test
    void getMarketItemsReturnsItemsWithIds() {
        List<ShopItem> items = mugloarService.getMarketItems(gameId);

        assertThat(items).isNotEmpty();
        assertThat(items).allSatisfy(item -> assertThat(item.id()).isNotBlank());
    }

    // --- Unknown game ---

    @ParameterizedTest
    @CsvSource({
            "GET,  /{gameId}/messages",
            "POST, /{gameId}/solve/abc",
            "POST, /{gameId}/investigate/reputation",
            "GET,  /{gameId}/shop",
            "POST, /{gameId}/shop/buy/hpot"
    })
    void endpointsRejectUnknownGame(String method, String path) {
        // exchange() returns the status without throwing on 4xx
        int status = rawClient.method(HttpMethod.valueOf(method))
                .uri(path, UNKNOWN_GAME_ID)
                .exchange((request, response) -> response.getStatusCode().value());

        assertThat(status).isIn(400, 404);
    }

    @Test
    void everyServiceCallThrowsForUnknownGame() {
        assertRejectsUnknownGame(() -> mugloarService.getMessageBoard(UNKNOWN_GAME_ID));
        assertRejectsUnknownGame(() -> mugloarService.solveAd(UNKNOWN_GAME_ID, "abc"));
        assertRejectsUnknownGame(() -> mugloarService.investigateReputation(UNKNOWN_GAME_ID));
        assertRejectsUnknownGame(() -> mugloarService.getMarketItems(UNKNOWN_GAME_ID));
        assertRejectsUnknownGame(() -> mugloarService.buyItem(UNKNOWN_GAME_ID, "hpot"));
    }

    private static void assertRejectsUnknownGame(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(MugloarApiException.class, e ->
                        assertThat(e.getError()).isIn(MugloarError.BAD_REQUEST, MugloarError.GAME_NOT_FOUND))
                .hasMessageContaining(UNKNOWN_GAME_ID);
    }
}
