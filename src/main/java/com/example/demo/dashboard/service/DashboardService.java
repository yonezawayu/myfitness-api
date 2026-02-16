package com.example.demo.dashboard.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.example.demo.dashboard.dto.DashboardResponseDto;
import com.example.demo.meal.repository.MealLogRepository;
import com.example.demo.record.entity.Record;
import com.example.demo.record.repository.RecordRepository;
import com.example.demo.training.repository.TrainingLogRepository;

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
                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);

                Integer totalCalories = defaultZero(mealLogRepository.sumCaloriesByDate(today));
                Integer totalProtein = defaultZero(mealLogRepository.sumProteinByDate(today));
                Long mealCount = defaultZero(mealLogRepository.countByDate(today));
                Integer trainingCalories = defaultZero(trainingLogRepository.sumCaloriesByDate(today));

                Double todayWeight = recordRepository.findByDate(today)
                                .map(Record::getWeight)
                                .orElse(null);

                Double yesterdayWeight = recordRepository
                                .findTopByDateOrderByIdDesc(yesterday)
                                .map(Record::getWeight)
                                .orElse(null);

                Double diff = (todayWeight != null && yesterdayWeight != null)
                                ? todayWeight - yesterdayWeight
                                : null;

                return new DashboardResponseDto(
                                today,
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
