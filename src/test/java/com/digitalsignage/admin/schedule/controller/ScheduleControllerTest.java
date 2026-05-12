package com.digitalsignage.admin.schedule.controller;

import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import com.digitalsignage.admin.common.exception.GlobalExceptionHandler;
import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.schedule.dto.CreateScheduleRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckResponse;
import com.digitalsignage.admin.schedule.dto.ScheduleResponse;
import com.digitalsignage.admin.schedule.dto.UpdateScheduleRequest;
import com.digitalsignage.admin.schedule.service.ScheduleService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ScheduleController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@WithMockUser(roles = {"ADMIN"})
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private JwtService jwtService;

    private static ScheduleResponse sample() {
        return ScheduleResponse.builder()
                .id(1L)
                .name("Morning")
                .targetType("SCREEN")
                .screenId(10L)
                .layoutId(2L)
                .playlistId(3L)
                .startDatetime(LocalDateTime.parse("2026-01-01T08:00:00"))
                .endDatetime(LocalDateTime.parse("2026-01-01T18:00:00"))
                .priority(1)
                .status("ACTIVE")
                .build();
    }

    @Test
    void list_returnsOk() throws Exception {
        when(scheduleService.listSchedules()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/admin/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Morning"));
    }

    @Test
    void get_returnsOk() throws Exception {
        when(scheduleService.getSchedule(1L)).thenReturn(sample());

        mockMvc.perform(get("/api/admin/schedules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void create_returnsOk() throws Exception {
        when(scheduleService.createSchedule(any(CreateScheduleRequest.class))).thenReturn(sample());

        CreateScheduleRequest req = new CreateScheduleRequest();
        req.setName("Morning");
        req.setTargetType(com.digitalsignage.admin.common.enums.ScheduleTargetType.SCREEN);
        req.setScreenId(10L);
        req.setLayoutId(2L);
        req.setPlaylistId(3L);
        req.setStartDatetime(LocalDateTime.parse("2026-01-01T08:00:00"));
        req.setEndDatetime(LocalDateTime.parse("2026-01-01T18:00:00"));
        req.setPriority(1);
        req.setStatus(ScheduleStatus.ACTIVE);

        mockMvc.perform(post("/api/admin/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void update_returnsOk() throws Exception {
        when(scheduleService.updateSchedule(eq(1L), any(UpdateScheduleRequest.class))).thenReturn(sample());

        UpdateScheduleRequest req = new UpdateScheduleRequest();
        req.setName("Morning 2");
        req.setTargetType(ScheduleTargetType.SCREEN);
        req.setScreenId(10L);
        req.setLayoutId(2L);
        req.setPlaylistId(3L);
        req.setStartDatetime(LocalDateTime.parse("2026-01-01T08:00:00"));
        req.setEndDatetime(LocalDateTime.parse("2026-01-01T18:00:00"));
        req.setPriority(1);
        req.setStatus(ScheduleStatus.ACTIVE);

        mockMvc.perform(put("/api/admin/schedules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returnsOk() throws Exception {
        doNothing().when(scheduleService).deleteSchedule(2L);

        mockMvc.perform(delete("/api/admin/schedules/2"))
                .andExpect(status().isOk());
        verify(scheduleService).deleteSchedule(2L);
    }

    @Test
    void checkConflict_returnsOk() throws Exception {
        when(scheduleService.checkConflict(any(ScheduleConflictCheckRequest.class)))
                .thenReturn(ScheduleConflictCheckResponse.builder().conflict(false).build());

        ScheduleConflictCheckRequest req = new ScheduleConflictCheckRequest();
        req.setTargetType(ScheduleTargetType.SCREEN);
        req.setScreenId(1L);
        req.setStartDatetime(LocalDateTime.parse("2026-01-01T08:00:00"));
        req.setEndDatetime(LocalDateTime.parse("2026-01-01T12:00:00"));

        mockMvc.perform(post("/api/admin/schedules/check-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conflict").value(false));
    }

    @Test
    void resolve_returnsOk() throws Exception {
        when(scheduleService.resolveForScreen(eq(10L), isNull()))
                .thenReturn(ActiveConfigResponse.builder().scheduleId(5L).build());

        mockMvc.perform(get("/api/admin/schedules/resolve").param("screenId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleId").value(5));
    }
}
