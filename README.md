MyFitness Tracker API

MyFitness Tracker API は
食事ログ・トレーニングログ・体重ログを管理し、日々の健康データを記録・集計するバックエンド API です。

本プロジェクトは バックエンドエンジニア向けポートフォリオとして、

Spring Boot

Spring Security

JWT認証

REST API設計

DB設計

テスト

Docker環境

などの実装を目的として開発しています。

機能

現在実装済みの主な機能

ユーザー登録

JWTログイン認証

JWTトークン発行

認証ユーザー取得 /users/me

食事ログ管理

トレーニングログ管理

体重ログ管理

ダッシュボード集計 API

Swagger APIドキュメント

Controller / Service テスト

API Documentation

Swagger UI から API を確認できます。

http://localhost:8080/swagger-ui/index.html

Swaggerでは

API仕様

Request / Response例

JWT認証付き実行

を確認できます。

Authentication

本 API は JWT (JSON Web Token) を使用した認証方式を採用しています。

認証フロー

/auth/login でログイン

JWTトークンを取得

Authorization: Bearer <token> を付与してAPIを呼び出す

Login API
POST /auth/login

request

{
"email": "test@example.com",
"password": "password"
}

response

{
"token": "eyJhbGciOiJIUzM4NCJ9..."
}
認証API例
GET /users/me

curl example

curl -H "Authorization: Bearer <token>" \
http://localhost:8080/users/me

response

{
"id": 1,
"email": "test@example.com",
"role": "USER"
}
Dashboard API

Dashboard API は

その日の食事・トレーニング・体重データを集計して返す API です。

複数ドメインのデータを横断して

食事カロリー

タンパク質量

食事回数

体重

トレーニング消費カロリー

などをまとめて返します。

Endpoint
GET /dashboard/today
Response Example
{
"date": "2025-12-12",
"totalCalories": 2000,
"totalProtein": 120,
"mealCount": 3,
"todayWeight": 70.0,
"weightDiffFromYesterday": -0.5,
"totalTrainingCalories": 500
}
フィールド説明
field type description
date string 対象日
totalCalories int 食事ログの合計カロリー
totalProtein int 食事ログの合計タンパク質
mealCount long 食事ログ件数
todayWeight double / null 当日の体重
weightDiffFromYesterday double / null 前日との差分
totalTrainingCalories int トレーニング消費カロリー
Error Response

本アプリケーションでは
@RestControllerAdvice により 共通エラーレスポンス形式 を採用しています。

Example

{
"message": "Internal Server Error",
"errors": null
}
Architecture

本プロジェクトでは

レイヤードアーキテクチャ を採用しています。

controller
↓
service
↓
repository
↓
entity

責務分離

Controller

HTTPリクエスト受付

レスポンス返却

Service

ビジネスロジック

集計処理

Repository

DBアクセス

Tech Stack

Java 17

Spring Boot 3

Spring Security

JWT Authentication

JPA / Hibernate

PostgreSQL

Docker

Swagger (springdoc-openapi)

JUnit5

Mockito

GitHub

Database

PostgreSQL を Docker で起動します。

主要テーブル

users
meal_logs
training_logs
records
Getting Started
1 Docker起動
open -a Docker
2 Database起動
docker compose up -d
3 Spring Boot起動
./mvnw spring-boot:run
4 API確認

Swagger UI

http://localhost:8080/swagger-ui/index.html
Testing

テストには

JUnit5

Mockito

を使用しています。

テスト実行

./mvnw test

現在のテスト

DashboardControllerTest
DashboardServiceTest
Project Structure
com.myfitness.api
│
├ auth
│
├ common
│
├ user
│
├ meal
│
├ training
│
├ record
│
└ dashboard
Development Status

現在の開発状況

ユーザー認証 ✓
JWT認証 ✓
Swagger ✓
食事ログ ✓
トレーニングログ ✓
体重ログ ✓
ダッシュボード ✓

今後の予定

Foodマスタ
Exerciseマスタ
カロリー計算ロジック
栄養計算
統計API
License

MIT License
