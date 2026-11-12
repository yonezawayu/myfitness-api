# M5-14 Production CSP・認証・記録E2Eの隔離検証

検証日：2026-11-11。対象：PR #55マージ `1800a0b9f6d3bb6fad857042a1127886f1ef3985`。

**結論：Productionの自動テスト・HTTP/モック契約検証は成功。2026-11-12にユーザーから、Production＋HTTPSモックで主要操作の手動ブラウザE2E成功が報告された。** Codexのブラウザ自動操作は内部エラーで未実施。HTTP成功だけから画面操作成功を推定したものではない。
アプリケーションコード・既存env・DBは変更せず、最新developをGit archiveした隔離コピーを使用した。

### ユーザーによる手動E2E確認（2026-11-12追記）

ユーザーがChromeで実施し、M5-15指示に記載した確認結果を記録する。Codexが独自に画面操作・Consoleを再観測した証跡ではない。

| 項目 | ユーザー報告 |
| --- | --- |
| ログイン・認証状態維持・ログアウト | 成功 |
| 種目選択・セット追加/削除/コピー | 成功 |
| タイマー | 成功 |
| TrainingSession保存・履歴・編集/再保存・削除 | 成功 |
| CSP/JavaScriptエラー | なし |
| 表示崩れ | なし |

対象は隔離HTTPSモックAPIであり、実Spring Boot・実PostgreSQL・Railway/Neonの動作保証には含めない。
登録画面・404・旧URL・強制401などの個別ブラウザ結果は今回の報告に明記されていないため、未報告として区別する。
M5-15では再利用・追加確認用に隔離プロセスを保持し、停止していない。

## 1. 環境と分離

```text
Chrome（主要操作はユーザー手動確認済み、詳細項目は別記）
  ├─ HTTPS localtest.me:34414（127.0.0.1のみ待受）
  │    └─ 固定upstream HTTP 127.0.0.1:33114 / Next.js Production
  └─ HTTPS localtest.me:34415（127.0.0.1のみ待受）
       └─ メモリ内のAPI契約モック。外部API/DB接続コードなし

既存3000 / 8080 / PostgreSQLには接続しない
```

- 作業先：`/private/tmp/myfitness-m514.WC9cvd`。親700、資格情報・TLS秘密鍵600。
- Next.js 16.4.5 / React18.3.1、Node22。最新マージコミットを展開し、実envをコピーしていない。
- Frontend build/start：`NEXT_PUBLIC_API_BASE_URL=https://localtest.me:34415`、テレメトリー無効。
- APIとFrontendのOriginは別ポートで分離。モックCORSはFrontend Originだけを許可。実BackendのCORSは変更していない。
- `localtest.me` は外部DNS名だが、確認時は127.0.0.1/::1のみへ解決した。Web接続はループバックのみ。DNSが異なる結果・解決不能になったら操作を停止する。
- TLSは隔離専用の自己署名証明書。OSやブラウザのグローバルな証明書ストアを変更せず、TLS/CSP検証無効化フラグも使用しない。
- 証明書は2026-11-14 12:57:21 UTCまで。期限後は使わず、隔離証明書の再作成が必要。

## 2. 手動ブラウザを開く手順

1. 既存localhost:3000のタブや入力画面はそのまま残す。可能なら拡張機能を無効化した専用ブラウザプロファイルを使う。
2. まず `https://localtest.me:34415/health` を開く。自己署名の警告が出るため、証明書の対象host・期限・下記SHA-256と、ローカル待受を確認する。ユーザー自身がこの隔離環境だけの例外を受け入れる場合に限り続行する。公開サイトの証明書警告を無視する手順ではない。
3. `https://localtest.me:34414/` を開き、同じ証明書を確認する。正常ログインの操作には実際のパスワード・JWTを使わない。
4. テスト専用メール・ランダムパスワードは `test-account.credentials` をローカルのエディタで閲覧して使用する。値をチャット、Console、ログ、スクリーンショット、Gitへ転記しない。
5. API healthが表示できない、証明書が一致しない、DNSが外部IPへ解決される場合は停止する。`--ignore-certificate-errors` や全体的なセキュリティ無効化で回避しない。

証明書SHA-256：

```text
7D:A8:55:F0:03:CA:D4:5B:BB:51:9F:1E:06:07:FF:92:B4:5F:6E:9A:BC:4E:80:CB:FE:75:65:9A:20:04:FC:49
```

Codexのブラウザ連携は `Cannot read properties of undefined (reading 'timeoutMs')` で失敗した。繰り返し試行や別の自動操作基盤への迂回は行っていない。Codex自身は証明書の例外操作・画面操作を確認できていないが、ユーザーによる主要操作の成功報告は冒頭に記録した。

## 3. API契約とモックの範囲

参照：`AuthController`、`UserController`、`TrainingSessionController`、各DTO、`TrainingSessionRequestHasher`、`SecurityConfig`、`TrainingSessionControllerSecurityIntegrationTest`、`TrainingSessionIdempotencyIntegrationTest`、`TrainingSessionCorsIntegrationTest`。

| API/機能 | モックの実装・自動確認 |
| --- | --- |
| POST /auth/login | email/password、200 accessToken/tokenType/expiresInSeconds、失敗401 |
| POST /users、GET /users/me | 登録201、重複409、基本入力検証、本人情報 |
| JWT/Bearer | モック専用ランダム鍵のHMAC JWT、署名・exp・issuer確認。実JWT/鍵は不使用 |
| GET /training-sessions と /{id} | ユーザー別一覧・詳細、他人のIDは404 |
| POST /training-sessions/complete | 入れ子DTO、UUID、201/再送200/内容違い409、ユーザー単位のキー |
| 編集 | PUT Session、Exercise、Set。個別追加POST・削除DELETEとGETも対応 |
| 削除 | 204、子データを含めメモリ内削除。使用済みキーの再送409 |
| 401 | token不正/欠落/期限切れ。管理画面からモックtokenだけを強制失効可能 |
| CORS | 明示Origin、GET/POST/PUT/DELETE/OPTIONS、Authorization/Content-Type/Idempotency-Key、credentials=true、Retry-After公開 |
| 種目選択 | 現アプリはFrontendの静的カタログを利用。架空の種目APIは追加していない |

モックはメモリ内であり、再起動すると記録・使用済みキー・署名鍵は消える。実PostgreSQLの原子性、advisory lock、永続化、障害時ROLLBACK、Flyway、最小権限、Spring Securityのfilter全体はこのモックでは検証できない。
パスの正規化・Java Bean Validationの全境界・BigDecimal/Unicodeのcanonical hash・並べ替えの全仕様・本番エラー本文・レート制限も完全再現ではない。通常のUIが利用するデータ形・主なHTTP status・所有権・再送の契約を対象とする。
モック上の並列POST成功を、実Backendの並列安全性を再検証した結果とは扱わない。既存BackendのH2/独立PostgreSQL検証結果とは分ける。

## 4. 今回実施した検証

| 項目 | 結果 |
| --- | --- |
| npm ci | 成功 |
| npm test | 49件成功、失敗・skipなし |
| lint / tsc --noEmit --incremental false | 成功 |
| npm run build / npm run start | 成功、全ページ動的SSR |
| npm audit --omit=dev | 全重大度0件 |
| 主要6画面のGET | 200。ログイン/登録/履歴/選択/新規/編集 |
| 404 / 旧URL | 404 / 307、履歴へ転送 |
| CSP | unsafe-inline/evalなし、connect-srcはselfとHTTPSモックOriginのみ |
| nonce | 各scriptとCSP一致、偽装値不採用、同一URLの再要求でも更新 |
| キャッシュ | private, no-store |
| HTTP security headers | nosniff、X-Frame-Options、Referrer/Permissions-Policyを確認 |
| JS/CSS | HTMLが参照する同一サイトのJS/CSSをGETし200。実行・適用は未確認 |
| モック認証/記録 | 正常/失敗ログイン、Bearer、me、登録、401、CRUD、再読込相当GET、201/200/409、他人404、CORS拒否を自動確認 |
| 実ブラウザ | Codex自動操作は未実施。主要操作は上記ユーザー手動報告で成功 |

HTTP検証は自己署名証明書を明示CAとして指定し、接続先127.0.0.1・TLS servername localtest.meで証明書を検証した。システム全体のTLS検証は無効化していない。
今回HSTS用の本番公開Originは指定していない。実際の公開ホストでのHSTS・公開CA証明書・HTTPS redirectは未検証。

証跡（隔離ディレクトリ内）：`m514-npm-ci.log`、`m514-test.log`、`m514-lint.log`、`m514-types.log`、`m514-build.log`、`m514-audit.json`、`m514-http-contract.log`。
再現スクリプト：`mock-api.mjs`、`verify.mjs`、`tls.cnf`。スクリプト/資格情報はGit管理外。資格情報・鍵の内容を表示しない。

## 5. 詳細手動E2Eチェックリスト（上記報告より細かい観点の確認用）

以下の詳細項目をすべてユーザーが個別確認したとは扱わない。主要操作の完了状態は冒頭の報告表、個別未報告の観点はこの補完チェックリストで管理する。

- [ ] DevTools Console/Networkを開きclear。HARはAuthorizationやパスワードを含むため共有しない。
- [ ] ログイン・登録画面のCSS/フォント/入力/表示切替/相互リンク。未認証の履歴URLはログインへ戻る。
- [ ] 専用資格情報でログインし履歴へ遷移。再読込後も認証が維持される。Authorizationの値を報告せず、Bearer送信の有無のみ記録する。
- [ ] 2種目選択→新規画面。重量/回数/メモ入力、セット追加・削除・前セットコピーを確認する。
- [ ] タイマーStart/Pause/Resume/Reset。必須項目の入力エラーを確認する。
- [ ] 保存中表示・二重クリック抑止・保存後履歴。NetworkではURL/回数/statusだけを記録し、token・password・payloadを転記しない。
- [ ] 履歴の重量/回数/メモ。編集で種目/セット追加・削除、値変更、再保存、再読込後保持。
- [ ] HTML風のメモ文字列が実行されずテキスト表示される。テストSessionを削除し履歴から消える。
- [ ] 通常ログアウトでログインへ戻り、保護画面へ移動しても表示できない。
- [ ] 再ログイン後、APIの `https://localtest.me:34415/__control` で「Expire all mock tokens」を押す。Frontendの履歴を再読込して401・認証解除・ログイン遷移を確認する。
- [ ] 404・旧URL転送。すべての画面/クライアント遷移/ハード再読込で表示崩れ・hydration/JS error・CSP violationなし。
- [ ] Consoleの `Content Security Policy` / `Refused to` / `blocked` を確認。違反があればdirective、resource、操作のみ記録し、不合格とする。HTTP200だけで合格にしない。

## 6. 起動管理

現在の隔離起動は次の方式。すでに起動している場合は再実行しない。既存3000/8080番には触れない。

```sh
cd /private/tmp/myfitness-m514.WC9cvd/frontend
NEXT_PUBLIC_API_BASE_URL=https://localtest.me:34415 NEXT_TELEMETRY_DISABLED=1 npm run start -- --hostname 127.0.0.1 --port 33114
# 別の隔離ターミナル
cd /private/tmp/myfitness-m514.WC9cvd
node mock-api.mjs
```

モックの公開CORS契約補正時に、今回作成した隔離モックのみ正常停止・再起動した。既存Frontend/Backend、前回M5-13の33013番は停止・再起動していない。
検証HTTPで作成したSessionはモックAPI経由で削除済み。モック内のテスト登録ユーザーと使用済みキーだけがメモリ内に残る。実DBへの接続・件数照会も実施していない。

## 7. 公開前ゲートと将来改善

| 区分 | 対応 |
| --- | --- |
| 公開前必須 | 本チェックリストの実ブラウザ操作。CSP違反・描画・JS実行を確認 |
| 公開前必須 | 公開CAのHTTPS、HSTS/redirect、正確なCORS、本番OriginでのE2E |
| 公開前必須 | Railway ingressの信頼境界・IP判定。全ユーザー同一枠や偽装回避を許さない |
| 公開前必須 | managed PostgreSQLのowner/migrator/app分離、空DB初期構築、バックアップ/復旧検証、Secrets管理 |
| 公開前必須 | readinessがDB疎通を保証しないことを前提にDB監視/通知・障害時運用を設計 |
| 公開前必須 | localStorage JWT/XSS残存リスクの受容判断、動的SSR/no-storeの費用・負荷確認 |
| 将来改善 | HttpOnly Cookie/refresh/revoke・CSRFの包括設計（別承認が必要） |
| スケール前必須 | 単一インスタンスのレート制限を共有化。複数台では枠が共有されない |
| 継続改善 | CSPの信頼script連鎖、依存更新、DOM変更のレビュー・監視・ブラウザ回帰 |

nonce方式でもDOM XSSや信頼scriptの悪用を完全に防げず、XSS成立時にはlocalStorage JWT窃取・本人操作のリスクが残る。動的SSRは共有HTML cache/ISRを前提にできず、コスト・負荷が増える可能性がある。
本番サービス作成、デプロイ、DB接続/変更、認証方式変更、コミット/Push/PRは今回未承認・未実施。
