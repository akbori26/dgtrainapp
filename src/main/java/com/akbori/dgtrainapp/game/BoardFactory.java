package com.akbori.dgtrainapp.game;

import com.akbori.dgtrainapp.dto.AdItem;
import com.akbori.dgtrainapp.dto.ShopItem;
import com.akbori.dgtrainapp.model.CachedAd;
import com.akbori.dgtrainapp.model.CachedItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the rows stored in cached_ad and cached_item from what the Mugloar server sends (Factory pattern),
 * and decides when a board has to be downloaded instead of read from the database.
 */
public final class BoardFactory {

    private BoardFactory() {}

    /**
     * Builds a new message board from downloaded ads, to replace the stored one.
     * Each ad's position becomes its display index, its risk category comes from its probability,
     * and the current turn is stored so the expiry countdown starts from now. Ads already used
     * keep their used flag if the server lists them again.
     *
     * @param gameId      Game the board belongs to
     * @param downloaded  Ads from the server, in board order
     * @param stored      The game's board as stored now, to carry over used flags
     * @param currentTurn Player turn now
     * @return The new rows, or empty if nothing was downloaded and the stored board should be kept
     */
    public static Optional<List<CachedAd>> messageBoard(String gameId, List<AdItem> downloaded,
                                                         List<CachedAd> stored, int currentTurn) {
        if (downloaded == null || downloaded.isEmpty()) return Optional.empty();

        Set<String> usedAdIds = stored.stream()
                .filter(CachedAd::isUsed)
                .map(CachedAd::getAdId)
                .collect(Collectors.toSet());

        List<CachedAd> rows = new ArrayList<>();
        for (int i = 0; i < downloaded.size(); i++) {
            AdItem ad = downloaded.get(i);
            CachedAd row = new CachedAd(gameId, i, ad, currentTurn);
            row.setUsed(usedAdIds.contains(ad.adId()));
            rows.add(row);
        }
        return Optional.of(rows);
    }

    /**
     * The shop list does not change during a game, so the market is downloaded only once per game.
     *
     * @param stored The game's market as stored now
     * @return true if the market has to be downloaded from the server
     */
    public static boolean marketNeedsDownload(List<CachedItem> stored) {
        return stored.isEmpty();
    }

    /**
     * Builds the market rows from downloaded shop items; each item's position becomes its display index.
     *
     * @param gameId     Game the market belongs to
     * @param downloaded Shop items from the server, in board order
     * @return The rows to store, empty if nothing was downloaded
     */
    public static List<CachedItem> market(String gameId, List<ShopItem> downloaded) {
        List<CachedItem> rows = new ArrayList<>();
        if (downloaded == null) return rows;
        for (int i = 0; i < downloaded.size(); i++) {
            rows.add(new CachedItem(gameId, i, downloaded.get(i)));
        }
        return rows;
    }
}