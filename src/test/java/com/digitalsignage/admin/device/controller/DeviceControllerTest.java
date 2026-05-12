package com.digitalsignage.admin.device.controller;

import com.digitalsignage.admin.common.exception.GlobalExceptionHandler;
import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.device.dto.DeviceActivateRequest;
import com.digitalsignage.admin.device.dto.DeviceActivateResponse;
import com.digitalsignage.admin.device.dto.DeviceHeartbeatRequest;
import com.digitalsignage.admin.device.dto.PlaybackLogSubmitRequest;
import com.digitalsignage.admin.device.service.DeviceService;
import com.digitalsignage.admin.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeviceController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeviceService deviceService;

    @MockBean
    private JwtService jwtService;

    @Test
    void activate_returnsOk() throws Exception {
        when(deviceService.activate(any(DeviceActivateRequest.class))).thenReturn(
                DeviceActivateResponse.builder().deviceToken("tok").screenId(9L).build());

        DeviceActivateRequest req = new DeviceActivateRequest();
        req.setDeviceCode("DEV1");
        req.setActivationCode("ABC");

        mockMvc.perform(post("/api/device/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.screenId").value(9))
                .andExpect(jsonPath("$.data.deviceToken").value("tok"));
    }

    @Test
    void activeConfig_returnsOk() throws Exception {
        when(deviceService.getActiveConfig()).thenReturn(
                ActiveConfigResponse.builder().scheduleId(1L).resolvedAt(LocalDateTime.parse("2026-01-01T12:00:00")).build());

        mockMvc.perform(get("/api/device/active-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleId").value(1));
    }

    @Test
    void heartbeat_returnsOk() throws Exception {
        doNothing().when(deviceService).heartbeat(any(DeviceHeartbeatRequest.class));

        DeviceHeartbeatRequest req = new DeviceHeartbeatRequest();
        req.setRuntimeHealthy(true);

        mockMvc.perform(post("/api/device/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(deviceService).heartbeat(any(DeviceHeartbeatRequest.class));
    }

    @Test
    void heartbeat_emptyBody_ok() throws Exception {
        doNothing().when(deviceService).heartbeat(any(DeviceHeartbeatRequest.class));

        mockMvc.perform(post("/api/device/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void playbackLogs_returnsOk() throws Exception {
        doNothing().when(deviceService).submitPlaybackLogs(any(PlaybackLogSubmitRequest.class));

        PlaybackLogSubmitRequest req = new PlaybackLogSubmitRequest();
        PlaybackLogSubmitRequest.Entry e = new PlaybackLogSubmitRequest.Entry();
        e.setMediaId(1L);
        e.setPlaylistId(2L);
        e.setPlayedAt(LocalDateTime.parse("2026-01-02T10:00:00"));
        req.setEntries(List.of(e));

        mockMvc.perform(post("/api/device/playback-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
