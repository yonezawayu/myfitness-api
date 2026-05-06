# MyFitness API

MyFitness API は、食事ログ、食品マスタ、トレーニングログ、体重記録を管理し、日々の健康データを集計する Spring Boot 製のバックエンド API です。

このリポジトリは、JWT 認証、REST API 設計、JPA による永続化、Swagger による API 確認、JUnit によるテストを含むポートフォリオ用の API として開発しています。

## 技術スタック

- Java 17
- Spring Boot 3
- Spring Security
- JWT Authentication
- Spring Data JPA / Hibernate
- PostgreSQL
- Docker Compose
- springdoc-openapi / Swagger UI
- JUnit 5
- Mockito

## 主な機能

- ユーザー登録
- JWT ログイン認証
- Swagger Bearer 認証
- 認証ユーザー取得
- Food CRUD
- MealLog CRUD
- MealItem 作成と栄養値自動計算
- MealLog 集計
- TrainingLog CRUD
- Record CRUD
- Dashboard 日次集計

## API 一覧

### Auth / User

| Method | Endpoint | 説明 |
| --- | --- | --- |
| POST | `/users` | ユーザー登録 |
| GET | `/users/me` | 認証ユーザー取得 |
| POST | `/auth/login` | ログイン、JWT 発行 |

### Food

| Method | Endpoint | 説明 |
| --- | --- | --- |
| POST | `/foods` | 食品登録 |
| GET | `/foods` | 食品一覧取得 |
| GET | `/foods/{foodId}` | 食品詳細取得 |

### MealLog / MealItem

| Method | Endpoint | 説明 |
| --- | --- | --- |
| POST | `/meal-logs` | 食事ログ登録 |
| GET | `/meal-logs` | 食事ログ一覧取得 |
| GET | `/meal-logs/{id}` | 食事ログ詳細取得 |
| PUT | `/meal-logs/{id}` | 食事ログ更新 |
| DELETE | `/meal-logs/{id}` | 食事ログ削除 |
| POST | `/meal-items` | 食事ログに食品明細を追加 |
| GET | `/meal-items/meal-log/{mealLogId}` | 食事ログに紐づく食品明細を取得 |

### TrainingLog

| Method | Endpoint | 説明 |
| --- | --- | --- |
| POST | `/training-logs` | トレーニングログ登録 |
| GET | `/training-logs` | トレーニングログ一覧取得 |
| GET | `/training-logs/{id}` | トレーニングログ詳細取得 |
| PUT | `/training-logs/{id}` | トレーニングログ更新 |
| DELETE | `/training-logs/{id}` | トレーニングログ削除 |

### Record

| Method | Endpoint | 説明 |
| --- | --- | --- |
| POST | `/records` | 体重記録登録 |
| GET | `/records` | 体重記録一覧取得 |
| GET | `/records/{id}` | 体重記録詳細取得 |
| PUT | `/records/{id}` | 体重記録更新 |
| DELETE | `/records/{id}` | 体重記録削除 |

### Dashboard

| Method | Endpoint | 説明 |
| --- | --- | --- |
| GET | `/dashboard/today` | 今日の食事、体重、トレーニングを集計 |

## 起動手順

### 1. Docker を起動

```bash
open -a Docker
```

### 2. PostgreSQL を起動

```bash
docker compose up -d
```

### 3. アプリケーションを起動

```bash
./mvnw spring-boot:run
```

### 4. テストを実行

```bash
./mvnw test
```

## Swagger 確認手順

アプリケーション起動後、以下にアクセスします。

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger UI では、API 仕様、Request / Response、JWT Bearer 認証付きの動作確認ができます。

## JWT 認証の使い方

### 1. ユーザー登録

```http
POST /users
```

### 2. ログイン

```http
POST /auth/login
```

リクエスト例:

```json
{
  "email": "test@example.com",
  "password": "password"
}
```

レスポンス例:

```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600
}
```

### 3. Swagger で Bearer 認証を設定

Swagger UI 右上の `Authorize` を押し、取得した JWT を次の形式で入力します。

```text
Bearer eyJhbGciOiJIUzM4NCJ9...
```

以後、認証が必要な API を Swagger から実行できます。

curl で実行する場合:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/users/me
```

## Dashboard レスポンス例

```http
GET /dashboard/today
```

```json
{
  "date": "2026-05-06",
  "totalCalories": 2000,
  "totalProtein": 120,
  "mealCount": 3,
  "todayWeight": 70.0,
  "weightDiffFromYesterday": -0.5,
  "totalTrainingCalories": 500
}
```

Dashboard では、当日の食事ログ、栄養値、体重記録、トレーニング消費カロリーを横断して集計します。

## 工夫ポイント

- 機能単位で `auth`、`user`、`food`、`meal`、`training`、`record`、`dashboard` にパッケージを分離
- Controller / Service / Repository / Entity / DTO の責務を分離
- JWT 認証を Spring Security の Filter として実装
- Swagger UI から Bearer 認証付き API を確認可能
- Food の栄養値と MealItem の量からカロリー、タンパク質、脂質、炭水化物をサーバ側で計算
- Dashboard で複数ドメインのデータを横断集計
- Service / Controller のテストを用意し、CI で確認可能

## 現在の制約

- TrainingLog は α版用の簡易トレーニング記録です。
- 現時点の TrainingLog は、Training / TrainingSet の親子構造ではなく、単一ログとして扱っています。
- 消費カロリーは現時点では入力値を使用しています。
- Exercise マスタは未実装です。
- Dashboard は日次集計のみ実装しています。
- フロントエンドは未実装です。

## 今後の改善点

- Exercise CRUD を追加する
- Training / TrainingSet の親子構造を追加する
- Exercise マスタ、TrainingSet、体重、時間、MET 値などを使って消費カロリーを自動計算する
- Dashboard の週次、月次集計を追加する
- Record に体脂肪率、筋肉量などの項目を追加する
- Food の更新、削除 API を追加する
- API レスポンス形式を共通化する
- フロントエンドを追加する

## プロジェクト構成

```text
com.myfitness.api
├── auth
├── common
├── dashboard
├── food
├── meal
├── practice
├── record
├── training
└── user
```

`practice` は JWT や CRUD の確認用に残している練習用パッケージです。α版の主要機能は `food`、`meal`、`training`、`record`、`dashboard` を中心に構成しています。
