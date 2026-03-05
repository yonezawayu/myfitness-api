package com.myfitness.api.dashboard.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.myfitness.api.dashboard.dto.DashboardResponseDto;
import com.myfitness.api.meal.repository.MealLogRepository;
import com.myfitness.api.record.entity.Record;
import com.myfitness.api.record.repository.RecordRepository;
import com.myfitness.api.training.repository.TrainingLogRepository;

@Service
public class DashboardService {

        private final MealLogRepository mealLogRepository;
        private final RecordRepository recordRepository;
        private final TrainingLogRepository trainingLogRepository;

        public DashboardService(
                        MealLogRepository mealLogRepository,
                        RecordRepository recordRepository,
                        TrainingLogRepository trainingLogRepository) {
                this.mealLogRepository = mealLogRepository;
                this.recordRepository = recordRepository;
                this.trainingLogRepository = trainingLogRepository;
        }

        public DashboardResponseDto getTodayDashboard() {
                return buildDashboard(LocalDate.now());
        }

        public DashboardResponseDto getDashboardByDate(LocalDate date) {
                return buildDashboard(date);
        }

        private DashboardResponseDto buildDashboard(LocalDate date) {
                LocalDate yesterday = date.minusDays(1);

                Integer totalCalories = defaultZero(mealLogRepository.sumCaloriesByDate(date));
                Integer totalProtein = defaultZero(mealLogRepository.sumProteinByDate(date));
                Long mealCount = defaultZero(mealLogRepository.countByDate(date));
                Integer trainingCalories = defaultZero(trainingLogRepository.sumCaloriesByDate(date));

                Double todayWeight = recordRepository.findByDate(date)
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
                                diff,
                                trainingCalories);
        }

        private int defaultZero(Integer value) {
                return value == null ? 0 : value;
        }

        private long defaultZero(Long value) {
                return value == null ? 0L : value;
        }
}
