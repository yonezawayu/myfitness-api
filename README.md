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

## MVP公開時の旧API機能フラグ

MVPではTrainingSessionを中心とし、所有権対応が完了していない旧APIはデフォルトで無効です。設定が存在しない場合もControllerは登録されません。

| 設定 | 対象API | デフォルト |
| --- | --- | --- |
| `myfitness.features.records.enabled` | `/records/**` | `false` |
| `myfitness.features.legacy-training.enabled` | `/training-logs/**` | `false` |
| `myfitness.features.meal-logs.enabled` | `/meal-logs/**` | `false` |
| `myfitness.features.meal-items.enabled` | `/meal-items/**` | `false` |
| `myfitness.features.dashboard.enabled` | `/dashboard/**` | `false` |
| `myfitness.features.practice-memos.enabled` | `/practice/memos/**` | `false` |
| `myfitness.features.foods.enabled` | `/foods/**` | `false` |

旧APIを再有効化する前に、対象データのユーザー所有権、認可、他ユーザーアクセス拒否、およびHTTP統合テストを実装してください。公開環境で所有権未対応のままフラグを有効にしてはいけません。MealLog、MealItem、Foods、Dashboardは相互依存するため、復旧時は依存関係も合わせて検証します。

Controllerの無効化はEntity、Repository、Service、DBテーブルを削除しないため、既存データは保持されます。

## 起動手順

### Backend

PostgreSQLの初期化管理者、Spring Bootのアプリ実行ロール、手動Flywayのマイグレーションロールは別の資格情報として管理します。固定値のフォールバックはありません。

| 環境変数 | 用途 |
| --- | --- |
| `POSTGRES_ADMIN_USER` | 空のデータディレクトリを初期化するPostgreSQL管理者ロール |
| `POSTGRES_ADMIN_PASSWORD` | PostgreSQL初期化管理者のパスワード |
| `MYFITNESS_DB_URL` | PostgreSQL JDBC URL |
| `MYFITNESS_DB_USERNAME` | Spring Bootが通常のCRUDに使用するアプリ実行ロール |
| `MYFITNESS_DB_PASSWORD` | アプリ実行ロールのパスワード |
| `MYFITNESS_JWT_SECRET` | JWT署名秘密鍵（32文字以上の十分にランダムな値） |
| `FLYWAY_URL` | 手動Flyway実行用JDBC URL |
| `FLYWAY_USER` | Flyway専用マイグレーションロール |
| `FLYWAY_PASSWORD` | Flyway専用ロールのパスワード |

`.env.backend.example`を参照し、ローカルではGit管理外の`.env.backend.local`に実値を設定します。管理者・アプリ・Flywayに同じロールやパスワードを設定しないでください。ファイルは所有者だけが読めるようにし、絶対にコミットしないでください。

```bash
chmod 600 .env.backend.local
```

#### 既存DBと新規DBの違い

`POSTGRES_USER`と`POSTGRES_PASSWORD`は、PostgreSQLイメージが**空のデータディレクトリを初期化するときだけ**使用します。既存のDockerボリュームに対して環境変数を変更しても、DBロール、パスワード、所有者、権限は変更されません。

- 既存の`demo_postgres-data`では、ロール移行を別手順で完了するまで、コンテナを再作成しないでください。
- Compose設定の変更だけでは、現在SUPERUSERである既存`myfitness_user`は最小権限化されません。
- 新規の空DBでは、まず`POSTGRES_ADMIN_USER`の管理者ロールと`myfitness` DBだけが初期化されます。
- 新規DBのアプリロールとFlywayロールは自動作成されません。検証済みのロール作成・権限付与SQLを管理者が別途実行してからBackendを起動してください。
- Spring Bootには`MYFITNESS_DB_*`だけを渡し、管理者用資格情報をアプリ設定へ流用しないでください。

##### 既存DBのロール移行前

現在の`.env.backend.local`には管理者用変数がないため、変更後のComposeファイルを使う`docker compose config`や`docker compose up`は、Dockerを操作する前に必須変数エラーで停止します。これは管理者とアプリの資格情報が再び共用されることを防ぐ、意図したfail-closed動作です。

- エラー回避のため、既存`myfitness_user`を`POSTGRES_ADMIN_USER`へ設定してはいけません。それではロール分離にならず、SUPERUSERのアプリ利用を継続する危険な暫定対応になります。
- 既存コンテナを直接再開しただけでは、新しいCompose環境変数は反映されず、DBロールも変わりません。
- ロール移行が完了するまで、変更後のCompose設定による`up`やコンテナ再作成を実施せず、現在のコンテナと`demo_postgres-data`を維持してください。

##### 既存DBのロール移行後

既存DBを安全に移行する順序は次のとおりです。

1. 最新バックアップと独立リストア結果を確認する。
2. Backendからの書き込みを停止する。
3. 現在の管理権限は移行作業だけに使用し、別名の管理者、NOLOGIN所有者、Flywayロールを作成する。既存`myfitness_user`はアプリロールとして維持する。
4. 所有権と権限を移行し、既存`myfitness_user`へ必要なDML権限だけを付与して、SUPERUSER等の不要な属性を外す。
5. 新しい管理者ロールが有効で、アプリロールが管理者・所有者ロールを継承しないことを確認する。
6. アプリロールでHibernate validateとCRUD、DDL拒否を確認する。
7. `.env.backend.local`の`POSTGRES_ADMIN_*`には新しい管理者を、`MYFITNESS_DB_*`には最小権限化したアプリロールを設定する。
8. すべての検証が成功した後に限り、既存ボリュームを維持したまま必要なコンテナ再作成を行う。

`POSTGRES_ADMIN_USER`と`MYFITNESS_DB_USERNAME`は必ず異なるロールにしてください。既存ボリュームを使う再作成では、Compose環境変数は既存ロールを作成・変更しないため、SQLによるロール移行を先に完了する必要があります。

##### 新規DBの構築順序

1. 管理者・Flyway・アプリで異なるロール名と資格情報を準備する。
2. `POSTGRES_ADMIN_*`を使って空のPostgreSQLデータディレクトリを初期化する。
3. 管理者でNOLOGIN所有者、Flyway、アプリの各ロールを作成し、必要な権限を付与する。
4. Flyway専用ロールでマイグレーションを適用する。
5. アプリロールでHibernate validateが成功することを確認してからBackendを公開する。

ロール作成・権限付与SQLはComposeから自動実行されません。新規DBの起動直後にBackendを接続してはいけません。

ロール移行前に`docker compose up --force-recreate`、`docker compose down -v`、ボリューム削除を実行してはいけません。

以下のCompose起動手順は、新規構築時または既存DBのロール移行と`.env.backend.local`更新が完了した後だけ実行してください。未移行の既存環境では実行しないでください。

Docker を起動します。

```bash
open -a Docker
```

PostgreSQL を起動します。

```bash
docker compose --env-file .env.backend.local up -d
```

必要な管理者変数が未設定の場合、Composeは起動前の設定解決で`POSTGRES_ADMIN_USER is required`または`POSTGRES_ADMIN_PASSWORD is required`として停止します。

環境変数を現在のシェルへ読み込み、Spring Bootアプリケーションを起動します。

```bash
(
  set -a
  source ./.env.backend.local
  set +a
  ./mvnw spring-boot:run
)
```

認証情報をコマンドラインへ直接記述したり、`.env.backend.local`の内容をログへ出力したりしないでください。

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
  "weightDiffFromYesterday": -0.5
}
```

Dashboard では、食事・体重を横断して、
「摂取 / 体重」の関係を1画面で確認できるようにしています。

## フロントエンド画面

### ログイン画面

- email / password を入力
- `POST /auth/login` を実行
- 取得した JWT を `localStorage` に保存
- 成功後 `/training-sessions` に遷移

### Dashboard 画面

- `GET /dashboard/today` を実行
- `totalCalories`
- `totalProtein`
- `todayWeight`

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
