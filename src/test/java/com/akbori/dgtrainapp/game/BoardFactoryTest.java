package com.akbori.dgtrainapp.game;

import com.akbori.dgtrainapp.dto.AdItem;
import com.akbori.dgtrainapp.dto.ShopItem;
import com.akbori.dgtrainapp.model.CachedAd;
import com.akbori.dgtrainapp.model.CachedItem;
import com.akbori.dgtrainapp.model.RiskCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class BoardFactoryTest {

    private static AdItem ad(String adId, String probability) {
        return new AdItem(adId, "Help a farmer", 25, 7, null, probability);
    }

    @Test
    void messageBoardNumbersAdsAndSetsRiskAndTurn() {
        List<CachedAd> rows = BoardFactory.messageBoard("g", List.of(ad("a1", "Piece of cake"), ad("a2", "Impossible")),
                List.of(), 4).orElseThrow();

        assertThat(rows).extracting(CachedAd::getDisplayIndex).containsExactly(0, 1);
        assertThat(rows).extracting(CachedAd::getRiskCategory).containsExactly(RiskCategory.SAFE, RiskCategory.FATAL);
        assertThat(rows).extracting(CachedAd::getFetchedTurn).containsOnly(4);
        assertThat(rows).extracting(CachedAd::isUsed).containsOnly(false);
    }

    @Test
    void messageBoardKeepsUsedFlagsForAdsListedAgain() {
        CachedAd used = new CachedAd("g", 0, ad("a1", "Risky"), 0);
        used.setUsed(true);

        List<CachedAd> rows = BoardFactory.messageBoard("g", List.of(ad("a2", "Risky"), ad("a1", "Risky")),
                List.of(used), 2).orElseThrow();

        assertThat(rows).extracting(CachedAd::getAdId, CachedAd::isUsed)
                .containsExactly(tuple("a2", false), tuple("a1", true));
    }

    @Test
    void emptyDownloadKeepsTheStoredBoard() {
        assertThat(BoardFactory.messageBoard("g", List.of(), List.of(), 0)).isEqualTo(Optional.empty());
        assertThat(BoardFactory.messageBoard("g", null, List.of(), 0)).isEmpty();
    }

    @Test
    void marketIsDownloadedOnlyWhenNothingIsStored() {
        List<CachedItem> market = BoardFactory.market("g", List.of(new ShopItem("hpot", "Healing potion", 50)));

        assertThat(BoardFactory.marketNeedsDownload(List.of())).isTrue();
        assertThat(BoardFactory.marketNeedsDownload(market)).isFalse();
        assertThat(market).extracting(CachedItem::getItemId, CachedItem::getDisplayIndex)
                .containsExactly(tuple("hpot", 0));
    }
}