package com.example.demo.dashboard.controller;

import com.example.demo.common.dto.ErrorResponse;
import com.example.demo.dashboard.dto.DashboardResponseDto;
import com.example.demo.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "日次集計（Meal / Training / Record を横断）")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @Operation(summary = "今日のダッシュボード集計", description = "未登録の数値項目は0を返します。体重・体重差分は比較不可の場合nullを返します。", responses = {

      @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "例（すべて登録あり）", value = """
              {
                "totalCalories": 1500,
                "totalProtein": 120,
                "mealCount": 3,
                "totalTrainingCalories": 450,
                "todayWeight": 70.5,
                "weightDiffFromYesterday": -0.3
              }
              """),
          @ExampleObject(name = "例（データなし日）", value = """
              {
                "totalCalories": 0,
                "totalProtein": 0,
                "mealCount": 0,
                "totalTrainingCalories": 0,
                "todayWeight": null,
                "weightDiffFromYesterday": null
              }
              """)
      })),

      @ApiResponse(responseCode = "400", description = "バリデーションエラー", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

      @ApiResponse(responseCode = "500", description = "予期しないエラー", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/today")
  public ResponseEntity<DashboardResponseDto> getTodayDashboard() {
    DashboardResponseDto dto = dashboardService.getTodayDashboard();
    return ResponseEntity.ok(dto);
  }

  @Operation(summary = "指定日のダッシュボード集計", description = "yyyy-MM-dd形式の日付を指定して集計します。未登録の数値項目は0を返します。体重・体重差分は比較不可の場合nullを返します。", responses = {
      @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardResponseDto.class))),

      @ApiResponse(responseCode = "400", description = "バリデーションエラー（date形式不正など）", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

      @ApiResponse(responseCode = "500", description = "予期しないエラー", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/{date}")
  public ResponseEntity<DashboardResponseDto> getDashboardByDate(
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    DashboardResponseDto dto = dashboardService.getDashboardByDate(date);
    return ResponseEntity.ok(dto);
  }
}
