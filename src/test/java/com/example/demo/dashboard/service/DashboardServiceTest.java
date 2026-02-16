package com.example.demo.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dashboard.dto.DashboardResponseDto;
import com.example.demo.meal.repository.MealLogRepository;
import com.example.demo.record.entity.Record;
import com.example.demo.record.repository.RecordRepository;
import com.example.demo.training.repository.TrainingLogRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MealLogRepository mealLogRepository;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private TrainingLogRepository trainingLogRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getTodayDashboard_success() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        when(mealLogRepository.sumCaloriesByDate(today)).thenReturn(2000);
        when(mealLogRepository.sumProteinByDate(today)).thenReturn(120);
        when(mealLogRepository.countByDate(today)).thenReturn(3L);

        Record todayRecord = new Record();
        todayRecord.setWeight(70.0);

        Record yesterdayRecord = new Record();
        yesterdayRecord.setWeight(70.5);

        when(recordRepository.findByDate(today))
                .thenReturn(Optional.of(todayRecord));

        when(recordRepository.findTopByDateOrderByIdDesc(yesterday))
                .thenReturn(Optional.of(yesterdayRecord));

        when(trainingLogRepository.sumCaloriesByDate(today))
                .thenReturn(500);

        // when
        DashboardResponseDto result = dashboardService.getTodayDashboard();

        // then
        assertThat(result.getDate()).isEqualTo(today);
        assertThat(result.getTotalCalories()).isEqualTo(2000);
        assertThat(result.getTotalProtein()).isEqualTo(120);
        assertThat(result.getMealCount()).isEqualTo(3);
        assertThat(result.getTodayWeight()).isEqualTo(70.0);
        assertThat(result.getWeightDiffFromYesterday()).isEqualTo(-0.5);
        assertThat(result.getTotalTrainingCalories()).isEqualTo(500);
    }

    @Test
    void getTodayDashboard_noData() {
        // given
        LocalDate today = LocalDate.now();

        when(mealLogRepository.sumCaloriesByDate(today)).thenReturn(null);
        when(mealLogRepository.sumProteinByDate(today)).thenReturn(null);
        when(mealLogRepository.countByDate(today)).thenReturn(null);

        when(recordRepository.findByDate(today)).thenReturn(Optional.empty());
        when(recordRepository.findTopByDateOrderByIdDesc(today.minusDays(1)))
                .thenReturn(Optional.empty());

        when(trainingLogRepository.sumCaloriesByDate(today)).thenReturn(null);

        // when
        DashboardResponseDto result = dashboardService.getTodayDashboard();

        // then
        assertThat(result.getDate()).isEqualTo(today);
        assertThat(result.getTotalCalories()).isZero();
        assertThat(result.getTotalProtein()).isZero();
        assertThat(result.getMealCount()).isZero();
        assertThat(result.getTodayWeight()).isNull();
        assertThat(result.getWeightDiffFromYesterday()).isNull();
        assertThat(result.getTotalTrainingCalories()).isZero();
    }

    @Test
    void getTodayDashboard_withWeightDiff() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Record todayRecord = new Record();
        todayRecord.setWeight(70.0);

        Record yesterdayRecord = new Record();
        yesterdayRecord.setWeight(70.5);

        when(mealLogRepository.sumCaloriesByDate(today)).thenReturn(1800);
        when(mealLogRepository.sumProteinByDate(today)).thenReturn(120);
        when(mealLogRepository.countByDate(today)).thenReturn(3L);

        when(recordRepository.findByDate(today))
                .thenReturn(Optional.of(todayRecord));

        when(recordRepository.findTopByDateOrderByIdDesc(yesterday))
                .thenReturn(Optional.of(yesterdayRecord));

        when(trainingLogRepository.sumCaloriesByDate(today)).thenReturn(400);

        // when
        DashboardResponseDto result = dashboardService.getTodayDashboard();

        // then
        assertThat(result.getTodayWeight()).isEqualTo(70.0);
        assertThat(result.getWeightDiffFromYesterday()).isEqualTo(-0.5);
    }
}
