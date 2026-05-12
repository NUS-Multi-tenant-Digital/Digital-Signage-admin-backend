package com.digitalsignage.admin.screengroup.controller;

import com.digitalsignage.admin.common.api.ApiResponse;
import com.digitalsignage.admin.screengroup.dto.CreateScreenGroupRequest;
import com.digitalsignage.admin.screengroup.dto.ScreenGroupResponse;
import com.digitalsignage.admin.screengroup.dto.UpdateScreenGroupRequest;
import com.digitalsignage.admin.screengroup.service.ScreenGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.admin-prefix}")
@RequiredArgsConstructor
public class ScreenGroupController {

    private final ScreenGroupService screenGroupService;

    @GetMapping("/screen-groups")
    public ApiResponse<List<ScreenGroupResponse>> list() {
        return ApiResponse.ok(screenGroupService.listGroups());
    }

    @GetMapping("/screen-groups/{id}")
    public ApiResponse<ScreenGroupResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(screenGroupService.getGroup(id));
    }

    @PostMapping("/screen-groups")
    public ApiResponse<ScreenGroupResponse> create(@Valid @RequestBody CreateScreenGroupRequest request) {
        return ApiResponse.ok(screenGroupService.createGroup(request));
    }

    @PutMapping("/screen-groups/{id}")
    public ApiResponse<ScreenGroupResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScreenGroupRequest request) {
        return ApiResponse.ok(screenGroupService.updateGroup(id, request));
    }

    @DeleteMapping("/screen-groups/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        screenGroupService.deleteGroup(id);
        return ApiResponse.ok();
    }
}
