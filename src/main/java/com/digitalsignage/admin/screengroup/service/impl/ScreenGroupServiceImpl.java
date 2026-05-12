package com.digitalsignage.admin.screengroup.service.impl;

import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.ScreenGroup;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.screengroup.dto.CreateScreenGroupRequest;
import com.digitalsignage.admin.screengroup.dto.ScreenGroupResponse;
import com.digitalsignage.admin.screengroup.dto.UpdateScreenGroupRequest;
import com.digitalsignage.admin.screengroup.repository.ScreenGroupRepository;
import com.digitalsignage.admin.screengroup.service.ScreenGroupService;
import com.digitalsignage.admin.security.SecurityUtils;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenGroupServiceImpl implements ScreenGroupService {

    private final ScreenGroupRepository screenGroupRepository;
    private final ScreenRepository screenRepository;
    private final ScheduleRepository scheduleRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ScreenGroupResponse> listGroups() {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        return screenGroupRepository.findByOrganization_IdOrderByUpdatedAtDesc(organizationId).stream()
                .map(ScreenGroupResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScreenGroupResponse getGroup(Long id) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        ScreenGroup group = screenGroupRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "screen group not found"));
        return ScreenGroupResponse.fromEntity(group);
    }

    @Override
    @Transactional
    public ScreenGroupResponse createGroup(CreateScreenGroupRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(400, "organization not found"));
        ScreenGroup group = new ScreenGroup();
        group.setOrganization(organization);
        group.setName(request.getName().trim());
        group.setLocation(request.getLocation());
        ScreenGroup saved = screenGroupRepository.save(group);
        return ScreenGroupResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ScreenGroupResponse updateGroup(Long id, UpdateScreenGroupRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        ScreenGroup group = screenGroupRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "screen group not found"));
        if (StringUtils.hasText(request.getName())) {
            group.setName(request.getName().trim());
        }
        if (request.getLocation() != null) {
            group.setLocation(request.getLocation());
        }
        ScreenGroup saved = screenGroupRepository.save(group);
        return ScreenGroupResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        ScreenGroup group = screenGroupRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "screen group not found"));
        if (screenRepository.existsByScreenGroup_Id(group.getId())) {
            throw new BusinessException(409, "screen group still has screens");
        }
        if (scheduleRepository.existsByScreenGroup_Id(group.getId())) {
            throw new BusinessException(409, "screen group is referenced by schedules");
        }
        screenGroupRepository.delete(group);
    }
}
