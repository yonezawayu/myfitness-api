# M5-15 Railway＋Neon技術評価・実環境検証計画

調査日：2026-11-12（JST）。対象：`1800a0b9f6d3bb6fad857042a1127886f1ef3985`。
契約・サービス作成・課金・クラウド接続・デプロイ・DB操作は未実施。アプリケーションコードは変更しない。
本書のコマンド/SQLは、明示したローカル隔離テスト以外は**未実行の計画**。手順を一括実行しない。

## 1. 結論・証跡

RailwayのJava/NodeサービスとNeon PostgreSQLは構成候補として適合する。ただし**安全なクライアントIP識別とNeon実権限/TLSが未検証のため、現時点で一般公開可能とは判定しない**。

- 既存feature作業ツリーを切り替えず、非チェックアウトのdevelopをfast-forward fetchで同期。develop/originは `1800a0b...`、0/0。既存Frontend差分2件とM5-14文書を保持。
- 隔離コピー：`/private/tmp/myfitness-m515.faS8tc`。実envコピーなし。Java17/H2/MockMvc/ランダムloopback Tomcatのみ。
- 既存対象110テスト成功：ClientIpResolver46、TrustedProxy6、AuthRateLimiter5、AuthRateLimitIntegration38、Health15。
- 隔離専用 `M515TopologyTest` 4件成功：共有NAT、独立replica、X-Real-IPだけでは現resolverがclientを選ばないこと、forwardヘッダー自動書換え拒否。
- Maven package成功（compileを含む）。JAR約61.6MB、Java class major61、DevToolsは実行JARに含まれない。
- 本番JARをNeonへ接続して起動したわけではない。SQL/Flyway/TLSを実接続で検証した結果ではない。
- 証跡：隔離先の `m515-tests.log`、`m515-topology.log`、`m515-package.log`。追加テストは隔離コピー限定で、実repoのコードへ反映しない。
- M5-14のユーザー手動報告は[検証資料](M5-14-production-e2e.md)へ追記。実Backend/DBのE2Eとは区別する。既存アプリ・M5-14プロセスは保持。

## 2. Railwayプロキシ：4種類の証拠を分離

公式サポート向けの[英文問い合わせ案](M5-15-railway-support-inquiry.md)を作成した。未送信であり、送信にも別途承認が必要。

### 公式資料で確認した範囲

[Public Networking仕様](https://docs.railway.com/networking/public-networking/specs-and-limits)は、X-Real-IPをclient remote IP、X-Forwarded-Protoをhttps、X-Forwarded-Hostを元Hostとして説明する。Railway側で公開TLS証明書を提供するが、仕様は変更され得る。

### 公式資料から確認できなかった範囲

今回確認した上記仕様には、X-Forwarded-Forのチェーン形式・利用者入力の上書き/append/reject規則、安定した信頼ingress CIDR、アプリが観測するsocket peerの識別保証、private/TCP経路を含む迂回遮断が定義されていない。
「公開されていない」と断定せず、**今回確認した公式文書では確定できない**とする。ヘッダー名の存在やsupport/community投稿だけを安全な認証境界の根拠にしない。

### ローカルで確認できた範囲

現ClientIpResolverはsocket peerを最初の信頼判定に用いる。空trustでは転送情報をすべて無視。信頼したIP/CIDRからのみXFFを読み、右端から未信頼hopへ遡る。X-Real-IP/Forwardedは採用しない。
信頼済みproxyが真のpeerをappend/検証済みチェーンとして置換することが前提。不正転送値は400、不正CIDRやforward-headers-strategy変更は起動拒否。IPv4/IPv6・encoded URL・CORS回帰は隔離テストで確認。

### Railway上でしか確定できない範囲

接続元の安定性・実経路・入力ヘッダーのsanitize・IPv6・外部CDN併用時の値・private経路の到達制御は未検証。
`MYFITNESS_TRUSTED_PROXY_CIDRS` を空にすると保守的だが、全員が同じedge peerとして制限枠を共有し得る。`0.0.0.0/0`、`::/0`、広いprivate CIDR、無条件X-Real-IP採用で解消しない。

### 実環境検証案（別承認まで実行しない）

1. providerへ、public/generated/custom domainごとのpeer識別・CIDR/認証方式・XFF/X-Real-IP上書き規則・private/TCP経路の差を文書で確認する。
2. 合意した予算の一時stagingに、DBも資格情報も持たない最小diagnostic serviceだけを用意する。public/private/TCPの到達範囲を限定する。
3. テスト専用回線2系統から同じ要求を送り、接続元と必要なIPヘッダーだけを一時観測。Authorization、Cookie、本文は収集しない。IP記録も個人情報として短期保管し削除方針を決める。
4. XFF先頭/末尾偽装、X-Real-IP、Forwarded、重複/不正ヘッダー、IPv4/IPv6、複数proxy、別hostと直接private経路をテスト。providerの保証と観測を両方照合する。
5. 安定した境界が確認できた場合だけ狭いCIDRの現方式を採用。X-Real-IP専用の方式が必要なら、経路限定・peer識別を含む別設計/コードレビュー/承認を行う。仕様不明なら公開停止。

Spring Bootは `SERVER_FORWARD_HEADERS_STRATEGY=none` を維持。framework/nativeで先にremoteAddrを書き換える構成は現resolverと不整合となる。HTTPS scheme/redirectの正規化はIP判定とは別検討。

## 3. 認証レート制限の本番適合性

| 項目 | 判定 |
| --- | --- |
| 直接接続・未信頼ヘッダー偽装 | 隔離テスト成功、実接続元単位 |
| 明示信頼proxy・複数client | 隔離テスト成功、右側の最初の未信頼hopごとに分離 |
| 共有NAT | 別アカウントでも同一IP枠を共有。仕様上の利用阻害リスク |
| login10回/300秒、登録5回/300秒 | 維持。429/Retry-After/no-store、最大10,000枠、期限回復・並列性を確認 |
| encoded URL | 通常URLと共通枠。拒否すべき不正URLの回帰を確認 |
| 単一process | 利用可能。ただし再起動・rolling交代で枠リセット/分離 |
| 複数replica | 枠は共有されず、2台で合計20loginを許し得る。共有制限導入前に拡張しない |
| Railway | ingress信頼境界が未解決。現設定だけで対応完了にはしない |

NAT対策として回数だけを無条件に増やさない。公開規模に応じ、アカウント単位の追加制限、共有store、登録悪用対策を別設計する。

## 4. Neonの仕様と検証計画

| 項目 | 文書で確認した範囲／実接続ゲート |
| --- | --- |
| PostgreSQL | PG16対応の公式紹介はある。現project作成ガイドはversion選択を説明するが、今回取得資料から最新の全選択肢は確定できない。作成承認後にPG16選択とserver_versionを確認。別majorへ自動変更しない |
| managed権限 | 真のSUPERUSER不可。Console/API等の管理ロールとSQL作成の制限ロールを区別 |
| owner | NOLOGIN対応。schema/objectの所有者とmigrator/appを分離する設計は可能候補 |
| DB所有者 | 変更用APIが文書化されている。NOLOGIN owner指定・実membership・schema所有権の可否は未接続 |
| SET ROLE | Postgres membershipで実現する設計。NeonでのGRANT options・DDL・Flyway接続全体は未実行 |
| TLS | JDBCでverify-full＋検証可能なCA/truststore。hostname/CA不一致を拒否することを実試験する |
| pooler | transaction poolingではsession SETを維持する前提にできない。初回はappも小さいdirect pool、Flyway/backupはdirect |
| 接続数 | 0.25CUの現公式値は112、管理予約7。poolerの10,000clientは同時DB query保証ではない。実SHOW値を確認 |
| Hibernate | 事前migration後にappでvalidate。Neon実接続の成功は未確認 |

根拠：[PG16紹介](https://neon.com/blog/json-features-pg-16)、[projects](https://neon.com/docs/manage/projects)、[roles](https://neon.com/docs/manage/roles)、[database ownership](https://neon.com/docs/manage/databases)、[pooling](https://neon.com/docs/connect/connection-pooling)。

### 空DB・最小権限の初期構築（未実行）

- ローカル既存ユーザー・記録・dumpはインポートしない。空のstagingを先に検証し、空のproductionは別承認。
- provider管理者は初期設定専用。`myfitness_owner NOLOGIN`、`myfitness_migrator LOGIN NOINHERIT`、`myfitness_app LOGIN NOINHERIT` をSQLで作成する候補。appにneon_superuserや管理membershipを付与しない。
- 管理者が必要なowner SET/ADMIN権限を持つことを確認してから、owner所有のDB/public schema、PUBLIC権限の剥奪、app/migrator CONNECT、app schema USAGEを設定する。ローカルbootstrap/tablespace移管SQLは転用しない。
- migratorにownerへのSETのみ許可し、INHERIT/ADMINなし。実role graphを監査。Neonの管理者はローカルSUPERUSERと同等ではないため、失敗時に権限を広げて進めない。
- 各資格情報は本番専用乱数、管理者・app・migrator・JWTを分離。対話/secret管理で設定し、引数・画面・ログへ出さない。

計画する接続（placeholder、未接続）：

```text
jdbc:postgresql://<direct-endpoint>:5432/myfitness?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory
```

[Neon TLS](https://neon.com/docs/connect/connect-securely)と[pgJDBC TLS](https://jdbc.postgresql.org/documentation/ssl/)を照合。pgJDBCのrequireはverify-fullの代替ではない。CA検証失敗時の弱体化・秘密値入りURL・検証無効factoryは禁止。

### V1～V4の静的確認と将来の実試験

V1はDDLのみ、V2は所有ユーザー列追加、V3はNULL所有者が残る場合に停止。空DBなら既存記録のbackfillは不要という静的見込み。
V4は `current_user=myfitness_owner` とappロール存在を要求し、冪等性table/identity/制約を作り必要DML/sequence権限を付与。これらの前提を満たすまで実行しない。
V4だけでDB/publicのPUBLIC ACLやprovider default権限を全て消すわけではないため、初期ACL設計と最終監査が別途必要。

専用migration jobのみへFLYWAY_URL/USER/PASSWORDを渡す。実行順序案（**今回は未実行、各段階に確認ゲートあり**）：

```sh
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:info
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:migrate
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:validate
```

まずdirect接続でsession_user=migrator/current_user=owner、正しいDB/空schemaを確認。owner/historyを含む全オブジェクト所有者、PUBLIC ACL、appのCRUDだけの成功とDDL/旧API/Flyway履歴更新の拒否、appからownerへSET ROLE不可を確認する。
PG16標準SQLの候補は[M5-11手順案](M5-11-production-runbook.md)を参照。ただしNeon固有権限をH2で再現したとはしない。今回SQL/Flywayは一切適用していない。

### バックアップ・復旧

[Neon pg_dump](https://neon.com/docs/manage/backup-pg-dump)に従いdirect接続でcustom形式、SHA、Flyway/件数/制約/所有権/ACLのmanifestを取得する計画。資格情報はpg_service/保護されたpassfile等を用い、password入りCLI例は転用しない。
ロール定義・membershipは別の秘密値なしmanifestで再現し、provider予約ロールを復元しない。新しい隔離DBへ復元し、owner/migrator/app・データ・schema・APIを比較する。pg_restore成功だけを合格条件にしない。
PITRはLaunchの最大7日が自動設定されるわけではない。現既定1日から必要な保持期間を明示設定する計画。[History window](https://neon.com/docs/postgres/backup-restore/history-window)
provider外の暗号化dumpも保管し、RPO/RTO・復旧担当・定期リストアを決める。料金・保存先は別承認。
migration失敗時は後続停止、接続先・履歴・transaction確定状態を保全。自動repair/clean/DROP/再実行/本番上書き復元はしない。旧artifactへ戻せるschema互換性を確認し、不可なら承認後に新restore branch/別DBへ復旧して切替。復旧点以降の書込み損失も承認判断に含める。

## 5. Railwayでの起動・設定案

### Backend

service rootはrepo root。build `./mvnw -B -DskipTests package`、実行JARは `target/demo-0.0.1-SNAPSHOT.jar`。CI test成功をrelease条件にする。
[Railpack Java](https://railpack.com/languages/java/)は既定Java21のため `RAILPACK_JDK_VERSION=17` を明示する。pomのtarget17だけではruntime17固定を保証しない。
PORTは現application.ymlで参照していないため、shell展開できるstart commandで明示する（Railwayでは未起動）：

```sh
java -XX:MaxRAMPercentage=60.0 -jar target/demo-0.0.1-SNAPSHOT.jar --server.address=0.0.0.0 --server.port="${PORT:-8080}"
```

Railway設定画面がこのshell展開を実行することと実listen portをstagingで確認する。healthcheck用PORTと一致させる。[Railway healthchecks](https://docs.railway.com/deployments/healthchecks)

| 設定 | 計画値・注意 |
| --- | --- |
| SPRING_PROFILES_ACTIVE | prod（productionも既存対応） |
| MYFITNESS_DB_URL/USERNAME/PASSWORD | direct TLS URL / myfitness_app / app専用secret |
| MYFITNESS_JWT_SECRET | 本番専用、Frontend/CIへ渡さない |
| MYFITNESS_CORS_ALLOWED_ORIGINS | 正確なFrontend HTTPS Originのみ |
| SERVER_FORWARD_HEADERS_STRATEGY | none |
| MYFITNESS_TRUSTED_PROXY_CIDRS | 未確定。勝手に設定しない。公開ゲート |
| DEBUG / TRACE | false、親環境も監査 |
| Hikari | 現コードは明示設定なし。含まれる5.1.0の既定最大10に依存。初期案は最大3、min idle0、idle timeout60秒、keepalive0。実負荷/Neon wakeをstagingで確認 |
| Flyway / Hibernate | 自動migration無効・ddl-auto=validateを維持 |
| probes | deployは/health/ready、外部生存監視は/health/live。DBは別監視 |

Hikari案はSPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE/MINIMUM_IDLE/IDLE_TIMEOUT/KEEPALIVE_TIME等のサービス変数で設定可能。今回既存envは変更しない。appサービスへadmin/Flyway secretを渡さない。

### Frontend

service rootは `frontend`。[Railpack Node](https://railpack.com/languages/node/)に従い `RAILPACK_NODE_VERSION=22`。npm ci→npm run build、startは次の案：

```sh
npm run start -- --hostname 0.0.0.0 --port "${PORT:-3000}"
```

NEXT_PUBLIC_API_BASE_URLはbuild前にBackend公開HTTPS Originを設定し、build/runtimeで同じ値を維持する。秘密値は禁止。MYFITNESS_FRONTEND_PUBLIC_ORIGINは正確なFrontend HTTPS Origin（HSTS対象）、NEXT_TELEMETRY_DISABLED=1。
現next.configはstandalone未設定だがnext start方式で起動可能候補。nonce CSPはNode proxy/SSRが必要で、静的exportへ変更しない。HTML/RSCのprivate/no-store、全ページ動的SSRによりCPU・RAM・通信量を測定する必要がある。
Railway向けsource変更は今回は不要。将来start wrapper/runtime固定の宣言ファイルを追加する案は別レビューとする。

## 6. リソース：事実と推定

- 事実：Java17向けJAR生成・DevTools除外、Next ProductionはM5-14隔離環境で起動済み。PostgreSQL接続poolの明示設定なし。
- 推定初期上限：Backend RAM1GiB、Frontend512MiB～1GiB、build2～4GiBを候補にstaging計測。JVM heap60%以外にもmetaspace/direct buffer/thread等が必要。
- 本番起動秒数・RSS・CPU・GC・Neon起床時間は未測定。H2テスト時間をNeon本番起動時間として流用しない。Railway Linuxのcgroup認識・OOM・health timeoutも実試験する。
- 小規模ではapp1 replica・DB0.25CUを候補。direct pool3＋migration/backup余裕を含める。DB接続維持・監視SELECT/定期処理がNeon scale-to-zeroを妨げる可能性を測定する。

## 7. 最新料金と3ケースの試算

確認日2026-11-12。[Railway料金](https://railway.com/pricing)はservice RAM $0.00000386/GB秒、CPU $0.00000772/vCPU秒、egress $0.05/GB。
[課金説明](https://docs.railway.com/pricing/understanding-your-bill)：Hobbyは最低$5（$5利用分込み）、Proは最低$20。利用額へ最低料金を二重加算しない。
[Neon料金](https://neon.com/pricing)：Launchは月額固定費/最低額なし、$0.106/CU時、DB $0.35/GB月、履歴 $0.20/GB月、snapshot/定期backup $0.09/GB月。表示Typical spend $15は最低料金ではない。旧資料の$5最低額/$0.14単価を今回に流用しない。

計算前提：Railway30日=2,592,000秒、平均使用RAM/CPUを課金。NeonのCU時は実active時間×サイズで別計算。為替未取得のため$1=150円を仮定。税・為替手数料・ドメイン・外部dump保存・build/追加branch/超過通信等は別。数値は見積り条件であり利用者数から確定しない。

| ケース | Railway仮定（Backend＋Frontend合計平均） | Neon仮定（CU時/DB GB/履歴 GB/backup GB） | Railway | Neon Launch | 合計/月 | 円概算 |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| 自分用 | RAM0.85GB、CPU0.02、egress1GB | 25 / 0.5 / 0.1 / 0.5 | $8.95 | $2.89 | $11.84 | 約1,777円 |
| 少人数日常利用 | RAM1GB、CPU0.05、egress10GB | 190 / 1 / 1 / 2 | $11.51 | $20.87 | $32.38 | 約4,856円 |
| 増加時 | RAM2GB、CPU0.25、egress100GB | 730 / 5 / 10 / 10 | $30.01 | $82.03 | $112.04 | 約16,806円 |

Railway = max(plan minimum, RAM×秒×単価＋CPU×秒×単価＋egress)。Neon = CU時×0.106＋DB×0.35＋履歴×0.20＋backup×0.09。増加時Proでも仮定usageが$20を超えるため同表額、共有レート制限は別途必要。
最小ケースの25CU時は低頻度・睡眠が成立する前提。0.25CUを730時間維持すると182.5CU時、computeだけで約$19.35へ増える。実測前は少人数ケース程度の予算余裕を確保する。
無料枠は恒久本番の推奨根拠にしない。Neon Freeは現100CU時/project・1GB・復旧最大6時間/履歴1GB。Railway Freeの0.5GB/service等はJava安定運用の実測前提を満たす保証がない。

### 課金制御

- [Railway cost control](https://docs.railway.com/pricing/cost-control)：通知とCompute hard limitを別設定可能。hard limitは全workload停止を伴うため可用性との判断・担当が必要。Agent課金枠は別物で、今回利用しない。
- [Neon spending notifications](https://neon.com/docs/introduction/spending-notifications)：80%/100%通知、15分周期。通知は総額の自動停止を保証しない。project別compute/storage/transfer quotaとは区別し、採用時に対象/精度/停止動作を確認する。
- 初期通知案：Railway $10、Neon $10（自分用前提）。hard/quotaは承認後に別決定。上限ぎりぎりの設定や通知だけで請求の絶対上限を保証しない。
- RAM/SSR、DB常時起動、増える使用済み冪等キー、WAL/保持期間、backup世代、egress、preview環境の放置が増額要因。

## 8. 公開前チェックリスト

状態は「ローカル実装」と「本番実証」を分ける。未検証項目を対応済みにしない。

| 項目 | 状態 | 残作業 |
| --- | --- | --- |
| nonce CSP・React安全表示 | 対応済み（隔離） | 公開HTTPS/実Backendで回帰 |
| JWT localStorage | 設計済み・未検証（本番） | 残存XSSリスクの受容、Cookie/失効は将来別設計 |
| Swagger本番無効・500情報抑制 | 対応済み（コード/既存テスト） | 本番profile/envを確認 |
| HTTPS/HSTS/CORS | 設計済み・未検証 | 公開CA・実Origin・scheme/redirect確認 |
| レート制限 | 対応済み（単一process） | NAT影響・rolling運用、replica拡張前の共有化 |
| ingress信頼・IP | 公開ブロッカー | 保証されたpeer/header境界・偽装試験 |
| health/live/ready | 対応済み | Railway受付/継続監視を設定 |
| DB障害検知 | 未対応（本番） | readinessはDB疎通なし。別監視/通知・復旧運用が公開前必須 |
| Secrets管理 | 設計済み・未検証 | 役割分離、MFA、限定job、ログ遮蔽・rotation |
| Neon role/TLS/Flyway | 公開ブロッカー | 空stagingで実権限・validate・拒否テスト |
| backup/PITR/復旧 | 設計済み・未検証 | 別DBrestore、RPO/RTO、暗号化外部copy |
| ログ/監視/通知 | 未対応（本番） | secret非出力、5xx/429/pool/OOM通知・担当 |
| 課金alert/停止方針 | 設計済み・未検証 | 予算/担当/停止の承認、実設定 |
| 本番起動/RSS/遅延 | 設計済み・未検証 | PORT/runtime/memory/Neon wake/SSR計測 |
| 実Backend＋Neon E2E | 公開ブロッカー | M5-14モック成功では代替しない |

## 9. 次の工程・承認

1. 契約前：Railwayのheader/peer/迂回経路に関する公式回答を取得し、採用可能な信頼方式を設計。回答が得られなければ構成見直し。
2. ユーザーが予算・地域・個人情報取扱い・停止条件・担当を承認後にだけ、期限付きstagingサービス/空Neon projectを作成する別タスクへ進む。
3. NeonでTLS・owner/SET ROLE・V1～V4・拒否テスト・Hibernate・dump/PITR/restoreを検証。ローカル既存データは移行しない。
4. Railwayでruntime17/22・PORT・RSS/CPU/起動・IP・health・課金制御を検証。実Backend＋空staging DBのE2Eを実施する。
5. 結果レビュー後、production作成/デプロイは改めて明示承認を得る。

この工程で必要になる費用・外部サービス・DB操作・Secrets設定・将来コード変更は今回の承認に含まれない。コミット/Push/PRも別指示。
