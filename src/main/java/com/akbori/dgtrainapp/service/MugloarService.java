package com.akbori.dgtrainapp.service;

import com.akbori.dgtrainapp.dto.*;
import com.akbori.dgtrainapp.service.api.MugloarApiException;
import com.akbori.dgtrainapp.service.api.MugloarOperation;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.akbori.dgtrainapp.service.api.MugloarOperation.*;

/**
 * Calls the Mugloar REST API. Every call goes through execute(), so a failure is always reported
 * the same way: as a MugloarApiException with the reason and a message for the player.
 */
@Service
public class MugloarService {

    private static final ParameterizedTypeReference<List<AdItem>> AD_LIST = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ShopItem>> SHOP_LIST = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public MugloarService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://dragonsofmugloar.com/api/v2")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Starts a new game.
     *
     * @return The new game's ID and starting state
     * @throws MugloarApiException if the call fails or the response has no game ID
     */
    public GameStartResponse startNewGame() {
        GameStartResponse response = execute(START_GAME, restClient.post().uri("/game/start"), GameStartResponse.class);
        if (response == null || response.gameId() == null) throw MugloarApiException.missingGameId();
        return response;
    }

    /**
     * Investigates the dragon's reputation with the people, the state and the underworld. Costs a turn.
     *
     * @param gameId Active game ID
     * @throws MugloarApiException if the call fails
     */
    public ReputationResponse investigateReputation(String gameId) {
        return execute(INVESTIGATE_REPUTATION,
                restClient.post().uri("/{gameId}/investigate/reputation", gameId), ReputationResponse.class, gameId);
    }

    /**
     * Downloads the ads on the message board, with encrypted ads decoded.
     *
     * @param gameId Active game ID
     * @return The ads in board order, empty if the server sent none
     * @throws MugloarApiException if the call fails
     */
    public List<AdItem> getMessageBoard(String gameId) {
        List<AdItem> ads = execute(FETCH_MESSAGES, restClient.get().uri("/{gameId}/messages", gameId), AD_LIST, gameId);
        if (ads == null) return List.of();
        return new BoardOfMessages(ads.stream().map(MugloarService::decodeAd).toList()).messages();
    }

    /**
     * Sends the dragon to solve an ad.
     *
     * @param gameId Active game ID
     * @param adId   ID of the ad to solve
     * @throws MugloarApiException if the call fails
     */
    public SolutionResponse solveAd(String gameId, String adId) {
        return execute(SOLVE_AD,
                restClient.post().uri("/{gameId}/solve/{adId}", gameId, adId), SolutionResponse.class, gameId, adId);
    }

    /**
     * Downloads the items for sale in the shop.
     *
     * @param gameId Active game ID
     * @return The shop items in board order, empty if the server sent none
     * @throws MugloarApiException if the call fails
     */
    public List<ShopItem> getMarketItems(String gameId) {
        List<ShopItem> items = execute(FETCH_SHOP, restClient.get().uri("/{gameId}/shop", gameId), SHOP_LIST, gameId);
        if (items == null) return List.of();
        return new BoardOfMarket(items).shopItems();
    }

    /**
     * Buys an item from the shop.
     *
     * @param gameId Active game ID
     * @param itemId ID of the item to buy
     * @throws MugloarApiException if the call fails
     */
    public TradeResponse buyItem(String gameId, String itemId) {
        return execute(BUY_ITEM,
                restClient.post().uri("/{gameId}/shop/buy/{itemId}", gameId, itemId), TradeResponse.class, gameId, itemId);
    }

    private <T> T execute(MugloarOperation operation, RestClient.RequestHeadersSpec<?> request,
                          Class<T> type, String... ids) {
        return execute(operation, request, response -> response.bodyTo(type), ids);
    }

    private <T> T execute(MugloarOperation operation, RestClient.RequestHeadersSpec<?> request,
                          ParameterizedTypeReference<T> type, String... ids) {
        return execute(operation, request, response -> response.bodyTo(type), ids);
    }

    /**
     * Runs one API call: reads the body on a 2xx status, and turns every failure into a MugloarApiException.
     * exchange() hands over the raw response, so the status is checked before any body mapping.
     *
     * @param operation  Which call this is, for the error message
     * @param request    The prepared request
     * @param bodyReader Reads the body into the expected type
     * @param ids        Game ID, then the ad or item ID if the call has one
     * @return The body, or null if a successful response had none
     */
    private <T> T execute(MugloarOperation operation, RestClient.RequestHeadersSpec<?> request,
                          Function<ConvertibleClientHttpResponse, T> bodyReader, String... ids) {
        try {
            return request.exchange((req, response) -> {
                if (response.getStatusCode().is2xxSuccessful()) return bodyReader.apply(response);
                throw MugloarApiException.fromStatus(operation, response.getStatusCode(), ids);
            });
        } catch (ResourceAccessException e) {
            throw MugloarApiException.networkUnavailable(operation, e, ids);
        } catch (RestClientException e) {
            throw MugloarApiException.unreadableResponse(operation, e, ids);
        }
    }

    /**
     * Returns the ad with adId, message and probability decoded, so callers only ever see plain text.
     * The server marks encrypted ads with encrypted = 1 (Base64) or 2 (ROT13).
     * An unknown marker is decoded as Base64 when that works, otherwise as ROT13.
     *
     * @param ad Ad as received from the server
     * @return The same ad in plain text, with encrypted set to null
     */
    static AdItem decodeAd(AdItem ad) {
        if (ad.encrypted() == null) return ad;

        UnaryOperator<String> decoder = switch (ad.encrypted()) {
            case "1" -> MugloarService::decodeBase64;
            case "2" -> MugloarService::rot13;
            default -> value -> {
                try {
                    return decodeBase64(value);
                } catch (IllegalArgumentException e) {
                    return rot13(value);
                }
            };
        };

        return new AdItem(decoder.apply(ad.adId()), decoder.apply(ad.message()), ad.reward(),
                ad.expiresIn(), null, decoder.apply(ad.probability()));
    }

    private static String decodeBase64(String value) {
        if (value == null) return null;
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String rot13(String value) {
        if (value == null) return null;
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c >= 'a' && c <= 'z') sb.append((char) ('a' + (c - 'a' + 13) % 26));
            else if (c >= 'A' && c <= 'Z') sb.append((char) ('A' + (c - 'A' + 13) % 26));
            else sb.append(c);
        }
        return sb.toString();
    }
}
