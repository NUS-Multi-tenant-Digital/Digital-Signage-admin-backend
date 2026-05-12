package com.digitalsignage.admin.screen.service;

import com.digitalsignage.admin.screen.dto.ActivationCodeResponse;
import com.digitalsignage.admin.screen.dto.AssignScreenGroupRequest;
import com.digitalsignage.admin.screen.dto.CreateScreenRequest;
import com.digitalsignage.admin.screen.dto.ScreenResponse;
import com.digitalsignage.admin.screen.dto.UpdateScreenRequest;

import java.util.List;

public interface ScreenService {

    List<ScreenResponse> listScreens();

    ScreenResponse getScreen(Long id);

    ScreenResponse createScreen(CreateScreenRequest request);

    ScreenResponse updateScreen(Long id, UpdateScreenRequest request);

    void deleteScreen(Long id);

    ScreenResponse assignGroup(Long id, AssignScreenGroupRequest request);

    ActivationCodeResponse generateActivationCode(Long id);
}
