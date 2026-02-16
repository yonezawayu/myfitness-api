package com.example.demo.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dashboard.dto.DashboardResponseDto;
import com.example.demo.dashboard.service.DashboardService;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DashboardService dashboardService;

        @Test
        void getTodayDashboard_success() throws Exception {
                // given
                DashboardResponseDto response = new DashboardResponseDto(
                                LocalDate.of(2025, 12, 12),
                                2000, // totalCalories
                                120, // totalProtein
                                3L, // mealCount
                                70.0, // todayWeight
                                -0.5, // weightDiffFromYesterday
                                500 // totalTrainingCalories
                );

                when(dashboardService.getTodayDashboard())
                                .thenReturn(response);

                // when & then
                mockMvc.perform(get("/dashboard/today"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.date").value("2025-12-12"))
                                .andExpect(jsonPath("$.totalCalories").value(2000))
                                .andExpect(jsonPath("$.totalProtein").value(120))
                                .andExpect(jsonPath("$.mealCount").value(3))
                                .andExpect(jsonPath("$.todayWeight").value(70.0))
                                .andExpect(jsonPath("$.weightDiffFromYesterday").value(-0.5))
                                .andExpect(jsonPath("$.totalTrainingCalories").value(500));
        }

        @Test
        void getTodayDashboard_internalServerError() throws Exception {
                when(dashboardService.getTodayDashboard())
                                .thenThrow(new RuntimeException("Internal Server Error"));

                mockMvc.perform(get("/dashboard/today"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.message").value("Internal Server Error"));
        }

        @Test
        void getTodayDashboard_serviceReturnsNull() throws Exception {
                // given
                when(dashboardService.getTodayDashboard())
                                .thenReturn(null);

                // when & then
                mockMvc.perform(get("/dashboard/today"))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        void getTodayDashboard_serviceThrowsResponseStatusException() throws Exception {
                // given
                when(dashboardService.getTodayDashboard())
                                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                                                "Service unavailable"));

                // when & then
                mockMvc.perform(get("/dashboard/today"))
                                .andExpect(status().isServiceUnavailable());
        }

}
