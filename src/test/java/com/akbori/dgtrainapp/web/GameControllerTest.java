package com.akbori.dgtrainapp.web;

import com.akbori.dgtrainapp.game.LifeState;
import com.akbori.dgtrainapp.game.check.CheckFailure;
import com.akbori.dgtrainapp.game.check.CheckResult;
import com.akbori.dgtrainapp.game.check.GameRuleException;
import com.akbori.dgtrainapp.model.RiskCategory;
import com.akbori.dgtrainapp.service.GameService;
import com.akbori.dgtrainapp.service.api.MugloarApiException;
import com.akbori.dgtrainapp.service.api.MugloarOperation;
import com.akbori.dgtrainapp.web.dto.ActionResult;
import com.akbori.dgtrainapp.web.dto.AdView;
import com.akbori.dgtrainapp.web.dto.GameStatusView;
import com.akbori.dgtrainapp.web.dto.RiskView;
import com.akbori.dgtrainapp.web.dto.SolveOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
class GameControllerTest {

    private static final GameStatusView STATUS =
            new GameStatusView("g1", 1, 3, 25, 0, 10, 10, LifeState.HEALTHY, null);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GameService gameService;

    @Test
    void startGameAnswersCreatedWithTheGameLocation() throws Exception {
        when(gameService.startGame()).thenReturn(STATUS);

        mvc.perform(post("/api/games"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/games/g1"))
                .andExpect(jsonPath("$.gameId").value("g1"))
                .andExpect(jsonPath("$.lifeState").value("HEALTHY"));
    }

    @Test
    void boardListsAdsWithRiskAndExpiry() throws Exception {
        when(gameService.board("g1")).thenReturn(List.of(new AdView("a1", "Help a farmer", 25, "Piece of cake",
                RiskView.of(RiskCategory.SAFE), 2, false, false, true)));

        mvc.perform(get("/api/games/g1/ads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adId").value("a1"))
                .andExpect(jsonPath("$[0].risk.code").value("SAFE"))
                .andExpect(jsonPath("$[0].risk.icon").value("🟢"))
                .andExpect(jsonPath("$[0].turnsLeft").value(2))
                .andExpect(jsonPath("$[0].solvable").value(true));
    }

    @Test
    void solveAdReturnsTheOutcomeAndTheUpdatedStatus() throws Exception {
        when(gameService.solveAd("g1", "a1"))
                .thenReturn(new ActionResult<>(new SolveOutcome("a1", true, 25, "Done!"), STATUS));

        mvc.perform(post("/api/games/g1/ads/a1/solve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome.success").value(true))
                .andExpect(jsonPath("$.status.gold").value(25));
    }

    @Test
    void brokenGameRuleAnswersConflictWithACode() throws Exception {
        when(gameService.solveAd("g1", "a1"))
                .thenThrow(new GameRuleException(CheckResult.fail(CheckFailure.AD_EXPIRED, "a1")));

        mvc.perform(post("/api/games/g1/ads/a1/solve"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.code").value("AD_EXPIRED"))
                .andExpect(jsonPath("$.title").value("Ad expired"))
                .andExpect(jsonPath("$.detail").value(
                        "Ad a1 has expired and can no longer be solved. Pick a different ad, or download a new board."));
    }

    @Test
    void unknownGameAnswersNotFound() throws Exception {
        when(gameService.status("nope"))
                .thenThrow(new GameRuleException(CheckResult.fail(CheckFailure.PLAYER_STATUS_MISSING)));

        mvc.perform(get("/api/games/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAYER_STATUS_MISSING"));
    }

    @Test
    void mugloarServerErrorAnswersBadGatewayWithTheServerStatus() throws Exception {
        when(gameService.refreshBoard("g1"))
                .thenThrow(MugloarApiException.fromStatus(MugloarOperation.FETCH_MESSAGES, HttpStatusCode.valueOf(503), "g1"));

        mvc.perform(post("/api/games/g1/ads/refresh"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("SERVER_ERROR"))
                .andExpect(jsonPath("$.mugloarStatus").value(503))
                .andExpect(jsonPath("$.detail").value("Mugloar server error (503) while fetching the message board."));
    }

    @Test
    void networkFailureAnswersServiceUnavailable() throws Exception {
        when(gameService.startGame())
                .thenThrow(MugloarApiException.networkUnavailable(MugloarOperation.START_GAME, new IOException("down")));

        mvc.perform(post("/api/games"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("NETWORK_UNAVAILABLE"));
    }

    @Test
    void unexpectedErrorHidesTheDetails() throws Exception {
        when(gameService.history("g1")).thenThrow(new IllegalStateException("database file is locked"));

        mvc.perform(get("/api/games/g1/history"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Something went wrong. Please try again."));
    }

    @Test
    void wrongMethodKeepsSpringsOwnStatus() throws Exception {
        mvc.perform(get("/api/games/g1/ads/a1/solve"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void corsAllowsTheConfiguredDevServer() throws Exception {
        mvc.perform(options("/api/games")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsRejectsOtherOrigins() throws Exception {
        mvc.perform(options("/api/games")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
