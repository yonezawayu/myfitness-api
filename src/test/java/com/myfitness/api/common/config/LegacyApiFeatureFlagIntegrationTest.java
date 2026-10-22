package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.myfitness.api.auth.controller.AuthController;
import com.myfitness.api.dashboard.controller.DashboardController;
import com.myfitness.api.dashboard.service.DashboardService;
import com.myfitness.api.food.controller.FoodController;
import com.myfitness.api.food.service.FoodService;
import com.myfitness.api.meal.controller.MealItemController;
import com.myfitness.api.meal.controller.MealLogController;
import com.myfitness.api.meal.service.MealItemService;
import com.myfitness.api.meal.service.MealLogService;
import com.myfitness.api.practice.controller.MemoController;
import com.myfitness.api.practice.service.MemoService;
import com.myfitness.api.record.controller.RecordController;
import com.myfitness.api.record.service.RecordService;
import com.myfitness.api.training.controller.TrainingLogController;
import com.myfitness.api.training.controller.TrainingSessionController;
import com.myfitness.api.training.service.TrainingLogService;
import com.myfitness.api.user.controller.UserController;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:legacy-api-feature-flag-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "jwt.secret=TEST_ONLY_KEY_FOR_LEGACY_API_FEATURE_FLAG_TESTS_1234567890"
})
@AutoConfigureMockMvc
class LegacyApiFeatureFlagIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Test
    void legacyControllersAreDisabledByDefaultAndCoreControllersRemainRegistered() {
        assertThat(applicationContext.getBeansOfType(RecordController.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(TrainingLogController.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(MealLogController.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(MealItemController.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(DashboardController.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(MemoController.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(FoodController.class)).isEmpty();

        assertThat(applicationContext.getBeansOfType(TrainingSessionController.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(AuthController.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(UserController.class)).hasSize(1);
    }

    @Test
    @WithMockUser
    void authenticatedUserCannotAccessDisabledLegacyApis() throws Exception {
        mockMvc.perform(get("/records")).andExpect(status().isNotFound());
        mockMvc.perform(get("/training-logs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/meal-logs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/meal-items/meal-log/1")).andExpect(status().isNotFound());
        mockMvc.perform(get("/dashboard/today")).andExpect(status().isNotFound());
        mockMvc.perform(get("/practice/memos")).andExpect(status().isNotFound());
        mockMvc.perform(get("/foods")).andExpect(status().isNotFound());
    }

    @Test
    void missingPropertiesDoNotRegisterAnyLegacyController() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerMockServices(context);
            registerLegacyControllers(context);
            context.refresh();

            assertThat(context.getBeansOfType(RecordController.class)).isEmpty();
            assertThat(context.getBeansOfType(TrainingLogController.class)).isEmpty();
            assertThat(context.getBeansOfType(MealLogController.class)).isEmpty();
            assertThat(context.getBeansOfType(MealItemController.class)).isEmpty();
            assertThat(context.getBeansOfType(DashboardController.class)).isEmpty();
            assertThat(context.getBeansOfType(MemoController.class)).isEmpty();
            assertThat(context.getBeansOfType(FoodController.class)).isEmpty();
        }
    }

    @Test
    void explicitTrueRegistersEveryLegacyController() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            TestPropertyValues.of(
                    "myfitness.features.records.enabled=true",
                    "myfitness.features.legacy-training.enabled=true",
                    "myfitness.features.meal-logs.enabled=true",
                    "myfitness.features.meal-items.enabled=true",
                    "myfitness.features.dashboard.enabled=true",
                    "myfitness.features.practice-memos.enabled=true",
                    "myfitness.features.foods.enabled=true")
                    .applyTo(context);

            registerMockServices(context);
            registerLegacyControllers(context);
            context.refresh();

            assertThat(context.getBeansOfType(RecordController.class)).hasSize(1);
            assertThat(context.getBeansOfType(TrainingLogController.class)).hasSize(1);
            assertThat(context.getBeansOfType(MealLogController.class)).hasSize(1);
            assertThat(context.getBeansOfType(MealItemController.class)).hasSize(1);
            assertThat(context.getBeansOfType(DashboardController.class)).hasSize(1);
            assertThat(context.getBeansOfType(MemoController.class)).hasSize(1);
            assertThat(context.getBeansOfType(FoodController.class)).hasSize(1);
        }
    }

    private void registerMockServices(AnnotationConfigApplicationContext context) {
        context.registerBean(RecordService.class, () -> mock(RecordService.class));
        context.registerBean(TrainingLogService.class, () -> mock(TrainingLogService.class));
        context.registerBean(MealLogService.class, () -> mock(MealLogService.class));
        context.registerBean(MealItemService.class, () -> mock(MealItemService.class));
        context.registerBean(DashboardService.class, () -> mock(DashboardService.class));
        context.registerBean(MemoService.class, () -> mock(MemoService.class));
        context.registerBean(FoodService.class, () -> mock(FoodService.class));
    }

    private void registerLegacyControllers(AnnotationConfigApplicationContext context) {
        context.register(
                RecordController.class,
                TrainingLogController.class,
                MealLogController.class,
                MealItemController.class,
                DashboardController.class,
                MemoController.class,
                FoodController.class);
    }
}
