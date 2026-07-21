package com.myfitness.api.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.myfitness.api.dashboard.dto.DashboardResponseDto;
import com.myfitness.api.meal.repository.MealItemRepository;
import com.myfitness.api.meal.repository.MealLogRepository;
import com.myfitness.api.record.entity.Record;
import com.myfitness.api.record.repository.RecordRepository;

@Service
public class DashboardService {

        private final MealLogRepository mealLogRepository;
        private final MealItemRepository mealItemRepository;
        private final RecordRepository recordRepository;

        public DashboardService(
                        MealLogRepository mealLogRepository,
                        MealItemRepository mealItemRepository,
                        RecordRepository recordRepository) {

                this.mealLogRepository = mealLogRepository;
                this.mealItemRepository = mealItemRepository;
                this.recordRepository = recordRepository;
        }

        public DashboardResponseDto getTodayDashboard() {
                return buildDashboard(LocalDate.now());
        }

        public DashboardResponseDto getDashboardByDate(LocalDate date) {
                return buildDashboard(date);
        }

        private DashboardResponseDto buildDashboard(LocalDate date) {

                LocalDate yesterday = date.minusDays(1);

                // 🔥 ここをMealItemベースに変更
                Integer totalCalories = defaultZero(mealItemRepository.sumCaloriesByDate(date));
                Integer totalProtein = defaultZero(mealItemRepository.sumProteinByDate(date));

                // 食事回数はMealLogのままでOK
                Long mealCount = defaultZero(mealLogRepository.countByDate(date));

                Double todayWeight = recordRepository.findTopByDateOrderByIdDesc(date)
                                .map(Record::getWeight)
                                .orElse(null);

                Double yesterdayWeight = recordRepository.findTopByDateOrderByIdDesc(yesterday)
                                .map(Record::getWeight)
                                .orElse(null);

                Double diff = (todayWeight != null && yesterdayWeight != null)
                                ? todayWeight - yesterdayWeight
                                : null;

                return new DashboardResponseDto(
                                date,
                                totalCalories,
                                totalProtein,
                                mealCount,
                                todayWeight,
                                diff);
        }

        // ===== null対策 =====

        private int defaultZero(Integer value) {
                return value == null ? 0 : value;
        }

        private long defaultZero(Long value) {
                return value == null ? 0L : value;
        }

        private int defaultZero(BigDecimal value) {
                return value == null ? 0 : value.intValue();
        }
}
