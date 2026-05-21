package com.kncn.jwtauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kncn.jwtauth.dto.AuthDtos.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void registerLoginRefreshHappyPath() throws Exception {
        var reg = new RegisterRequest("alice", "supersecret123");
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(reg)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists());

        var login = new LoginRequest("alice", "supersecret123");
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(login)))
            .andExpect(status().isOk());

        var bad = new LoginRequest("alice", "wrongpassword");
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(bad)))
            .andExpect(status().isUnauthorized());
    }
}
