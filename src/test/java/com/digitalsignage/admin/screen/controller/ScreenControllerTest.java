package com.digitalsignage.admin.screen.controller;

import com.digitalsignage.admin.common.enums.ScreenStatus;
import com.digitalsignage.admin.common.exception.GlobalExceptionHandler;
import com.digitalsignage.admin.screen.dto.ActivationCodeResponse;
import com.digitalsignage.admin.screen.dto.AssignScreenGroupRequest;
import com.digitalsignage.admin.screen.dto.CreateScreenRequest;
import com.digitalsignage.admin.screen.dto.ScreenResponse;
import com.digitalsignage.admin.screen.dto.UpdateScreenRequest;
import com.digitalsignage.admin.screen.service.ScreenService;
import com.digitalsignage.admin.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ScreenController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@WithMockUser(roles = {"ADMIN"})
class ScreenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScreenService screenService;

    @MockBean
    private JwtService jwtService;

    private static ScreenResponse sample() {
        return ScreenResponse.builder()
                .id(1L)
                .deviceCode("DEV1")
                .name("Lobby")
                .activationStatus("PENDING")
                .status(ScreenStatus.OFFLINE.name())
                .wsStatus("DISCONNECTED")
                .lastHeartbeatAt(LocalDateTime.parse("2026-01-01T10:00:00"))
                .build();
    }

    @Test
    void list_returnsOk() throws Exception {
        when(screenService.listScreens()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/admin/screens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deviceCode").value("DEV1"));
    }

    @Test
    void get_returnsOk() throws Exception {
        when(screenService.getScreen(1L)).thenReturn(sample());

        mockMvc.perform(get("/api/admin/screens/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void create_returnsOk() throws Exception {
        when(screenService.createScreen(any(CreateScreenRequest.class))).thenReturn(sample());

        CreateScreenRequest req = new CreateScreenRequest();
        req.setDeviceCode("DEV1");
        req.setName("Lobby");

        mockMvc.perform(post("/api/admin/screens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Lobby"));
    }

    @Test
    void update_returnsOk() throws Exception {
        when(screenService.updateScreen(eq(1L), any(UpdateScreenRequest.class))).thenReturn(sample());

        UpdateScreenRequest req = new UpdateScreenRequest();
        req.setName("Lobby 2");

        mockMvc.perform(put("/api/admin/screens/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returnsOk() throws Exception {
        doNothing().when(screenService).deleteScreen(2L);

        mockMvc.perform(delete("/api/admin/screens/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(screenService).deleteScreen(2L);
    }

    @Test
    void assignGroup_returnsOk() throws Exception {
        when(screenService.assignGroup(eq(1L), any(AssignScreenGroupRequest.class))).thenReturn(sample());

        AssignScreenGroupRequest req = new AssignScreenGroupRequest();
        req.setScreenGroupId(5L);

        mockMvc.perform(put("/api/admin/screens/1/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void activationCode_returnsOk() throws Exception {
        when(screenService.generateActivationCode(1L))
                .thenReturn(ActivationCodeResponse.builder().activationCode("CODE12").build());

        mockMvc.perform(post("/api/admin/screens/1/activation-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activationCode").value("CODE12"));
    }
}
