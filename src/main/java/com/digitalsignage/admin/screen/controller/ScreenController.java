package com.digitalsignage.admin.screen.controller;

import com.digitalsignage.admin.common.api.ApiResponse;
import com.digitalsignage.admin.screen.dto.ActivationCodeResponse;
import com.digitalsignage.admin.screen.dto.AssignScreenGroupRequest;
import com.digitalsignage.admin.screen.dto.CreateScreenRequest;
import com.digitalsignage.admin.screen.dto.ScreenResponse;
import com.digitalsignage.admin.screen.dto.UpdateScreenRequest;
import com.digitalsignage.admin.screen.service.ScreenService;
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
public class ScreenController {

    private final ScreenService screenService;

    @GetMapping("/screens")
    public ApiResponse<List<ScreenResponse>> list() {
        return ApiResponse.ok(screenService.listScreens());
    }

    @GetMapping("/screens/{id}")
    public ApiResponse<ScreenResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(screenService.getScreen(id));
    }

    @PostMapping("/screens")
    public ApiResponse<ScreenResponse> create(@Valid @RequestBody CreateScreenRequest request) {
        return ApiResponse.ok(screenService.createScreen(request));
    }

    @PutMapping("/screens/{id}")
    public ApiResponse<ScreenResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScreenRequest request) {
        return ApiResponse.ok(screenService.updateScreen(id, request));
    }

    @DeleteMapping("/screens/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        screenService.deleteScreen(id);
        return ApiResponse.ok();
    }

    @PutMapping("/screens/{id}/group")
    public ApiResponse<ScreenResponse> assignGroup(
            @PathVariable Long id,
            @Valid @RequestBody AssignScreenGroupRequest request) {
        return ApiResponse.ok(screenService.assignGroup(id, request));
    }

    @PostMapping("/screens/{id}/activation-code")
    public ApiResponse<ActivationCodeResponse> activationCode(@PathVariable Long id) {
        return ApiResponse.ok(screenService.generateActivationCode(id));
    }
}
