# M5-12 信頼プロキシとヘルスチェック

2026-11-08にGit管理外の隔離コピーで実装・テスト。2026-11-09、ユーザーの自動再起動リスク承認後に実リポジトリへ反映。
クラウド・既存env・ローカル実DBは変更していない。稼働Backendで新しいhealth応答と保護APIの401を確認。本番Railwayでの検証・設定は未実施。

## 信頼境界

既定は信頼プロキシなし。全Forwarded / X-Forwarded-For / X-Real-IPを無視し、socket peerの数値IPを使う。
認証レート制限の判定にだけ専用ClientIpResolverを使い、ServletのremoteAddr・scheme・hostは書き換えない。
`server.forward-headers-strategy=none` を維持し、native/framework/空/不正値への上書きは起動失敗にする。
独自ForwardedHeaderFilterやRemoteIpValve等で事前にpeerを書き換える構成は併用しない。

設定例（架空の専用proxy network。実envの変更は今回行わない）：

```text
MYFITNESS_TRUSTED_PROXY_CIDRS=10.20.0.2/32,2001:db8:ffff::2/128
```

- カンマ区切りの数値IPまたはCIDR、最大64項目。空全体は無効化、空項目・hostname・不正prefix・/0は起動時拒否。
- IPv4/IPv6を別familyとして照合。IPv4-mapped IPv6はIPv4へ正規化するため、mapped用の/128指定は使わずIPv4 CIDRを指定する。
- trust listはproxy専用の送信元だけ。client network、private network全体、loopback全体等を安易に登録しない。
- CIDRのhost bitsはprefix部分で照合する。初期は/32、/128の厳密なpeer指定を優先する。

## X-Forwarded-Forの処理

未信頼peerならヘッダーを解析しない。信頼済みpeerの場合だけ、X-Forwarded-Forのチェーンを右端から辿り、最初の未信頼IPを利用者として採用する。
その左側の自己申告値はIPの選択に使わない。直前proxyが観測した実peerをappendするか、検証したチェーンへoverwriteすることが必須。
任意の入力ヘッダーをそのまま転送するproxyは信頼対象にしない。

```text
攻撃者の偽装値, 実クライアント, 信頼proxy1 → socket peer=信頼proxy2
                   ↑ 右側の信頼hopだけを除いて、ここで停止
```

受け付けるのはbareの数値IPだけ（hostname/port/bracket/zone/obfuscated/for=形式は不可）。
最大2048文字・32hop。複数の同名ヘッダー、空要素、不正IP、全hop信頼済みで境界不明の場合は、認証POSTを汎用400で拒否する。
エラーにIP/ヘッダー/資格情報を含めず、Cache-Control:no-storeを付ける。
信頼peerなのにXFFがない場合はpeerへfallbackし、共有枠になり得ることを運用監視する。
Forwarded / X-Real-IPは信頼peerからでも使用しない。複数ヘッダー系列を混在解析しない。

login10回/登録5回・各300秒、429/Retry-After、最大10,000枠、期限切れ回収・並列保護は既存実装のまま。
permitAllとrate filterのMvcRequestMatcher共有を維持し、URLの独自decodeは追加しない。
OPTIONSは枠を消費しない。認証API以外の認可・既存CORS設定は維持する。

制約：単一process内の枠で、再起動時リセット・rolling更新中の一時的な枠分離・NAT共有は残る。
権限を与えたproxyの侵害・過大なCIDR設定はアプリ側では防げない。複数replica前には共有制限が必要。

## Railway：確認できたことと未確認事項

確認日：2026-11-08。[公式header仕様](https://docs.railway.com/networking/public-networking/specs-and-limits)にはX-Real-IP、X-Forwarded-Proto、X-Forwarded-Host等の記載がある。
一方、本調査で確認した公式文書からは、安定した信頼peer IP/CIDR、任意入力の上書き保証、XFFチェーンの構築保証、private/TCP経路を含めた迂回遮断までは確定できない。

**今回のresolverを設定するだけでRailway対応完了とはしない。**
Railway用の自動CIDR、全private trust、X-Real-IP無条件採用は追加しない。正式仕様が確定するまでは空trust listを維持する。
将来の本番構築では次を公開ゲートにする：

1. public ingressの実socket peerと安定した信頼元識別方式をproviderへ確認。
2. 利用ヘッダーのsanitize/append/overwrite保証を文書で確認。X-Real-IPのみを使う保証なら専用方式を別設計し、このXFF方式へ推測で流用しない。
3. private/TCP/別hostを含む迂回経路とネットワーク境界を限定。
4. 任意XFF/X-Real-IP/Forwarded偽装、複数proxy、IPv4/IPv6、2利用者の枠分離、HTTPS scheme/hostを実環境で検証。
5. 安全な識別ができなければ一般公開停止。全員peer共有枠になる問題は未解決の公開ブロッカーとして扱う。

TLS scheme/redirectへの転送情報反映は今回追加しない。IP resolverはHTTPS判定器ではない。
将来信頼scheme処理が必要なら同じtrust境界で別途設計し、forward-headers-strategyを安易に切り替えない。
[Spring Boot forward header仕様](https://docs.spring.io/spring-boot/3.4/how-to/webserver.html)

## ヘルスチェック

追加依存なしの専用Controllerを採用し、Spring Boot ApplicationAvailabilityを使う。
Actuatorは導入せず、不必要な管理endpointを作らない。

| GET URL | 正常200の条件 | 用途 |
| --- | --- | --- |
| /health/live | LivenessState.CORRECT | processの生存状態。外部DB障害をrestartループにしない |
| /health/ready | liveness正常かつReadinessState.ACCEPTING_TRAFFIC | 起動完了・traffic受入状態 |
| /health | /health/readyと同じ | provider向け簡易URL |

異常/未readyは503。本文は `{"status":"UP"}` / `{"status":"DOWN"}` のみ、Cache-Control:no-store。
認証不要の許可は上記GETだけ。管理系pathをwildcardでpermitAllにしない。他の保護APIは401を維持する。
起動前にHTTP server自体がない段階は接続失敗になる。起動中のreadiness拒否・停止開始・liveness異常はAvailabilityの状態に従う。

**DB確認は行わない。** 正常readyはDBの現時点の接続可否・Flyway状態・全APIの正常性を保証しない。
Hibernate validateは従来どおり起動時に実行するが、その後のDB障害は別の監視が必要。
DB SELECT1 readiness、scheduler、専用pool等は今回は追加しない。将来追加するなら上限timeout/cache/同時実行数/Neon wake費用を別途検証する。
通常監視はAuthorizationなしでliveへ、deploy受付にはreadyへ。継続監視・providerによるrestart判断・監視間隔は別設定。
[Spring Boot availability/probes](https://docs.spring.io/spring-boot/3.4/reference/actuator/endpoints.html)

## テストと反映条件

H2・MockMvc・隔離Tomcatランダムportだけを使う。実envはコピーせず、実DBへの接続・Flyway・実データ操作なし。
数値IP/CIDR境界、偽装prefix、未信頼hop、IPv6/mapped、異常設定・ヘッダー、同一/別利用者、encoded URL、429/CORS/OPTIONS、health200/503/401、本文最小化、DB connection借用0をテストする。
M5-10の全テストを残し、rate limit・Swagger無効化・情報漏えい回帰も同じsuiteで実行する。

稼働BackendがDevToolsを読み込んでいるため、実repoのソース反映・自動buildにより再起動し得る。
今回の反映に付随する自動再起動リスクはユーザー承認済み。手動停止/再起動・実repoでのMaven buildは行っていない。Frontend2ファイルは反映対象外。

### 隔離検証結果

- Java17、Backend全169件：成功、失敗/エラー/skip 0（既存102＋追加67）。
- 追加内訳：ClientIpResolverTest 46、TrustedProxyIntegrationTest 6、HealthIntegrationTest 15。
- Maven `-DskipTests compile`：成功。
- 新規source/test/doc 11ファイルの差分空白検査、Markdownリンク・code fence・秘密情報パターン検査：成功。
- 最初の制限付き実行はsandboxのrandom port待受拒否で失敗。待受権限を得た隔離実行で全件再検証し成功。
- Frontend変更・新規依存・migrationなし。Frontend全テストは今回未実施。

変更ファイル（隔離検証後、実リポジトリへ反映済み）：

- src/main/java/com/myfitness/api/common/config/ClientIpResolver.java
- src/main/java/com/myfitness/api/common/config/AuthRateLimitFilter.java
- src/main/java/com/myfitness/api/common/config/SecurityConfig.java
- src/main/java/com/myfitness/api/common/health/HealthController.java
- src/main/resources/application.yml
- src/test/java/com/myfitness/api/common/config/ClientIpResolverTest.java
- src/test/java/com/myfitness/api/common/config/TrustedProxyIntegrationTest.java
- src/test/java/com/myfitness/api/common/config/HealthIntegrationTest.java
- docs/M5-12-proxy-health.md
- docs/M5-11-production-runbook.md
- docs/M5-11-security-and-roadmap.md
