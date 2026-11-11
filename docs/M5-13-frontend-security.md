# M5-13 Frontend XSS・CSP・JWT保存方式

調査：2026-11-11。基点：PR #54 / `e866dd37b37409d3cda95efc2d2d456f963d337c`。
実装・検証はGit外の隔離コピーで実施。ユーザーがFrontend自動再起動リスクを承認後、検証済み13ファイルを実リポジトリへ反映した。手動停止・再起動は行わない。
反映後の読み取り確認：localhost:3000のログイン画面はHTTP 200、localhost:8080の未認証保護APIは401。待受PIDはFrontend 1115・Backend 75372を維持。これは開発サーバーの疎通確認であり、本番CSPのブラウザE2E合格ではない。
実DB、Backend、認証API、既存env、依存関係のバージョンは変更しない。

## 1. 調査結果：事実と残るリスク

- JWTはBackendがHMAC署名し、issuer・署名・exp・subjectを検証する。subjectはメール。既定有効期限60分。
- ログイン応答はaccessToken/tokenType/expiresInSeconds。FrontendはlocalStorageの `myfitness_access_token` へ保存し、APIへBearer送信する。
- Cookie認証・refresh APIは未実装。再読込後も保存tokenを使い、認証失敗時に削除してログインへ戻る。ログアウトは端末側削除であり、発行済みJWTのサーバー側失効ではない。
- 筋トレ記録のメモ・種目名・APIエラーはReactの通常のテキストノードで表示。dangerouslySetInnerHTML、HTML/Markdown renderer、innerHTML代入、eval、新規外部scriptは見つからない。
- API URLは既存のAPI_BASE_URL検証を経由。画面遷移先は固定の内部path、種目queryはURLSearchParams。入力HTMLをURLとして実行する仕組みは確認されない。
- token/passwordのconsole出力は確認されない。ログイン/履歴APIのテストでもconsole非出力を確認。
- localStorageは同一originのJavaScriptから読める。将来XSSが成立すればtoken窃取や本人権限の操作が可能。CSPだけでこのリスクをゼロとはしない。

今回、通常のReact文字列を二重escapeする変更、不要なsanitize依存、認証APIの書換えは行わない。
[OWASP localStorageの注意](https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html)

## 2. JWT保存方式の比較

| 比較 | A：localStorage＋Bearer | B：メモリ＋再ログイン | C：HttpOnly/Secure/SameSite Cookie |
| --- | --- | --- | --- |
| XSS窃取 | 読み取り可能。CSP・安全なrenderで発生機会を減らすが窃取耐性は残らない | 永続diskには残さない。ただし実行中XSSのtoken利用/操作は防げない | JSからcookie値を読めない。ただしXSSによる本人操作は残る |
| CSRF | tokenを明示headerへ入れる現方式ではcookie自動送信型CSRFと性質が異なる | 同左 | cookie自動送信のためCSRF token/origin検証等の設計が必要 |
| 持続性・reload | 現在どおりreload/タブ間で持続。期限切れはAPIで拒否 | reload/新タブで失われ再ログイン。ユーザー体験が変わる | cookie期限・session/永続cookie・refresh方式次第 |
| logout | localStorage削除。盗難tokenは期限まで利用可能 | メモリ削除。サーバー失効は別途必要 | サーバーSet-Cookieで失効、path/domain/refresh/revokeも設計 |
| API影響 | 今回は変更なし | Bearer API維持、全画面の状態管理変更 | Backendがcookie発行/検証/削除を担い、現認証filter/CSRF設定を変更 |
| CORS | 現行Authorization/明示Originを維持 | 原則現行どおり | cross-origin credentials・SameSite/site/domain・HTTPSを再設計 |
| 実装工数 | 小～中。主にCSP/render/テスト | 中。persistの扱い・再ログイン・タブ連携テスト | 大。Backend/Frontend/CSRF/refresh/運用をまとめて変更 |
| テスト影響 | 既存の認証・記録保存を維持してCSPを追加 | reload/タブ/入力保持の広い回帰が必要 | 全認証API、CSRF、logout、domain/HTTPS、ブラウザ互換の回帰が必要 |
| 運用 | 現行と互換。ただしXSS時の残存リスクを明示 | 簡単なtoken保持でも再ログイン負担が増える | 同一site設計なら将来候補。鍵管理・cookie/CSRF障害対応が増える |

**今回の推奨・実装範囲はA**：認証持続性を変えずにCSPを強化する。Aを無条件に最も安全な方式とはしない。
B/Cへの移行は今回未実装・未承認。公開形態・同一site構成・refresh/失効/CSRF・入力保護を設計し、別承認で判断する。
特に現BackendのCSRF無効設定のままcookieだけを受け入れる変更は行ってはいけない。

## 3. CSPの実装

Next.js 16.4.5同梱guideと[Next.js公式CSP](https://nextjs.org/docs/app/guides/content-security-policy)に基づくrequest nonce方式。

- 本番だけproxy.tsで暗号学的乱数16byteのnonceを毎回生成する。固定値・クライアント指定値を使わない。
- callerからのx-nonce/CSPを削除し、新nonceとCSPをSSR向けrequest headerとresponseへ設定する。
- RootLayoutのconnection()でrequest時renderに切替。Nextがframework/inline scriptへ同じnonceを付与する。
- 静的headers生成から従来の部分CSPを除き、二重policyを避ける。その他のセキュリティヘッダー・HSTSは維持。
- nonce付きHTML/RSCはprivate/no-store。CDNのHTML共有キャッシュ・静的生成・ISRを前提にしない。
- Next.js scriptの動的ロードはstrict-dynamic、非nonce scriptは不許可。unsafe-inline/unsafe-evalは追加しない。
- 既存12か所のcol幅style属性は同じ幅のTailwindクラスへ置換。既定404のinline styleを避ける最小not-found画面を追加。
- legacy画面の履歴へのredirectは維持する。開発環境にCSPを付与せず、Hot Reload向けunsafe-evalを本番へ持ち込まない。

| Directive | 方針 |
| --- | --- |
| default-src | none、各resourceを個別許可 |
| script-src | self、request nonce、strict-dynamic |
| script-src-attr | none（HTML event handlerを許可しない） |
| style-src | self、request nonce |
| style-src-attr | none |
| connect-src | selfと設定済みHTTPS API originのみ。URL pathやcredentialsをCSPへ入れない |
| img-src / font-src / manifest-src | selfのみ |
| frame-src / frame-ancestors / object-src / worker-src / media-src | none |
| base-uri | none |
| form-action | self |

connect-srcのAPI originはNEXT_PUBLIC_API_BASE_URLから決まり、**Frontend build前に設定**する。
公開origin変更は再buildが必要。外部font/image/worker/analytics等は現機能で不要なため許可しない。
将来必要になった場合もwildcardではなく個別レビューする。
upgrade-insecure-requestsは追加しない。本番HTTPS終端・HTTP redirect・既存host限定HSTSを別途確認する。localhostのHTTP隔離検証をHTTPSへ強制変換しない。

## 4. 制約・公開前ゲート

- 全ページ動的renderでSSR負荷・費用・初回応答・cache戦略が変わる。nonce付きHTMLをCDNで共有しない。
- Nextのnonce対応は現バージョン・現在のresourceを対象にした検証。更新時に再確認する。
- strict-dynamicで信頼scriptが生成したscriptは信頼連鎖に入る。DOM XSS gadget等も防ぎ切る保証ではない。
- localStorageのtoken窃取耐性そのものは変更していない。XSS発生時の本人操作、tokenのTTL、サーバー側logout失効なしは残る。
- style-src-attr noneにより、将来inline styleを足すと壊れる。外部CSS/クラス化とテストを維持する。
- 実ブラウザ連携が内部エラーで利用できず、主要画面のhydration・CSS適用・認証後の遷移をブラウザでは未検証。HTTP200やbuildだけでE2E合格とはしない。
- 一般公開前に、Production起動＋mock API/隔離DBでログイン/再読込/401/logout/種目選択/保存/履歴/編集/削除/コピー/タイマー、console CSP違反を手動確認する。
- Railway proxy/IP識別、DB readiness、監視・復旧等のM5-11/12公開ゲートも残る。

## 5. 検証方式

隔離コピーでnpm ci、全Frontendテスト、lint、TypeScript、標準npm run build、npm audit --omit=devを実施する。
HTTPは隔離Production Frontendの33013番だけを使い、実Backend・実DBに保存要求を送らない。
API URLはダミーのhttps://api.example.invalid。ブラウザでこのURLへの認証送信は実施しない。

自動テスト：CSP directive/危険API URL/nonce変化、実Proxyの偽nonce上書き・request/response一致、開発設定・legacy redirect。
実際の履歴コンポーネントをmock hook/storage/APIとReact rendererで検証し、HTML風メモ/種目/エラーがtextになること、未認証redirect、認証解除、logout、持続性維持を確認。
実API clientのBearer送信・401エラーとconsole非出力もmock fetchで確認する。これは実ブラウザE2Eではない。

検証対象外：実DBへの追加/変更/削除、Flyway、Docker、Backend変更、Cookie認証・refresh API・token保持先の変更。

### 隔離検証結果

- Node.js 22.14.0、Next.js 16.4.5 / React18.3.1を維持。npm ci成功、package.json/lockfile変更なし。
- Frontend全49テスト成功（既存34＋追加15）、失敗・skipなし。
- ESLint、TypeScript `--noEmit --incremental false`、標準 `npm run build` 成功。
- npm audit --omit=dev：Critical/High/Moderate/Lowすべて0。
- HTTP：主要6ルート200、404画面404、旧画面307。全scriptのnonce一致、request間のnonce相違、偽装nonce不採用、no-store、style属性0を確認。
- 最初のbuildはsandboxの内部待受拒否で停止。隔離側の失敗cacheを別名保全し、許可された環境の空cacheで再buildした結果は成功。
- 実ブラウザは連携内部エラーで操作できず、成功扱いしない。認証後の実画面E2Eは公開前ゲートに残す。

変更対象はFrontend10ファイル＋文書3ファイルの計13ファイル。Backend・DB migration・依存バージョンは変更なし。

```text
frontend/app/layout.tsx
frontend/app/not-found.tsx
frontend/app/training-sessions/new/page.tsx
frontend/app/training-sessions/[sessionId]/edit/page.tsx
frontend/proxy.ts
frontend/lib/content-security-policy.ts
frontend/lib/content-security-policy.test.mjs
frontend/lib/frontend-security.test.mjs
frontend/lib/security-headers.mjs
frontend/lib/security-headers.test.mjs
docs/M5-13-frontend-security.md
docs/M5-11-security-and-roadmap.md
README.md
```

## 6. 反映条件

proxy.tsと設定helperの変更がnext devの自動再起動につながる可能性があるため、反映前にユーザー承認を得る。
実repoのfrontend/next-env.d.ts・frontend/AGENTS.mdは変更・stage・commitせず、検証で生成されたコピーを持ち込まない。
反映後はユーザーの既存ログイン/入力画面を操作せず、稼働状態だけ確認する。コミット・Push・PRは別指示。

## 7. PR前最終レビュー・手動Production E2E準備

2026-11-11再検証：隔離コピーと実repoのアプリケーション変更・テスト・設定・依存ファイルが一致することを確認し、49テスト、lint、型チェック、Production buildを再実行してすべて成功。本番依存auditは0件。
GET検証では主要6ルート200、404、旧URL307を確認。同一URLへの連続要求でもnonceが変化し、各HTMLの全script nonceが応答CSPと一致。偽装nonceは不採用、inline style属性0、Cache-Controlはprivate, no-store。
これはHTTP/ソース検証であり、ブラウザでのCSS適用・hydration・ログイン後E2Eの実施結果ではない。

### 現在用意したProduction Frontend

- 隔離コピー：`/private/tmp/myfitness-m513.gc8p7i/frontend`。
- URL：`http://127.0.0.1:33013`。既存3000番・Backendとは別プロセス。
- API設定：`https://api.example.invalid`（到達させないダミー）。本物のメール・パスワード・JWTを入力しない。
- この環境で手動確認できるのはログイン/登録の描画、相互リンク、表示切替、入力検証、404、未認証redirect、Consoleの初期CSP違反。正常ログイン・保存はできない。
- 再起動が必要になった場合も既存3000番を操作せず、隔離環境の起動者が対象を確認すること。以下は今回の隔離build/startで使用した方式であり、既存devと同じ作業ディレクトリで実行しない。

```sh
cd /private/tmp/myfitness-m513.gc8p7i/frontend
NEXT_PUBLIC_API_BASE_URL=https://api.example.invalid NEXT_TELEMETRY_DISABLED=1 npm run build
NEXT_PUBLIC_API_BASE_URL=https://api.example.invalid NEXT_TELEMETRY_DISABLED=1 npm run start -- --hostname 127.0.0.1 --port 33013
```

buildとstartは同時に実行しない。今回start済みの33013番へ重複起動しない。

### 既存Backendを使えない理由と安全な接続条件

保存POSTなしのOPTIONSで、既存BackendはOrigin `http://localhost:3000` を200、`http://127.0.0.1:33013` を403で応答した。
加えてproductionビルドはHTTP API・localhost APIを拒否する。33013番から既存8080番へ直接接続する構成は、現在のCSP/API URL/CORSのままでは成立しない。
CORSの緩和、Origin偽装、production検証の無効化、既存Backendへの中継による実DB接続は行わない。

認証後E2Eには次のいずれかを別途用意する必要がある（今回未構築・未検証）。

1. ブラウザテスト基盤のrequest interceptionでダミーHTTPS APIへの全要求をmock応答し、未処理要求はネットワーク送信せず失敗させる。認証・保存・編集・削除・401を再現する。Cookie/実tokenは使わない。
2. 既存DBとは分離したBackend/DBと、ブラウザが信頼する証明書を持つHTTPS APIホストを用意する。隔離BackendのみでFrontendの正確なOriginを許可し、Authorization/Content-Type/Idempotency-Keyのプリフライトを確認する。そのHTTPS API URLでFrontendを再buildする。既存CORS/envは変更しない。

2にはホスト・証明書・隔離Backend/DBの準備が必要であり、今回契約・デプロイ・Docker・DB構築は実施しない。実DBを使うE2Eはユーザーの別判断と操作まで行わない。

### Chromeでの手動チェック順序

1. 専用ブラウザプロファイルで33013番を開く。DevTools Console/Networkを開き、Consoleをclearして開始。認証情報・Authorizationを含むHARや画面を共有しない。
2. ログイン/登録画面、入力検証、表示切替、相互リンク、存在しないURLの404を確認。未認証の筋トレ画面からログインへ戻ることを確認。
3. 上記mockまたは隔離APIが用意できた後だけ、架空の専用アカウントでログインし、再読込時の認証維持を確認。
4. 2種目選択→新規作成→重量/回数/メモ入力→セット追加/削除/前セットコピー→タイマーStart/Pause/Resume/Reset。
5. 保存中表示・二重クリック抑止・履歴遷移を確認。履歴の値、編集、種目/セット変更、再保存・再読込、専用Session削除、ログアウトを確認。
6. mock入力のメモ・種目名にHTML風文字列を含め、文字として表示されることを確認。401時は認証解除されることを確認。
7. 各画面/クライアント遷移/ハード再読込でConsoleのCSP violation、hydration error、CSS崩れ、API拒否がないことを確認。script/style/connectの違反があれば合格にせず、directive・resource・再現操作だけを記録する。

### PR本文案

タイトル：`feat: strengthen frontend CSP and XSS protections`

- Summary：本番限定のrequest nonce CSP、SSR request/responseのnonce統一、inline styleのクラス化と404対応、JWT保存方式の比較・制約を文書化。認証方式・持続性は維持。
- Tests：Frontend49件、lint、TypeScript、Production build、production audit0件。主要画面/404/旧redirect、nonceの一致と更新、CSP、no-storeを隔離HTTPで確認。
- Security：unsafe-inline/unsafe-evalなし、外部通信は指定API Originのみ。Reactテキスト表示・401/logout・偽nonce拒否・ログ非出力を自動テスト。
- Limitations：実ブラウザE2Eは未実施。localStorage JWT窃取、strict-dynamicの信頼連鎖、将来resourceとの互換性リスクは残る。
- Deployment：全ページ動的SSRとなり静的HTML/ISR/CDN共有cacheを前提にしない。HTML/RSCはno-store。負荷・費用・本番ブラウザ検証が必要。
- Exclusions：保護対象2件、Backend、DB、env、依存更新は対象外。コミット・Push・PR作成は別承認。

PRレビュー用資料は準備済み。ただし認証後の実ブラウザE2Eを合格扱いせず、公開前ゲートとして明示する。
