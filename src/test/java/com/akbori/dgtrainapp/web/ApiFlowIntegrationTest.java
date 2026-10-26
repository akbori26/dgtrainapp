package com.akbori.dgtrainapp.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A whole game through the HTTP API: the real controller, service, rules and SQLite storage,
 * with only the Mugloar server mocked. Uses a throwaway in-memory database built from the real DDL script.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:apiflowtest?mode=memory&cache=shared",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:data/mugloar_schema.sql"
})
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
class ApiFlowIntegrationTest {

    private static final String MUGLOAR = "https://dragonsofmugloar.com/api/v2";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MockRestServiceServer mugloar;

    @Test
    void playsAGameFromStartToPurchase() throws Exception {
        mugloar.expect(requestTo(MUGLOAR + "/game/start")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"gameId":"flow1","lives":3,"gold":0,"level":0,"score":0,"highScore":0,"turn":0}
                        """, MediaType.APPLICATION_JSON));
        mugloar.expect(requestTo(MUGLOAR + "/flow1/messages")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"adId":"a1","message":"Help a farmer","reward":60,"expiresIn":5,"encrypted":null,"probability":"Piece of cake"}]
                        """, MediaType.APPLICATION_JSON));
        mugloar.expect(requestTo(MUGLOAR + "/flow1/solve/a1")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"success":true,"lives":3,"gold":60,"score":20,"highScore":0,"turn":1,"message":"You did it!"}
                        """, MediaType.APPLICATION_JSON));
        mugloar.expect(requestTo(MUGLOAR + "/flow1/shop")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"hpot","name":"Healing potion","cost":50}]
                        """, MediaType.APPLICATION_JSON));
        mugloar.expect(requestTo(MUGLOAR + "/flow1/shop/buy/hpot")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"shoppingSuccess":true,"gold":10,"lives":4,"level":0,"turn":2}
                        """, MediaType.APPLICATION_JSON));

        mvc.perform(post("/api/games"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value("flow1"));

        mvc.perform(get("/api/games/current"))
                .andExpect(jsonPath("$.gameId").value("flow1"));

        mvc.perform(post("/api/games/flow1/ads/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adId").value("a1"))
                .andExpect(jsonPath("$[0].risk.code").value("SAFE"));

        mvc.perform(post("/api/games/flow1/ads/a1/solve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome.success").value(true))
                .andExpect(jsonPath("$.status.gold").value(60));

        // The same ad can't be solved twice, and the server is not asked
        mvc.perform(post("/api/games/flow1/ads/a1/solve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AD_ALREADY_USED"));

        mvc.perform(get("/api/games/flow1/shop"))
                .andExpect(jsonPath("$[0].itemId").value("hpot"))
                .andExpect(jsonPath("$[0].affordable").value(true));

        mvc.perform(post("/api/games/flow1/shop/hpot/buy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome.message").value("Bought Healing potion for 50 gold"))
                .andExpect(jsonPath("$.status.lives").value(4));

        mvc.perform(get("/api/games/flow1/history"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("PURCHASE"))
                .andExpect(jsonPath("$[0].goldChange").value(-50))
                .andExpect(jsonPath("$[1].type").value("MISSION"))
                .andExpect(jsonPath("$[1].goldChange").value(60));

        mugloar.verify();
    }

    @Test
    void swaggerUiAndApiDocsAreServed() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Dragon Trainer API"))
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/ads/{adId}/solve']").exists());
    }
}
