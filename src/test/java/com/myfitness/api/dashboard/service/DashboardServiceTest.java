package com.myfitness.api.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myfitness.api.dashboard.dto.DashboardResponseDto;
import com.myfitness.api.meal.repository.MealItemRepository;
import com.myfitness.api.meal.repository.MealLogRepository;
import com.myfitness.api.record.entity.Record;
import com.myfitness.api.record.repository.RecordRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

        @Mock
        private MealLogRepository mealLogRepository;

        @Mock
        private MealItemRepository mealItemRepository;

        @Mock
        private RecordRepository recordRepository;

        @InjectMocks
        private DashboardService dashboardService;

        @Test
        void getTodayDashboard_success() {
                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);

                when(mealItemRepository.sumCaloriesByDate(today)).thenReturn(BigDecimal.valueOf(2000));
                when(mealItemRepository.sumProteinByDate(today)).thenReturn(BigDecimal.valueOf(120));
                when(mealLogRepository.countByDate(today)).thenReturn(3L);

                Record todayRecord = new Record();
                todayRecord.setWeight(70.0);

                Record yesterdayRecord = new Record();
                yesterdayRecord.setWeight(70.5);

                when(recordRepository.findTopByDateOrderByIdDesc(today)).thenReturn(Optional.of(todayRecord));
                when(recordRepository.findTopByDateOrderByIdDesc(yesterday)).thenReturn(Optional.of(yesterdayRecord));
                DashboardResponseDto result = dashboardService.getTodayDashboard();

                assertThat(result.getDate()).isEqualTo(today);
                assertThat(result.getTotalCalories()).isEqualTo(2000);
                assertThat(result.getTotalProtein()).isEqualTo(120);
                assertThat(result.getMealCount()).isEqualTo(3);
                assertThat(result.getTodayWeight()).isEqualTo(70.0);
                assertThat(result.getWeightDiffFromYesterday()).isEqualTo(-0.5);
        }

        @Test
        void getTodayDashboard_noData() {
                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);

                when(mealItemRepository.sumCaloriesByDate(today)).thenReturn(BigDecimal.ZERO);
                when(mealItemRepository.sumProteinByDate(today)).thenReturn(BigDecimal.ZERO);
                when(mealLogRepository.countByDate(today)).thenReturn(null);

                when(recordRepository.findTopByDateOrderByIdDesc(today)).thenReturn(Optional.empty());
                when(recordRepository.findTopByDateOrderByIdDesc(yesterday)).thenReturn(Optional.empty());
                DashboardResponseDto result = dashboardService.getTodayDashboard();

                assertThat(result.getDate()).isEqualTo(today);
                assertThat(result.getTotalCalories()).isZero();
                assertThat(result.getTotalProtein()).isZero();
                assertThat(result.getMealCount()).isZero();
                assertThat(result.getTodayWeight()).isNull();
                assertThat(result.getWeightDiffFromYesterday()).isNull();
        }

        @Test
        void getTodayDashboard_withWeightDiff() {
                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);

                Record todayRecord = new Record();
                todayRecord.setWeight(70.0);

                Record yesterdayRecord = new Record();
                yesterdayRecord.setWeight(70.5);

                when(mealItemRepository.sumCaloriesByDate(today)).thenReturn(BigDecimal.valueOf(1800));
                when(mealItemRepository.sumProteinByDate(today)).thenReturn(BigDecimal.valueOf(120));
                when(mealLogRepository.countByDate(today)).thenReturn(3L);

                when(recordRepository.findTopByDateOrderByIdDesc(today)).thenReturn(Optional.of(todayRecord));
                when(recordRepository.findTopByDateOrderByIdDesc(yesterday)).thenReturn(Optional.of(yesterdayRecord));
                DashboardResponseDto result = dashboardService.getTodayDashboard();

                assertThat(result.getTodayWeight()).isEqualTo(70.0);
                assertThat(result.getWeightDiffFromYesterday()).isEqualTo(-0.5);
        }
}
