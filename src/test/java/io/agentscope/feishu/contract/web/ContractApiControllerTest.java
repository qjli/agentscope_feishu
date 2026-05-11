package io.agentscope.feishu.contract.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContractApiController.class)
class ContractApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void infoDemoContract() throws Exception {
        mockMvc.perform(get("/api/contract/info").param("contractCode", "HT-20250908192882"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractCode").value("HT-20250908192882"))
                .andExpect(jsonPath("$.contractName").exists())
                .andExpect(jsonPath("$.partyA").exists());
    }
}
