package com.digitalsignage.admin.screen.service.impl;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.entity.ScreenGroup;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.dto.ActivationCodeResponse;
import com.digitalsignage.admin.screen.dto.AssignScreenGroupRequest;
import com.digitalsignage.admin.screen.dto.CreateScreenRequest;
import com.digitalsignage.admin.screen.dto.ScreenResponse;
import com.digitalsignage.admin.screen.dto.UpdateScreenRequest;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.screen.service.ScreenService;
import com.digitalsignage.admin.common.util.RandomCodes;
import com.digitalsignage.admin.security.SecurityUtils;
import com.digitalsignage.admin.screengroup.repository.ScreenGroupRepository;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final ScreenGroupRepository screenGroupRepository;
    private final ScheduleRepository scheduleRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ScreenResponse> listScreens() {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        return screenRepository.findByOrganization_IdOrderByUpdatedAtDesc(organizationId).stream()
                .map(ScreenResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScreenResponse getScreen(Long id) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Screen screen = screenRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
        return ScreenResponse.fromEntity(screen);
    }

    @Override
    @Transactional
    public ScreenResponse createScreen(CreateScreenRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        if (screenRepository.existsByDeviceCode(request.getDeviceCode())) {
            throw new BusinessException(409, "device code already exists");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(400, "organization not found"));
        Screen screen = new Screen();
        screen.setOrganization(organization);
        screen.setDeviceCode(request.getDeviceCode().trim());
        screen.setName(request.getName().trim());
        if (request.getScreenGroupId() != null) {
            ScreenGroup group = screenGroupRepository.findByIdAndOrganization_Id(request.getScreenGroupId(), organizationId)
                    .orElseThrow(() -> new BusinessException(404, "screen group not found"));
            screen.setScreenGroup(group);
        }
        Screen saved = screenRepository.save(screen);
        return ScreenResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ScreenResponse updateScreen(Long id, UpdateScreenRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Screen screen = screenRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
        if (StringUtils.hasText(request.getName())) {
            screen.setName(request.getName().trim());
        }
        if (request.getScreenGroupId() != null) {
            ScreenGroup group = screenGroupRepository.findByIdAndOrganization_Id(request.getScreenGroupId(), organizationId)
                    .orElseThrow(() -> new BusinessException(404, "screen group not found"));
            screen.setScreenGroup(group);
        }
        Screen saved = screenRepository.save(screen);
        return ScreenResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteScreen(Long id) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Screen screen = screenRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
        if (scheduleRepository.existsByScreen_Id(screen.getId())) {
            throw new BusinessException(409, "screen is referenced by schedules");
        }
        screenRepository.delete(screen);
    }

    @Override
    @Transactional
    public ScreenResponse assignGroup(Long id, AssignScreenGroupRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Screen screen = screenRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
        if (request.getScreenGroupId() == null) {
            screen.setScreenGroup(null);
        } else {
            ScreenGroup group = screenGroupRepository.findByIdAndOrganization_Id(request.getScreenGroupId(), organizationId)
                    .orElseThrow(() -> new BusinessException(404, "screen group not found"));
            screen.setScreenGroup(group);
        }
        Screen saved = screenRepository.save(screen);
        return ScreenResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ActivationCodeResponse generateActivationCode(Long id) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Screen screen = screenRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
        String code = RandomCodes.alphanumeric(12);
        screen.setActivationCode(code);
        screen.setActivationStatus(ActivationStatus.PENDING);
        screen.setDeviceToken(null);
        screenRepository.save(screen);
        return ActivationCodeResponse.builder().activationCode(code).build();
    }
}
