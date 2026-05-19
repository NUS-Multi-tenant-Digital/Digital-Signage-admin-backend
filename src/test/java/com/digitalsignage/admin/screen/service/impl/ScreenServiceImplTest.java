package com.digitalsignage.admin.screen.service.impl;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.common.enums.ScreenStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.enums.WsStatus;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.entity.ScreenGroup;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.dto.ActivationCodeResponse;
import com.digitalsignage.admin.screen.dto.CreateScreenRequest;
import com.digitalsignage.admin.screen.dto.ScreenResponse;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.AdminPrincipal;
import com.digitalsignage.admin.screengroup.repository.ScreenGroupRepository;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreenServiceImplTest {

    private static final Long ORG_ID = 10L;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private ScreenGroupRepository screenGroupRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private ScreenServiceImpl screenService;

    private Organization organization;
    private Screen screen;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(ORG_ID);

        screen = new Screen();
        screen.setId(1L);
        screen.setOrganization(organization);
        screen.setDeviceCode("dev-001");
        screen.setName("Lobby");
        screen.setActivationStatus(ActivationStatus.PENDING);
        screen.setStatus(ScreenStatus.OFFLINE);
        screen.setWsStatus(WsStatus.DISCONNECTED);

        AdminPrincipal principal = AdminPrincipal.builder()
                .userId(1L)
                .organizationId(ORG_ID)
                .username("admin")
                .role(UserRole.ADMIN)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listScreens_success() {
        when(screenRepository.findByOrganization_IdOrderByUpdatedAtDesc(ORG_ID)).thenReturn(List.of(screen));

        List<ScreenResponse> responses = screenService.listScreens();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getDeviceCode()).isEqualTo("dev-001");
    }

    @Test
    void createScreen_duplicateDeviceCode_throws409() {
        CreateScreenRequest request = new CreateScreenRequest();
        request.setDeviceCode("dev-001");
        request.setName("New Screen");

        when(screenRepository.existsByDeviceCode("dev-001")).thenReturn(true);

        assertThatThrownBy(() -> screenService.createScreen(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 409);
        verify(screenRepository, never()).save(any());
    }

    @Test
    void createScreen_success() {
        CreateScreenRequest request = new CreateScreenRequest();
        request.setDeviceCode("dev-new");
        request.setName("New Screen");

        when(screenRepository.existsByDeviceCode("dev-new")).thenReturn(false);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));

        Screen saved = new Screen();
        saved.setId(2L);
        saved.setOrganization(organization);
        saved.setDeviceCode("dev-new");
        saved.setName("New Screen");
        saved.setActivationStatus(ActivationStatus.PENDING);
        saved.setStatus(ScreenStatus.OFFLINE);
        saved.setWsStatus(WsStatus.DISCONNECTED);

        when(screenRepository.save(any(Screen.class))).thenReturn(saved);

        ScreenResponse response = screenService.createScreen(request);

        assertThat(response.getDeviceCode()).isEqualTo("dev-new");
    }

    @Test
    void deleteScreen_referencedBySchedule_throws409() {
        when(screenRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(screen));
        when(scheduleRepository.existsByScreen_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> screenService.deleteScreen(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 409);
        verify(screenRepository, never()).delete(any());
    }

    @Test
    void generateActivationCode_success() {
        when(screenRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(screen));
        when(screenRepository.save(any(Screen.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivationCodeResponse response = screenService.generateActivationCode(1L);

        assertThat(response.getActivationCode()).hasSize(12);
        assertThat(screen.getActivationStatus()).isEqualTo(ActivationStatus.PENDING);
        assertThat(screen.getDeviceToken()).isNull();
    }

    @Test
    void getScreen_notFound_throws404() {
        when(screenRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> screenService.getScreen(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void createScreen_withGroup_success() {
        CreateScreenRequest request = new CreateScreenRequest();
        request.setDeviceCode("dev-g");
        request.setName("Grouped");
        request.setScreenGroupId(5L);

        ScreenGroup group = new ScreenGroup();
        group.setId(5L);
        group.setOrganization(organization);
        group.setName("Floor 1");

        when(screenRepository.existsByDeviceCode("dev-g")).thenReturn(false);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(screenGroupRepository.findByIdAndOrganization_Id(5L, ORG_ID)).thenReturn(Optional.of(group));

        Screen saved = new Screen();
        saved.setId(3L);
        saved.setOrganization(organization);
        saved.setDeviceCode("dev-g");
        saved.setName("Grouped");
        saved.setScreenGroup(group);
        saved.setActivationStatus(ActivationStatus.PENDING);
        saved.setStatus(ScreenStatus.OFFLINE);
        saved.setWsStatus(WsStatus.DISCONNECTED);

        when(screenRepository.save(any(Screen.class))).thenReturn(saved);

        ScreenResponse response = screenService.createScreen(request);

        assertThat(response.getScreenGroupId()).isEqualTo(5L);
    }
}
