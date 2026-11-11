# M5-11 本番DB初期構築・デプロイ・復旧手順案

作成：2026-11-08。第一候補：Railway×2＋Neon Launch、Singapore。
**全工程は未実行。本番サービス・DBへの接続試験も未実施。別途承認後に使う手順案であり、そのまま一括実行しない。**
前提・費用は[比較文書](M5-11-deployment-comparison.md)、公開ゲートは[チェックリスト](M5-11-security-and-roadmap.md)。

## 0. 実行前の停止条件

- 契約/料金/地域/個人情報取扱いの承認、予算通知の担当、復旧担当が未決定ならサービスを作らない。
- proxy/IP対策、health、固定runtime/build、厳格CSPまたは同等リスク対策の承認が未完了なら一般公開しない。
- managed DBでownerへのSET ROLE・DDL・ACLを再現できなければ停止。appへ管理権限を足して回避しない。
- 接続先・空DB・role・Git SHA・migration SHAが一致しなければ停止。
- migrate/validate/所有者/ACL/件数/起動/E2Eに失敗したら後続停止。自動repair/clean/DROP/再実行/復元はしない。

## 1. アカウント・プラン

承認後にRailway、Neon、GitHubの管理者を決め、MFAと回復コードを安全に保管。
Railway Hobby、Neon Launchを初期候補とし、Singaporeの提供・PG16の選択可否を契約時に再確認。
Hobbyの運用・権限要件が不足する場合だけProへ変更する。無料試用を恒久的予算として扱わない。
ドメイン取得、外部backup保存先、個人情報の海外保管についても別承認を得る。

## 2. GitHub連携・release管理

GitHub Appのrepositoryアクセスはこのrepoだけ。最初は本番への自動deployを無効にして設定する。
mainの保護、必須CI、release commit/tag、承認担当を決定。develop/PR previewには本番Secretsを渡さない。
自動化する場合はCI成功後のprotected environment＋手動承認＋production concurrency=1。
GitHubプランによってrequired reviewers等が使えるか確認し、使えなければ手動releaseで同等の承認を記録。
同じcommitからartifact/imageを1度作り、SHA/digestを保持。新Backendとmigrationの世代混在を防ぐ。
RailwayのGit連携を使うならWait for CIを有効にし、migration前に自動起動しない順序を別途設計する。[公式Git autodeploy](https://docs.railway.com/deployments/github-autodeploys)

デプロイトークンが必要ならproject/environmentの最小スコープ。OIDC利用可否はサービス別に確認し、未確認のOIDC対応を前提にしない。
production Secrets変更は今回の範囲外。将来もforkや未承認PRを実行するjobに渡さない。

## 3. 本番DBを空から作成

Neon Launchでproduction専用project/branch、PostgreSQL16、Singaporeを選択。
ローカルのdump・13ユーザー・既存記録はインポートしない。データ移行は別タスク・別承認。
providerが作る初期管理ロールは緊急管理・初期設定専用とし、appのDataSourceに使わない。

Neonのconsole/API作成ロールは強いmembershipを持つため、app/migrator/ownerはSQLで制限付きロールとして作る。
managed adminは真のSUPERUSERではない。ローカルbootstrapの属性変更や共有DB/tablespace移管SQLを転用しない。[Neon roles](https://neon.com/docs/manage/roles)

## 4. ロール・所有権・資格情報

| ロール | 用途・許可 |
| --- | --- |
| provider管理者 | 初期管理・緊急作業のみ。既定roleの名称・権限はproviderに従う |
| myfitness_owner | NOLOGIN、schema/全アプリobject所有者、通常の接続不可 |
| myfitness_migrator | LOGIN、NOINHERIT、ownerへのSETのみ。appから所属させない |
| myfitness_app | LOGIN、NOINHERIT、CONNECT/schema USAGE、V4で限定GRANT。全管理属性なし |

以下SQLは **空の新規本番DB用の検証前テンプレート**。既存ロールがあれば停止し、重複作成や再設定はしない。
PG16構文のmembership optionsは[PostgreSQL GRANT](https://www.postgresql.org/docs/16/sql-grant.html)を参照。

```sql
-- provider管理者の専用接続。管理コンソールの接続先を確認してから実行する計画。
-- 作成ロールは最初NOLOGINで作り、パスワード設定・認証検証まで外部利用させない。
BEGIN;
CREATE ROLE myfitness_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
  NOREPLICATION NOBYPASSRLS;
CREATE ROLE myfitness_migrator NOLOGIN NOINHERIT NOSUPERUSER NOCREATEDB
  NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE myfitness_app NOLOGIN NOINHERIT NOSUPERUSER NOCREATEDB
  NOCREATEROLE NOREPLICATION NOBYPASSRLS;
GRANT myfitness_owner TO myfitness_migrator WITH INHERIT FALSE;
GRANT myfitness_owner TO myfitness_migrator WITH SET TRUE;
GRANT myfitness_owner TO myfitness_migrator WITH ADMIN FALSE;
COMMIT;
```

次にprovider管理者へownerのSET/必要なADMIN権限を限定付与し、新しい `myfitness` DBをowner所有で作る。
この部分はprovider管理者名・managed権限によって異なるため、実際の管理ロール名を照合し隔離Neon環境で検証してから実行用SQLを確定する。
`CREATE DATABASE myfitness OWNER myfitness_owner` はtransaction外で実行する必要がある。
providerがDB所有者移管を認めない場合は停止して設計レビュー。schema/objectのowner統一とDB管理者所有の例外を承認してから進める。
PG16のGRANT membership optionは1文1項目で指定し、複数optionのカンマ列挙を使わない。実接続での構文・権限検証は別工程。

myfitness接続を確認後のSQL案：

```sql
BEGIN;
ALTER SCHEMA public OWNER TO myfitness_owner;
REVOKE ALL ON DATABASE myfitness FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT CONNECT ON DATABASE myfitness TO myfitness_app, myfitness_migrator;
GRANT USAGE ON SCHEMA public TO myfitness_app;
-- owner自身は所有権からDDL可能。provider管理者の接続権は別途維持・確認する。
COMMIT;
```

managed環境ではPUBLIC以外のprovider role・default ACLも棚卸しする。provider予約roleを変更しない。
所有権変更は対象DB/schemaを列挙し、REASSIGN OWNEDを使わない。
owner所属はmigratorと承認された管理者のみ。migratorのADMIN OPTIONは不要。

資格情報は本番専用にOS乱数で生成し、ローカルのDB/JWT資格情報を再利用しない。
安全な管理端末のpsql対話 `\password myfitness_app` / `\password myfitness_migrator` 等で設定し、LOGINを有効化する工程を別途レビューする。
端末echo・SQL/parameterログ・CI traceに秘密値/検証子を出さないことを確認。コマンド引数にPASSWORDや接続URIのpasswordを入れない。
保存はsecret managerを優先、端末保管が必要ならGit外dir700/file600。
app/migratorはそれぞれ別TCP接続で正しいpassword成功・誤password拒否を検証し、admin資格情報で代用しない。

## 5. 接続方式・TLS

最初は小さいHikari pool＋Neon **direct endpoint** を使用する計画。
Flyway/pg_dump/restoreは必ずdirect接続。poolerのtransaction poolingではsession SETが維持されないため、FlywayのSET ROLEに使用しない。[Neon接続方式](https://neon.com/docs/connect/connection-pooling)

JDBC例（ホスト・DB名はプレースホルダー、URLにpasswordは入れない）：

```text
jdbc:postgresql://ep-REPLACE.ap-southeast-1.aws.neon.tech:5432/myfitness?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory
```

上記はJVM truststore利用の設計案であり未接続。provider実endpoint・SNI・CA/JRE truststoreを照合する。
証明書検証に失敗してもsslmode=requireや検証無効factoryへ落とさない。[pgJDBC TLS](https://jdbc.postgresql.org/documentation/ssl/)
将来appだけpoolerへ変える場合もprepared statements/transaction advisory lock/JPAを隔離負荷試験し、Flywayはdirectを維持する。

## 6. Flyway V1～V4初期適用

専用migration jobへだけ `FLYWAY_URL`（上記direct）、`FLYWAY_USER=myfitness_migrator`、`FLYWAY_PASSWORD` を注入。
app serviceにはFLYWAY_PASSWORDを渡さない。jobは固定commitのrepoルートで実行、値のecho・set -x・Maven -Xは使用しない。

**下記は本番未実行。新しい空DBを確認・承認した後だけ実行するコマンド例。**

```sh
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:info
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:migrate
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:validate
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:info
```

実行前に別のmigrator接続で `session_user=myfitness_migrator`、SET ROLE後 `current_user=myfitness_owner` を検証する。
空DBではbaseline/repairをしない。V1→V2→V3→V4を順番に実行。
V3のNULL ownerガードは空DBで対象行なし。V4はownerでの実行とmyfitness_app存在が必須。
V1～V4はマージ済みファイルをそのまま使い、SQLを本番用に書き換えない。

期待値：業務11テーブル＋flyway_schema_historyの計12、identity sequence11。
users/session/exercise/set/creationは初期0件、Flyway成功4・失敗0。
schema/業務table/sequence/historyのownerを確認する。managed内部objectはこの件数に含めない。

### 適用後の読み取り確認SQL案

```sql
SELECT current_database(), session_user, current_user, version();
SELECT installed_rank, version, description, checksum, success
FROM public.flyway_schema_history ORDER BY installed_rank;
SELECT c.relname, c.relkind, pg_get_userbyid(c.relowner) AS owner, c.relacl
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind IN ('r','S') ORDER BY c.relkind, c.relname;
SELECT rolname, rolcanlogin, rolsuper, rolinherit, rolcreatedb, rolcreaterole,
       rolreplication, rolbypassrls
FROM pg_roles WHERE rolname IN ('myfitness_owner','myfitness_app','myfitness_migrator');
SELECT (SELECT count(*) FROM public.users) AS users,
       (SELECT count(*) FROM public.training_sessions) AS sessions,
       (SELECT count(*) FROM public.training_exercises) AS exercises,
       (SELECT count(*) FROM public.training_sets) AS sets,
       (SELECT count(*) FROM public.training_session_creations) AS creations;
SELECT has_table_privilege('myfitness_app','public.users','SELECT') AS users_read,
       has_table_privilege('myfitness_app','public.users','INSERT') AS users_insert,
       has_table_privilege('myfitness_app','public.users','DELETE') AS users_delete,
       has_table_privilege('myfitness_app','public.training_session_creations','UPDATE') AS keys_update,
       has_schema_privilege('myfitness_app','public','CREATE') AS app_ddl;
```

期待ACL：users SELECT/INSERT、training3テーブルCRUD、関連4sequence USAGE/SELECT。
creation SELECT/INSERTとsequence USAGE/SELECTのみ。旧API table/historyへの不要DML、appのDDL/SET ROLE管理者は禁止。
appとPUBLICのDB CREATE/TEMP、schema CREATE、不要membership/default privilegesも確認。
権限拒否の実操作テストは隔離環境で行い、本番ではまずcatalog/has_*検査を使う。
今後のmigrationはownerで行い、新objectのapp権限をそのmigrationで個別に付与する。ALL TABLESへの広いdefault GRANTは設定しない。

## 7. Backend環境変数

以下は必要項目の一覧。実値はサービスSecret UI/承認jobから設定し、リポジトリのenvをcopyしない。

| 変数 | 本番値の方針 |
| --- | --- |
| SPRING_PROFILES_ACTIVE | prod（productionも実装対応） |
| MYFITNESS_DB_URL | TLS検証するdirect JDBC URL |
| MYFITNESS_DB_USERNAME | myfitness_app |
| MYFITNESS_DB_PASSWORD | app専用Secret |
| MYFITNESS_JWT_SECRET | 本番専用の強い乱数。既存実装のキー形式を隔離で検証、少なくとも256bitのentropy |
| MYFITNESS_CORS_ALLOWED_ORIGINS | https://app.example.com のみ。カンマ複数可、wildcard禁止 |
| DEBUG / TRACE | false / false。logging環境変数の上書きも検査 |
| SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE | 5（負荷検証後に確定） |
| SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE | 0（idle停止への影響を検証） |
| SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT | 300000（ms、計画値） |
| SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT | 30000（ms、計画値） |

ddl-auto=validate、Flyway自動実行falseを維持する。管理/owner/migrator資格情報をDataSourceに設定しない。
managed NeonではCompose用POSTGRES_ADMIN_*変数は使わない。

## 8. Backend build・起動

root build、Java17固定、OS/JREサポートのimageを固定digestで管理する方式を後続タスクで用意。
現状にDockerfileはないため自動判定任せの本番起動を検証済みとは扱わない。

```sh
# 承認後のbuild環境で。今回実行していない。
./mvnw -B -DskipTests package
# PORTはproviderの非秘密設定。shell展開できる起動方式でのみ有効。
java -XX:MaxRAMPercentage=60.0 -jar target/demo-0.0.1-SNAPSHOT.jar --server.port="${PORT:-8080}"
```

Maven test合格をrelease gateにし、skipTestsをテスト省略の口実にしない。
JAR内にDevToolsが含まれていないこと、Java/cgroup/RAM、listen0.0.0.0、graceful shutdownを確認。
前工程のV4/ACL確認後に起動し、Hibernate validateとDB上のapp接続を確認。

## 9. Frontend環境変数・build・起動

service rootはfrontend、Node22固定。build時：

```text
NEXT_PUBLIC_API_BASE_URL=https://api.example.com
MYFITNESS_FRONTEND_PUBLIC_ORIGIN=https://app.example.com
NEXT_TELEMETRY_DISABLED=1
```

NEXT_PUBLICは公開情報・build埋め込み。password/JWT署名鍵/DB接続情報は一切設定しない。
API URL・HSTS origin変更時は再buildが必要。CI用example.invalidを本番に残さない。

```sh
# 承認後のFrontend build環境で。今回実行していない。
npm ci
npm test
npm run lint
npx tsc --noEmit --incremental false
npm run build
npm run start -- --hostname 0.0.0.0 --port "${PORT:-3000}"
```

standaloneを採用する場合は別のimage/起動コマンドへ変更するため、上記next startと混在させない。
providerのbuild検知で意図せず16.4.5以外にならないようlockfileとnpm ciを使う。

## 10. HTTPS・CORS・proxy

DNSを設定してTLS発行を確認し、HTTP→HTTPS redirect、公開domain、許可Originの完全一致を確認。
FrontendのHSTSは明示hostだけ。includeSubDomains/preloadは付けない。Backend側HSTSもTLS終端・trusted schemeと整合させて確認する。

本番公開前にproxy/IP課題を解消する。現行はforward-headers-strategy:noneとgetRemoteAddrであり、そのままではedgeのIPで全員が同じ制限枠になり得る。
Railway公式にX-Real-IPの供給記載はあるが、本調査では任意入力の上書き保証・全迂回経路の遮断を公式仕様から確定できていない。[Railway header仕様](https://docs.railway.com/networking/public-networking/specs-and-limits)
実装案：信頼するpublic edge経由だけを許可し、providerの上書き済みcanonical IPを検証するresolverを導入。private/TCP/別public URLからの偽装経路は拒否または安全なpeer-IP fallback。
保証が取れなければprovider照会・信頼境界の変更を行い、一般公開を止める。単にforward-headers-strategy=frameworkやXFF先頭採用へ変更しない。

## 11. ヘルスチェック

M5-12ではDBを照会しない `/health/live` と `/health/ready`（`/health` alias）を実装し、隔離検証後、2026-11-09に承認済みの反映を実施。
正確な条件・本番での未検証事項・DB疎通を保証しない制約は[M5-12仕様](M5-12-proxy-health.md)を参照。下記のDB probeは将来案であり今回未実装。

`/health/live` と `/health/ready` はM5-12仕様に従う。readinessへbounded DB probeを追加する場合は、負荷・timeout・情報非公開を別途設計・検証する。
認証なし200の最小応答、未ready503、他のActuator endpointは公開しない。権限・CORS・rate limit対象を回帰試験する。
Railwayのdeployment health成功と継続監視は別物として運用する。[Railway Healthchecks](https://docs.railway.com/deployments/healthchecks)
接続確認を過密に行ってNeonを常時wakeしないよう、監視間隔・readiness用途を区別する。

## 12. ログ・監視・通知

初期閾値案：5xx連続、ready失敗、DB pool枯渇、RAM上限接近、429急増、backup失敗/24時間以上未取得、予算通知。
受信先・対応時間・手動停止判断を決める。providerログの保存期間だけを長期監査と扱わない。
request body、Authorization、JWT、password、JDBC password、個人メールをedge/app/buildログに出さない。
URL/query/例外に機密が混入しないことを本番相当テストと初回公開時に確認。IPの長期保存は目的・期間を限定。

## 13. バックアップ設定

初期目標（ユーザー承認が必要）：日次別保管によるRPO24h、復旧訓練でRTO4h以内を確認。
PITRは直前状態へ戻せる追加手段だが、実際のRPO/RTOを保証する数値として未検証のまま掲示しない。
Neon Launchのrestore windowを明示的に7日へ設定（有料の既定値が1日のため確認）。snapshot scheduleと容量も確認する。
別の障害領域・資格情報で暗号化dumpを保存し、7日次＋4週次＋3月次は保持方針案として容量試算後に確定する。

承認後のbackup job例：PostgreSQL16対応client、direct接続、PGSERVICEFILEとPGPASSFILEはsecret注入のGit外600ファイル、job実行dir700。
サービス定義にhost/db/user/TLSを置き、引数に秘密値を載せない。

```sh
# 新規の専用保存dirを事前に作成・確認。BACKUP_PATHは未使用の絶対パス。
umask 077
test -n "$BACKUP_PATH" && test ! -e "$BACKUP_PATH" || exit 1
pg_dump --dbname=service=myfitness_prod_backup --format=custom --file="$BACKUP_PATH"
# 成功後にサイズ、chmod600、pg_restore --list、SHA256を記録する。
```

上記ファイルの存在確認だけでは競合に対する完全な排他にはならないため、実jobはconcurrency1＋新規専用dir＋一意timestamp/UUID名で実装する。
バックアップ用のread権限・sequence/historyの全読取りを検証。app資格情報をbackupへ流用しない。
providerのreserved globalsは移植しない。別途非秘密のrole属性・membership・owner・ACL manifestとmigration SHAを保管する。
password verifierを含むglobalsを公開artifact/logに出さない。

## 14. リストア検証

月1回と重要migration前に、新しい隔離project/DBへ復元。元DBを上書きしない。
SQL作成のroleを先に再現し、provider予約roleは除外、所有者/ACLを保持して復元する。
`--no-owner --no-acl` で成功させただけでは完全復旧合格としない。採用するなら再付与SQLと比較を別途必須にする。
pg_restore終了0、履歴checksum、件数、PK/FK/unique、sequence値、owner、ACL、appの拒否権限、Hibernate validateを照合。
復元側Secretsは本番から分離する。個人情報を含むbackupは必要最小の担当のみアクセスする。

## 15. 障害対応・切り戻し

1. 新規deploy停止、時刻・commit・migration結果・影響を記録。自動repairや権限追加をしない。
2. migration失敗なら履歴・schemaを読み取り確認。PG transactionがrollbackしたかを確認し、原因修正・承認後に再実行可否を決める。
3. アプリだけの不具合なら、DB互換性を確認して以前artifactへ戻す。DBは自動downgradeしない。
4. データ破損/誤削除/復旧不能なschema損傷の場合のみ、別承認でPITRまたは検証済みdumpから新DBへ復元。
5. 切替中の書込み停止・取りこぼし・RPO以降の損失を説明し、復元先検証後にSecrets/接続先を切替。旧DBは保全する。

Frontendも以前artifactのbuild時API URL・CSP/HSTS・Backend互換性を確認して戻す。
請求上限停止・provider障害時はprovider statusとbudgetを先に確認し、DB復元を最初の手段にしない。

## 16. デプロイ後E2E

新しい本番専用検証アカウントで登録/ログイン/401/ログアウト、種目選択、セットコピー、タイマー、保存・履歴・編集・削除を確認。
同一キー201→200、異内容409、並列POSTは隔離環境を基本とし、本番スモークは承認された専用Sessionだけ。
他ユーザー404、CORSの3ヘッダー、Swagger拒否、rate limit429/Retry-After、秘密情報なし、HTTPS/header/CSP違反を確認。
作成ID・削除対象を記録し、既存ユーザーデータに触れない。使用済み冪等キーは無期限保持の現仕様を維持する。

## 17. 課金設定

[比較文書の試算](M5-11-deployment-comparison.md)を承認し、Railway soft/hard、Neon通知、backup/domain料金を個別設定する。
Neon通知はhard capではない。最大compute・環境数・snapshot保持量も制限し、通知を受けた人が対応できる運用にする。
hard capの停止に伴うサービス停止を許容するか明示承認を得る。アラート配信のテストも行う。

## 18. 公開最終ゲート

[セキュリティチェックリスト](M5-11-security-and-roadmap.md)を全件レビューし、証跡・rollback・復旧訓練・予算・ドメインを確認。
未検証の項目がある状態で一般公開を承認しない。最初は限定betaを選び、登録対象・保持データ・問い合わせ窓口を明示する。
今回作成した文書の承認と、実装/契約/DB作成/migration/deploy/Secrets登録の承認は別である。
