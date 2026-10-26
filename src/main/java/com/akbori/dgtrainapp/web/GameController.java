package com.akbori.dgtrainapp.web;

import com.akbori.dgtrainapp.dto.ReputationResponse;
import com.akbori.dgtrainapp.service.GameService;
import com.akbori.dgtrainapp.web.dto.ActionResult;
import com.akbori.dgtrainapp.web.dto.AdView;
import com.akbori.dgtrainapp.web.dto.GameStatusView;
import com.akbori.dgtrainapp.web.dto.HistoryEntryView;
import com.akbori.dgtrainapp.web.dto.PurchaseOutcome;
import com.akbori.dgtrainapp.web.dto.ShopItemView;
import com.akbori.dgtrainapp.web.dto.SolveOutcome;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * The game's HTTP API: one endpoint per button or command, for any front end.
 * It only translates between HTTP and GameService; errors are answered by ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/games")
@Tag(name = "Game", description = "Play Dragons of Mugloar: start a game, solve ads, shop and review the history")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @Operation(summary = "Start a new game", description = "Also makes it the game returned by GET /api/games/current.")
    public ResponseEntity<GameStatusView> startGame(UriComponentsBuilder uriBuilder) {
        GameStatusView status = gameService.startGame();
        return ResponseEntity
                .created(uriBuilder.path("/api/games/{gameId}").buildAndExpand(status.gameId()).toUri())
                .body(status);
    }

    @GetMapping("/current")
    @Operation(summary = "Resume the last game started")
    public GameStatusView currentGame() {
        return gameService.currentGame();
    }

    @GetMapping("/{gameId}")
    @Operation(summary = "Check the player's status")
    public GameStatusView status(@PathVariable String gameId) {
        return gameService.status(gameId);
    }

    @GetMapping("/{gameId}/ads")
    @Operation(summary = "View the stored message board", description = "Empty until the board is downloaded.")
    public List<AdView> board(@PathVariable String gameId) {
        return gameService.board(gameId);
    }

    @PostMapping("/{gameId}/ads/refresh")
    @Operation(summary = "Download a new message board from the server")
    public List<AdView> refreshBoard(@PathVariable String gameId) {
        return gameService.refreshBoard(gameId);
    }

    @PostMapping("/{gameId}/ads/{adId}/solve")
    @Operation(summary = "Send the dragon on an ad", description = "Refused if the ad is used, expired or not on the board.")
    public ActionResult<SolveOutcome> solveAd(@PathVariable String gameId, @PathVariable String adId) {
        return gameService.solveAd(gameId, adId);
    }

    @PostMapping("/{gameId}/reputation")
    @Operation(summary = "Investigate the dragon's reputation", description = "Costs a turn.")
    public ActionResult<ReputationResponse> investigateReputation(@PathVariable String gameId) {
        return gameService.investigateReputation(gameId);
    }

    @GetMapping("/{gameId}/shop")
    @Operation(summary = "View the shop", description = "Downloaded from the server the first time for each game.")
    public List<ShopItemView> shop(@PathVariable String gameId) {
        return gameService.shop(gameId);
    }

    @PostMapping("/{gameId}/shop/{itemId}/buy")
    @Operation(summary = "Buy a shop item", description = "Refused if the player doesn't have enough gold.")
    public ActionResult<PurchaseOutcome> buyItem(@PathVariable String gameId, @PathVariable String itemId) {
        return gameService.buyItem(gameId, itemId);
    }

    @GetMapping("/{gameId}/history")
    @Operation(summary = "View the missions and purchases of the game, newest first")
    public List<HistoryEntryView> history(@PathVariable String gameId) {
        return gameService.history(gameId);
    }
}
