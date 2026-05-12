package com.digitalsignage.admin.screengroup.controller;

import com.digitalsignage.admin.common.exception.GlobalExceptionHandler;
import com.digitalsignage.admin.screengroup.dto.CreateScreenGroupRequest;
import com.digitalsignage.admin.screengroup.dto.ScreenGroupResponse;
import com.digitalsignage.admin.screengroup.dto.UpdateScreenGroupRequest;
import com.digitalsignage.admin.screengroup.service.ScreenGroupService;
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

@WebMvcTest(controllers = ScreenGroupController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@WithMockUser(roles = {"ADMIN"})
class ScreenGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScreenGroupService screenGroupService;

    @MockBean
    private JwtService jwtService;

    private static ScreenGroupResponse sample() {
        return ScreenGroupResponse.builder().id(1L).name("Floor 1").location("A").build();
    }

    @Test
    void list_returnsOk() throws Exception {
        when(screenGroupService.listGroups()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/admin/screen-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Floor 1"));
    }

    @Test
    void get_returnsOk() throws Exception {
        when(screenGroupService.getGroup(1L)).thenReturn(sample());

        mockMvc.perform(get("/api/admin/screen-groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void create_returnsOk() throws Exception {
        when(screenGroupService.createGroup(any(CreateScreenGroupRequest.class))).thenReturn(sample());

        CreateScreenGroupRequest req = new CreateScreenGroupRequest();
        req.setName("Floor 1");
        req.setLocation("A");

        mockMvc.perform(post("/api/admin/screen-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void update_returnsOk() throws Exception {
        when(screenGroupService.updateGroup(eq(1L), any(UpdateScreenGroupRequest.class))).thenReturn(sample());

        UpdateScreenGroupRequest req = new UpdateScreenGroupRequest();
        req.setName("Floor 1b");

        mockMvc.perform(put("/api/admin/screen-groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returnsOk() throws Exception {
        doNothing().when(screenGroupService).deleteGroup(2L);

        mockMvc.perform(delete("/api/admin/screen-groups/2"))
                .andExpect(status().isOk());
        verify(screenGroupService).deleteGroup(2L);
    }
}
