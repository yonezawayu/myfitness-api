# MyFitness API

MyFitness API は、食事ログ、食品マスタ、トレーニングログ、体重記録を管理し、日々の健康データを集計する Spring Boot 製のバックエンド API です。

ポートフォリオ用に、JWT 認証付きの REST API と、最低限の Next.js フロントエンドを同一リポジトリにまとめています。

## 特徴

- 食事・トレーニング・体重を横断して1日単位で集計
- Food と MealItem から栄養値をサーバ側で自動計算
- JWT 認証を Spring Security の Filter として実装
- Swagger UI から認証付きで API を検証可能
- Next.js フロントエンドからログインと Dashboard 表示が可能

## 技術スタック

### Backend

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

### Frontend

- Next.js App Router
- TypeScript
- React
- Tailwind CSS
- fetch API

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
- フロントエンドのログイン画面
- フロントエンドの Dashboard 画面

## API 一覧

### Auth / User

| Method | Endpoint      | 説明               |
| ------ | ------------- | ------------------ |
| POST   | `/users`      | ユーザー登録       |
| GET    | `/users/me`   | 認証ユーザー取得   |
| POST   | `/auth/login` | ログイン、JWT 発行 |

### Food

| Method | Endpoint          | 説明         |
| ------ | ----------------- | ------------ |
| POST   | `/foods`          | 食品登録     |
| GET    | `/foods`          | 食品一覧取得 |
| GET    | `/foods/{foodId}` | 食品詳細取得 |

### MealLog / MealItem

| Method | Endpoint                           | 説明                           |
| ------ | ---------------------------------- | ------------------------------ |
| POST   | `/meal-logs`                       | 食事ログ登録                   |
| GET    | `/meal-logs`                       | 食事ログ一覧取得               |
| GET    | `/meal-logs/{id}`                  | 食事ログ詳細取得               |
| PUT    | `/meal-logs/{id}`                  | 食事ログ更新                   |
| DELETE | `/meal-logs/{id}`                  | 食事ログ削除                   |
| POST   | `/meal-items`                      | 食事ログに食品明細を追加       |
| GET    | `/meal-items/meal-log/{mealLogId}` | 食事ログに紐づく食品明細を取得 |

### TrainingLog

| Method | Endpoint              | 説明                     |
| ------ | --------------------- | ------------------------ |
| POST   | `/training-logs`      | トレーニングログ登録     |
| GET    | `/training-logs`      | トレーニングログ一覧取得 |
| GET    | `/training-logs/{id}` | トレーニングログ詳細取得 |
| PUT    | `/training-logs/{id}` | トレーニングログ更新     |
| DELETE | `/training-logs/{id}` | トレーニングログ削除     |

### Record

| Method | Endpoint        | 説明             |
| ------ | --------------- | ---------------- |
| POST   | `/records`      | 体重記録登録     |
| GET    | `/records`      | 体重記録一覧取得 |
| GET    | `/records/{id}` | 体重記録詳細取得 |
| PUT    | `/records/{id}` | 体重記録更新     |
| DELETE | `/records/{id}` | 体重記録削除     |

### Dashboard

| Method | Endpoint           | 説明                                 |
| ------ | ------------------ | ------------------------------------ |
| GET    | `/dashboard/today` | 今日の食事、体重、トレーニングを集計 |

## 起動手順

### Backend

Docker を起動します。

```bash
open -a Docker
```

PostgreSQL を起動します。

```bash
docker compose up -d
```

Spring Boot アプリケーションを起動します。

```bash
./mvnw spring-boot:run
```

バックエンドのテストを実行します。

```bash
./mvnw test
```

### Frontend

フロントエンドの依存関係をインストールします。

```bash
cd frontend
npm install
```

Next.js を起動します。

```bash
npm run dev
```

ブラウザで以下にアクセスします。

```text
http://localhost:3000
```

フロントエンドは `http://localhost:8080` の API に接続します。

## Swagger 確認手順

アプリケーション起動後、以下にアクセスします。

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger UI から、JWT 認証付きで API を検証できます。

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

### 3. Bearer 認証を設定

Swagger UI 右上の `Authorize` を押し、取得した JWT を次の形式で入力します。

```text
Bearer eyJhbGciOiJIUzM4NCJ9...
```

curl 例:

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

Dashboard では、食事・トレーニング・体重を横断して、
「摂取 / 消費 / 体重」の関係を1画面で確認できるようにしています。

## フロントエンド画面

### ログイン画面

- email / password を入力
- `POST /auth/login` を実行
- 取得した JWT を `localStorage` に保存
- 成功後 `/dashboard` に遷移

### Dashboard 画面

- `GET /dashboard/today` を実行
- `totalCalories`
- `totalProtein`
- `todayWeight`
- `totalTrainingCalories`

## 工夫ポイント

- 機能単位で `auth`、`user`、`food`、`meal`、`training`、`record`、`dashboard` にパッケージを分離
- Controller / Service / Repository / Entity / DTO の責務を分離
- JWT 認証を Spring Security の Filter として実装
- Food の栄養値と MealItem の量からサーバ側で栄養計算
- Dashboard で複数ドメインを横断して日次集計
- Service / Controller のテストを用意し、CI で確認可能
- フロントエンドは状態管理ライブラリなしで、fetch と localStorage のみで実装

## 現在の制約

- TrainingLog は α版用の簡易トレーニング記録です。
- Training / TrainingSet の親子構造は未実装です。
- 消費カロリーは現時点では入力値を使用しています。
- Exercise マスタは未実装です。
- Dashboard は日次集計のみ実装しています。
- フロントエンドはログインと Dashboard 表示のみ実装しています。

## 今後の改善点

- Exercise CRUD を追加する
- Training / TrainingSet の親子構造を追加する
- MET 値や体重を用いた消費カロリー自動計算を追加する
- Dashboard の週次 / 月次集計を追加する
- Record に体脂肪率・筋肉量を追加する
- API レスポンス形式を統一する
- フロントエンドに Food / Meal / Training / Record の入力画面を追加する

## プロジェクト構成

```text
.
├── frontend
│   ├── app
│   ├── lib
│   └── package.json
└── src/main/java/com/myfitness/api
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

`practice` は JWT や CRUD の検証用の練習パッケージです。
