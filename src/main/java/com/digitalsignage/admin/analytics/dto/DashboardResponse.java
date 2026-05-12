package com.digitalsignage.admin.analytics.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {

    private long screenTotal;
    private long screenOnline;
    private long screenSuspect;
    private long screenOffline;
    private long screenError;
    private long playsToday;
    private long alertsToday;
}
