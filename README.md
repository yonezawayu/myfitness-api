# 📊 Dashboard API

## API仕様（Swagger / OpenAPI）

本 API の詳細な仕様およびリクエスト・レスポンス例は

Swagger（OpenAPI）で確認できます。

- URL: http://localhost:8080/swagger-ui.html
- Dashboard API の正常系 / エラー系レスポンス例を掲載

## 概要

Dashboard API は、

**その日の食事・トレーニング・体重データを集計して一覧で返す API** です。

- 食事ログ（MealLog）
- トレーニングログ（TrainingLog）
- 体重ログ（Record）

これら複数ドメインのデータを横断して集計し、

**1 日の状態をダッシュボード用データとして提供**します。

---

## エンドポイント

```
GET /dashboard/today

```

---

## レスポンス仕様

### 正常時（200 OK）

```json
{
  "date": "2025-12-12",
  "totalCalories": 2000,
  "totalProtein": 120,
  "mealCount": 3,
  "todayWeight": 70.0,
  "weightDiffFromYesterday": -0.5,
  "totalTrainingCalories": 500
}
```

### フィールド説明

| フィールド名            | 型                | 説明                                         |
| ----------------------- | ----------------- | -------------------------------------------- |
| date                    | string (ISO-8601) | 対象日                                       |
| totalCalories           | int               | 食事ログの合計カロリー（未登録時は 0）       |
| totalProtein            | int               | 食事ログの合計タンパク質量（未登録時は 0）   |
| mealCount               | long              | 食事ログ件数（未登録時は 0）                 |
| todayWeight             | double / null     | 当日の体重（未登録時は null）                |
| weightDiffFromYesterday | double / null     | 前日との差分（比較不可の場合は null）        |
| totalTrainingCalories   | int               | トレーニング消費カロリー合計（未登録時は 0） |

---

## 異常系レスポンス

本アプリケーションでは、すべてのエラーレスポンスを

`@RestControllerAdvice` により共通形式で返却します。

※ 業務エラー（NOT_FOUND 等）は今後の拡張で追加予定

### 500 Internal Server Error

```json
{
  "message": "Internal Server Error",
  "errors": null
}
```

### 発生条件

- Service 層で想定外例外が発生した場合
- Dashboard データ生成に失敗した場合

---

## 実装ポイント

### Service 層での集計責務

- Controller では **集計ロジックを一切持たない**
  - リクエスト受付とレスポンス返却のみを担当
  - ビジネスロジックや集計処理は持たない
- DashboardService が以下を担当
  - 各 Repository から日付単位でデータ取得
  - null 値の安全なハンドリング
  - 体重差分の計算（当日・前日両方存在する場合のみ）

---

### null / データ無し日の設計方針

| 項目                     | 方針              |
| ------------------------ | ----------------- |
| 数値系（カロリー・件数） | 0 を返却          |
| 体重                     | 未登録時は null   |
| 体重差分                 | 比較不可時は null |

👉 **フロント側での扱いやすさを優先**

---

## テスト方針

### 実装済みテスト

- DashboardServiceTest
  - 正常系
  - データ無し日の集計
  - 体重差分の計算
- DashboardControllerTest
  - 正常レスポンス
  - Service 例外時の 500 エラー

---

## 設計上の意図

- **「一覧画面用 API」専用に最適化**
- 複数ドメインをまたぐ集計処理の練習
- Service / Controller / ExceptionHandler の責務分離を意識
