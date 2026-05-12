package com.digitalsignage.admin.screengroup.service;

import com.digitalsignage.admin.screengroup.dto.CreateScreenGroupRequest;
import com.digitalsignage.admin.screengroup.dto.ScreenGroupResponse;
import com.digitalsignage.admin.screengroup.dto.UpdateScreenGroupRequest;

import java.util.List;

public interface ScreenGroupService {

    List<ScreenGroupResponse> listGroups();

    ScreenGroupResponse getGroup(Long id);

    ScreenGroupResponse createGroup(CreateScreenGroupRequest request);

    ScreenGroupResponse updateGroup(Long id, UpdateScreenGroupRequest request);

    void deleteGroup(Long id);
}
