# M5-16 Railway＋Neon staging実行計画・承認ゲート

作成・公式情報再確認：2026-11-12（JST）。基点：PR #56マージ `70cf26bedb5689a5cf90c5d26b97ff0b1e6b5fe8`。
文書PR前の最終レビュー：2026-11-14（JST）。公式の料金・プロキシ仕様・課金制御を再確認。以下の費用は使用量を仮定した試算であり、確定料金ではない。
**準備資料のみ。以下のサービス操作・SQL・Flyway・デプロイ・問い合わせ送信は一切実行していない。コマンドは未検証の実行候補であり、各ゲートの承認前に実行しない。**

## 1. 現状・既存資料レビュー

- 作業ブランチを変えず、非チェックアウトのdevelopをfast-forward fetchで上記commitへ同期。develop/originは0/0。
- 保護対象 `frontend/next-env.d.ts` と `frontend/AGENTS.md` は変更・stageしない。M5-14環境/成果物も停止・削除しない。
- M5-15の114テスト・JAR生成、M5-14のユーザーChrome＋HTTPSモックE2Eは過去の証跡。今回はテストを再実行せず、実Backend＋Neonの合格へ読み替えない。

| 参照資料 | 現在の採用方針と差異 |
| --- | --- |
| [M5-11構成比較](M5-11-deployment-comparison.md) | 比較当時のhealth未実装・RAM/pool/費用は履歴。本書のstaging条件へ自動流用しない |
| [M5-11 Runbook](M5-11-production-runbook.md) | 空DB・role分離・direct migration・復旧原則を維持。pool最大5案ではなく初期3案を今回評価 |
| [M5-11 roadmap](M5-11-security-and-roadmap.md) | 部分CSP/反映待ち等は当時の棚卸し。現状はM5-13/14を優先。公開ゲートは継続 |
| [M5-12 proxy/health](M5-12-proxy-health.md) | 現コードと整合。none戦略、右端XFF、CIDR信頼。readyはDB疎通なし |
| [M5-13 CSP](M5-13-frontend-security.md) | nonce・動的SSR/no-store・localStorage/Bearer維持。実環境での再検証は必要 |
| [M5-14 E2E](M5-14-production-e2e.md) | ユーザー手動・HTTPSモックの主要操作成功。実DB/Backendは対象外 |
| [M5-15 readiness](M5-15-railway-neon-readiness.md) | IP・Neonの未確定事項を引継ぎ。ここから実行単位と期待結果を具体化 |
| [英文問い合わせ案](M5-15-railway-support-inquiry.md) | 必要な7質問と信頼境界を確認済み。変更不要・未送信 |

公開ブロッカー：Railwayのpeer/header信頼境界、Neon最小権限/TLS/初期migration、復旧試験、本番起動/監視、実Backend＋DB E2E。
本書の承認は操作承認を兼ねない。一般公開・staging作成・契約・課金は未承認。今回の変更はこのMarkdown新規1件のみ。

## 2. Railwayの信頼境界：判定表

[公式Public Networking仕様](https://docs.railway.com/networking/public-networking/specs-and-limits)から確認できるのは、X-Real-IPのclient remote IPという説明、X-Forwarded-Proto=https、元Host情報、公開edgeのTLS対応等。
今回確認した資料では、callerの同名header除去/正規化の保証、XFFの構築規則、安定したingress CIDR、socket peerの保証、private/TCP経路の迂回遮断を確定できない。プラットフォーム全体に仕様が存在しないと断定するものではない。

| 確認事項 | サポートへ求める回答 | 将来の実測と合格条件 |
| --- | --- | --- |
| X-Real-IP | 全public経路で上書き/単一正規値化されるか | 任意値・重複・大小文字・不正値でも選択IPを偽装できない |
| XFF | replace/append/reject、hop順、IPv4/IPv6 | 自己申告prefixを採用せず、利用者実IPへ正規化される |
| socket peer | IP/CIDR/認証可能な接続元の安定性・変更通知 | deploy/時刻/複数経路で境界が維持される。観測だけで恒久CIDRとしない |
| 迂回 | internal/TCP/別host直アクセスの遮断方法 | 未信頼経路は到達不可、または転送値を無視/拒否する |
| Spring Boot | 推奨方式とscheme/hostの信頼設定 | forward-headers-strategy=noneの現resolverと両立するか、別設計が必要か確定 |

### 検証データと成功条件

- 同意を得た2つの外部回線（別public IP）・IPv4/可能ならIPv6を利用。実IPをGit/公開文書へ書かない。診断ログは必要IP情報のみ短期保管し、Authorization/Cookie/password/本文は収集しない。
- 正規利用者A/Bを別枠で識別。同じAは通常/encoded URL/正規proxyの変更でも同じ枠。元から同じNATのA/Bが同一IP枠になる点は現仕様であり、edgeのせいで全員同一枠になる問題と区別する。
- loginは11回目、登録は6回目が429、Retry-After>0、no-store。300秒後に回復し、Bの枠は影響を受けない。試験は専用アカウント/診断stubだけで実施する。
- XFF先頭/末尾、X-Real-IP、Forwarded、重複/不正値を変えてもAの429を解除できない。不正URLはControllerへ到達しない。不正転送値は、信頼境界内で400、未信頼境界では無視または到達拒否。
- CORS/OPTIONS、保護API401、既存認証契約を維持。CIDRの空/不正設定はM5-12の安全側動作を維持。
- 公式保証と上記実測の両方が揃い、対象domain/private/TCP/CDN経路の範囲が明示されて初めて合格。未確認経路が残れば一般公開不可。

失敗時は認証endpointを公開しない。広いprivate CIDRや/0、XFF先頭、無条件X-Real-IP信頼、framework/nativeによる事前remoteAddr書換えで強行しない。
最初の診断はDB/アプリ資格情報を持たない別serviceで行う。app接続前に、staff以外からの到達制限を実証する。推測困難なURLやCORSだけをアクセス制限にしない。

### 代替案（実装せず比較のみ）

| 案 | 安全性・制約 | 工数の推定 | 月額の推定 |
| --- | --- | --- | --- |
| 専用proxyをRailwayに追加 | 自前proxy→Backendの境界は作れるが、前段Railway edge→proxyのclient IP保証は残る。追加するだけでは解決しない | 2～4人日＋provider確認 | RAM0.125GB/平均CPU0.005常駐なら約$1.35追加。実測前・既存floor内なら請求差なしもあり |
| IP＋アカウント軸の併用 | credential攻撃の補助。登録乱用、アカウントへのDoS、IP由来不明は解消せず、単独代替ではない | 1～3人日、共有化は追加 | process内なら追加infra $0候補。共有storeは未選定・別見積り |
| 直接公開するVPS/Caddy＋非公開Backend | 自分でsocket境界/許可portを固定できる。CDNなし、Backend直portを閉じる。OS/TLS/監視の保守責任が増える | 3～7人日＋継続保守 | 参考：さくら石狩4GB月払い3,740円＋Neon/backup。初回支払・最低期間は別確認 |

VPS例の根拠：[公式プラン案内](https://vps.sakura.ad.jp/functions/scaleup/)。年払いの月額換算と月払いは混同しない。工数は作業計画用の推定で保証ではない。
Railwayで保証が得られなければ、費用・保守担当の再承認を得てVPS案を検討する。今のJWT保存方式や認証APIは勝手に変更しない。

## 3. staging構成案・決定項目

```text
承認されたテスター（架空データのみ）
  ├─ HTTPS <STG_FRONT_ORIGIN> → Railway Frontend / Node22 / 1 replica
  │        build時NEXT_PUBLIC_API_BASE_URL=<STG_API_ORIGIN>
  └─ HTTPS <STG_API_ORIGIN> → 信頼境界を検証したedge → Backend / Java17 / 1 replica
                       app資格情報のみ、TLS/JDBC direct、小pool
                                     ↓
                 専用Neon project / PG16 / myfitness_staging（空から）
                   provider管理者 / owner(NOLOGIN) / migrator / app
                         ├─ history/snapshot
                         └─ direct pg_dump → 暗号化別保管 → 別DBでrestore

専用migration jobのみ → direct接続・SET ROLE owner・V1～V4
DBなし診断serviceはIP確認後に撤収。production/ローカル環境とは別resource/Secret。
```

- 第一候補：Railway Hobbyの専用staging project、Neon専用projectのLaunch。復旧を含む評価なのでFreeの短い履歴窓だけで代用しない。契約前に予算承認。
- 地域候補：Railway Singapore `asia-southeast1-eqsg3a`、Neon AWS Singapore `aws-ap-southeast-1`。同じ都市でも同じprivate networkではない。[Railway regions](https://docs.railway.com/deployments/regions)、[Neon regions](https://neon.com/docs/introduction/regions)
- staff限定到達方式・MFA・操作担当・予算/期限・削除承認者が決まるまで作成しない。productionと同じDB、JWT鍵、Secret参照、Git自動deployを共有しない。
- role名はV4と整合するowner/migrator/appを専用project内で使用。ローカルdump・実ユーザー・Sessionは移行しない。個人情報を使わず架空アカウントのみ。
- JWT鍵・DB passwordは環境専用乱数。Secret UI/承認jobから注入。CLI引数やURLにpasswordを含めない。実envファイルはコピーしない。
- Backend：repo root、`RAILPACK_JDK_VERSION=17`、prod、DEBUG/TRACE=false、forward strategy none、Hikari max3/minIdle0/idle60秒/keepalive0を仮案として実測。migrator/admin Secretを渡さない。
- Frontend：service root frontend、`RAILPACK_NODE_VERSION=22`、lockfile＋npm ci、API/HSTS用のHTTPS Originをbuild前に明示。nonce CSPは緩和しない。
- deploy healthは/health/ready、生存監視は/health/live。readyはDBを確認しないためDB障害通知を別途準備。公開管理endpointを増やさない。
- logはINFO、秘密値・個人メール・認証本文を非出力。5xx/429急増、pool枯渇、OOM、backup失敗、費用通知の受信テストを行う。Neonを起こし続ける監視SQLは避ける。

承認後に設定する変数案（値は未設定、既存envを流用しない）：

| 対象 | 変数・候補値 | 検証/注意 |
| --- | --- | --- |
| Backend | `SPRING_PROFILES_ACTIVE=prod`、`MYFITNESS_DB_URL=<VERIFY_FULL_DIRECT_JDBC_URL>`、`MYFITNESS_DB_USERNAME=myfitness_app`、`MYFITNESS_DB_PASSWORD=<STAGING_SECRET>`、`MYFITNESS_JWT_SECRET=<STAGING_SECRET>` | admin/migrator資格情報を渡さない。JWT鍵は既存方式に適した十分な乱数長を確認 |
| Backend | `MYFITNESS_CORS_ALLOWED_ORIGINS=<STG_FRONT_HTTPS_ORIGIN>`、`MYFITNESS_TRUSTED_PROXY_CIDRS=<VERIFIED_PROXY_CIDRS>` | CIDR未確定なら空のまま診断し、認証APIを一般公開しない |
| Backend | `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3`、`SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0`、`SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=60000`、`SPRING_DATASOURCE_HIKARI_KEEPALIVE_TIME=0` | 初期評価案。DB容量・cold start・接続数を実測して承認する |
| migration runnerのみ | `FLYWAY_URL=<VERIFY_FULL_DIRECT_JDBC_URL>`、`FLYWAY_USER=myfitness_migrator`、`FLYWAY_PASSWORD=<STAGING_SECRET>` | アプリ自動migrationは既存設定のdisabledを維持 |
| Frontend | `NEXT_PUBLIC_API_BASE_URL=<STG_API_HTTPS_ORIGIN>`、`MYFITNESS_FRONTEND_PUBLIC_ORIGIN=<STG_FRONT_HTTPS_ORIGIN>` | build前に設定し、start時も整合。NEXT_PUBLICへ秘密値を置かない |
| 各service | Railwayの`PORT`、`RAILPACK_JDK_VERSION=17` / `RAILPACK_NODE_VERSION=22` | PORTは下記start引数へ明示。runtime/lockfileを起動ログで確認 |

[Railpack Java](https://railpack.com/languages/java/)、[Railpack Node](https://railpack.com/languages/node/)の指定方法を参照。service root/build検出と実際のruntimeはRailway実環境では未検証。

## 4. Neon検証：SQL・コマンドと期待結果（すべて未実行）

事前にproject/branch/endpoint/region/PG major/DB名/commitを二者確認し、別環境を指したら停止。
psqlは `-X -v ON_ERROR_STOP=1`、接続はGit外600のPGSERVICEFILE/PGPASSFILE（親700）を使用する計画。下記service名は未作成のplaceholder。set -x、Maven -X、秘密値のechoは禁止。

### N0：バージョン・TLS・接続先

```sh
psql -X -v ON_ERROR_STOP=1 'service=myfitness_stg_admin'
```

```sql
SELECT current_database(), session_user, current_user,
       current_setting('server_version_num')::int AS server_version_num;
SELECT ssl, version, cipher FROM pg_stat_ssl WHERE pid=pg_backend_pid();
```

作成時にPG16を選択できること、version_numが160000以上170000未満であることを確認。最新の全選択肢は未確認のため、別majorに自動変更しない。
管理用初期DB名はproviderの値をmanifestと照合し、後工程のtargetはmyfitness_stagingに固定する。
libpqは `sslmode=verify-full`＋信頼CA、JDBC案は以下。CA/hostname不一致は接続失敗が期待値。証明書検証を弱めて進めない。

```text
jdbc:postgresql://<STG_DIRECT_HOST>:5432/myfitness_staging?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory
```

`pg_stat_ssl.ssl=true`だけではCA/hostname検証の証明にならない。別の試験用接続で不正CA・不一致hostnameを与え、SQL実行前に失敗することも確認する。
[Neon TLS](https://neon.com/docs/connect/connect-securely)、[pgJDBC TLS](https://jdbc.postgresql.org/documentation/ssl/)。

### N1：roleと空DB（provider管理者、Neon固有権限は未検証）

既存同名role/DBがあれば停止。provider管理者はSUPERUSERではない。以下は専用projectだけの計画。`<NEON_ADMIN_ROLE>`は実際の識別子へ安全に置換する。

```sql
BEGIN;
CREATE ROLE myfitness_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE myfitness_migrator NOLOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE myfitness_app NOLOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
GRANT myfitness_owner TO myfitness_migrator WITH INHERIT FALSE;
GRANT myfitness_owner TO myfitness_migrator WITH SET TRUE;
GRANT myfitness_owner TO myfitness_migrator WITH ADMIN FALSE;
GRANT myfitness_owner TO "<NEON_ADMIN_ROLE>" WITH SET TRUE;
COMMIT;
-- 別ゲート：上記成功後、transaction外で実行する計画
CREATE DATABASE myfitness_staging OWNER myfitness_owner;
```

role作成/membership/DB所有者の権限が拒否された場合は停止し、Neonの管理者権限・サポート仕様を再確認。appに管理membershipを追加しない。
パスワードは対話 `\password` 等の安全な方法で個別設定し、ログ出力条件を確認後、app/migratorだけLOGINへ変更する。ownerはNOLOGINを維持する。別TCP接続で正password成功・誤password拒否を確認する。

myfitness_stagingに接続し直し、以降の変更前に次のguardを実行する計画：

```sql
DO $$ BEGIN
  IF current_database() <> 'myfitness_staging' THEN
    RAISE EXCEPTION 'Unexpected target database';
  END IF;
END $$;
BEGIN;
ALTER SCHEMA public OWNER TO myfitness_owner;
REVOKE ALL ON DATABASE myfitness_staging FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT CONNECT ON DATABASE myfitness_staging TO myfitness_app, myfitness_migrator;
GRANT USAGE ON SCHEMA public TO myfitness_app;
COMMIT;
```

guardはproject/host識別の代替ではない。adminの継続接続権を別接続で確認してから管理接続を閉じる。
rolesはSQL作成を基本とし、neon_superuser等への意図しない所属を確認。provider予約role/DBを変更しない。[Neon roles](https://neon.com/docs/manage/roles)、[DB所有権](https://neon.com/docs/manage/databases)、[PG16 GRANT](https://www.postgresql.org/docs/16/sql-grant.html)

### N2：direct接続・SET ROLE・V1～V4

migratorのdirect接続で、session_user=migrator→SET ROLE後current_user=ownerを確認する。session SETを必要とするFlyway/backupにtransaction poolerを使わない。

```sql
SELECT current_database(), session_user, current_user;
SET ROLE myfitness_owner;
SELECT session_user, current_user;
RESET ROLE;
```

固定commitの隔離runner（root）へFLYWAY_URL/USER/PASSWORDをSecret注入。まず接続先・空schema・4ファイルSHA・role存在・許可済み対象を確認し、**1回ずつ**実行する案：

```sh
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:info
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:migrate
./mvnw -B -Dflyway.initSql='SET ROLE myfitness_owner' -Dflyway.target=4 flyway:validate
```

V1～V4成功4件/失敗0。V3は所有者NULLを拒否し、V4はcurrent_user=ownerとapp存在が必須。空DBなのでbackfillは不要という静的見込み。migration/historyのownerもownerであることを確認する。
業務table11＋history1、業務identity sequence11が想定。provider管理objectは別manifest。users/session/exercise/set/creationは初期0件。差異があれば停止し、DROP/repair/cleanで帳尻を合わせない。

### N3：属性・membership・ACL検査

```sql
SELECT rolname, rolcanlogin, rolsuper, rolinherit, rolcreatedb, rolcreaterole,
       rolreplication, rolbypassrls
FROM pg_roles WHERE rolname IN ('myfitness_owner','myfitness_migrator','myfitness_app');
SELECT granted.rolname AS granted_role, member.rolname AS member_role,
       m.admin_option, m.inherit_option, m.set_option
FROM pg_auth_members m JOIN pg_roles granted ON granted.oid=m.roleid
JOIN pg_roles member ON member.oid=m.member
WHERE member.rolname IN ('myfitness_app','myfitness_migrator');
SELECT c.relname,c.relkind,pg_get_userbyid(c.relowner) AS owner,c.relacl
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname='public' AND c.relkind IN ('r','S') ORDER BY c.relname;
SELECT pg_has_role('myfitness_app','myfitness_owner','MEMBER') AS app_is_owner_member,
       has_schema_privilege('myfitness_app','public','CREATE') AS app_schema_create,
       has_database_privilege('myfitness_app','myfitness_staging','CREATE') AS app_db_create,
       has_database_privilege('myfitness_app','myfitness_staging','TEMP') AS app_temp;
```

appは管理属性なし・owner/admin/neon_superuserへの昇格なし。users SELECT/INSERT、training3表CRUD、creation SELECT/INSERT、対応5sequence USAGE/SELECTのみ。PUBLIC/default ACLを含め全実効権限を検査し、V4だけで全初期ACLが安全になると扱わない。
旧API/historyの不要権限、users UPDATE/DELETE、creation UPDATE/DELETE、sequence UPDATE、DB/schema CREATE/TEMPは拒否する。

app接続で、別transactionごとに次のnegative testを行う計画。期待SQLSTATEは42501。成功してしまった場合も必ずROLLBACKし、後続停止する。既存/production DBでは実行しない。

```sql
BEGIN;
CREATE TABLE public.m516_probe(id int);
ROLLBACK;
-- 以下も1文ずつ独立したBEGIN/ROLLBACKで確認する
UPDATE public.users SET email=email WHERE false;
DELETE FROM public.foods WHERE false;
UPDATE public.flyway_schema_history SET success=success WHERE false;
DELETE FROM public.training_session_creations WHERE false;
SET ROLE myfitness_owner;
```

このblockは一括実行スクリプトではなく、個別の試験文一覧。各文を独立接続のBEGIN内で試し、成功時はROLLBACK、期待した権限エラー時もROLLBACKまたは接続終了によるrollbackを確認する。ON_ERROR_STOPで終了した場合に同じ接続で後続ROLLBACKが実行されるとは仮定しない。エラーを捕捉するrunnerは今後作成・レビューし、同じrunnerで後続DDLを自動継続しない。

### N4：app・pooled接続・Hibernate

app/directの最小poolで起動しHibernate validateを確認。Neon側でアプリ接続のusenameがmyfitness_appであることを確認する。appからDDLや自動Flywayが実行されないことも確認。
pooled endpointは別試験としてappだけで比較する。JPAのCRUD・transaction advisory lock・並列POST・pool枯渇/復帰を試験し、SET ROLEのsession継続を前提にしない。未合格ならdirectのまま、poolerへ勝手に変更しない。[pooling制約](https://neon.com/docs/connect/connection-pooling)

### N5：backup・restore・失敗時

専用接続とownerへのSET権限を使い、Git外新規dir700/file600で取得。既存archiveを上書きしない。以下は未実行のplaceholder：

```sh
pg_dump --dbname=service=myfitness_stg_migrator --role=myfitness_owner --format=custom --file='<NEW_ARCHIVE_PATH>'
pg_restore --list '<NEW_ARCHIVE_PATH>'
shasum -a 256 '<NEW_ARCHIVE_PATH>'
# 別の空DB myfitness_staging_restore、role/owner準備・接続先確認後だけ
pg_restore --dbname=service=myfitness_stg_restore --role=myfitness_owner --exit-on-error --single-transaction '<NEW_ARCHIVE_PATH>'
```

DBごとのrole定義/membership/ACL manifestを別保管し、providerのreserved globalsは移植しない。restore先を元DBにしない。件数・値・FK/unique・sequence・owner・ACL・history/checksum・Hibernate・app拒否テストを比較する。
PITRも専用test branch/別DBで確認。Launchの履歴は既定1日・最大7日なので明示設定する計画。snapshotと別保管dumpは保持量/復旧目標に合わせて承認する。[backup](https://neon.com/docs/manage/backup-pg-dump)、[history](https://neon.com/docs/postgres/backup-restore/history-window)
migration失敗時は確定/ROLLBACK状態・history・schemaを保全し停止。再実行/repair/clean/手動DROP/自動復元はしない。テストのみの空stagingでも、再作成は対象と損失を確認して別承認を得る。

## 5. staging費用と撤収予算

2026-11-12調査、2026-11-14公式再確認。USD、税/為替手数料/domain/外部dump保存先は別。為替は未取得で$1=150円を仮定。
[Railway価格](https://railway.com/pricing)：Hobby最低$5（$5利用分込み）、通常service RAM $0.00000386/GB秒・CPU $0.00000772/vCPU秒・egress $0.05/GB。
[Railwayプラン](https://docs.railway.com/pricing/plans)：service buildのCPU/RAM/image等は課金なし。別のVM/Agent、外部CI、buildで起動した一時serviceは同じ扱いと推定しない。build容量/時間制限は別。
[Neon価格](https://neon.com/pricing)：Launch固定/最低額なし、$0.106/CU時、DB $0.35/GB月、history $0.20/GB月、snapshot/定期backup $0.09/GB月。Freeのquotaを有料利用から控除しない。

計算式：Railway=max(5,RAM×秒×単価＋平均CPU×秒×単価＋egress)、Neon=CU時×0.106＋DB×0.35＋history×0.20＋backup×0.09。
以下は人数からの保証値ではなく使用量モデル。Railway新規Hobbyの1課金周期を想定。既存共有workspaceなら増分と全workspace消費を別に算定する。

| 条件 | A：短期（検証時のみ） | B：常時（月30日） |
| --- | --- | --- |
| Railway稼働 | 48時間、合計平均RAM1GB/CPU0.05、egress1GB | 720時間、合計平均RAM1GB/CPU0.05、egress5GB |
| Railway実消費試算→請求 | 約$0.78→最低$5 | 約$11.26 |
| Neon compute | 主/restore合計4CU時（0.25CUで16時間相当） | 主180CU時＋restore0.5CU時 |
| DB/history/backup平均保持量 | 0.5/0.1/0.5GBを最大1か月保管する保守的モデル | 同左 |
| Neon DB＋backup合計 | 約$0.66（うちbackup $0.045） | 約$19.37（うちbackup $0.045） |
| 合計 | 約$5.66＝約850円/周期 | 約$30.63＝約4,600円/月 |
| 計画用予備込み予算案 | $15≒2,250円/周期以内で承認を求める | $45≒6,750円/月以内で承認を求める |

請求制度上の下限はRailway $5＋Neon実消費であり、全構成を常時$5で運用できる意味ではない。実負荷・再build後の二重deploy・Neon起床・診断service追加等で変動する。
停止中もHobby契約料、volume/bucket、Neon DB/history/snapshot、外部backup保管は残り得る。Neon scale-to-zeroはcompute停止でありstorage削除ではない。Aの48時間を超えたら無断延長しない。

### 通知と強制上限を分ける

- [Railway cost control](https://docs.railway.com/pricing/cost-control)：soft通知とCompute hard limitは別。Aは通知$5/hard $10、Bは通知$12/hard $20を候補に、最低設定額・含まれる費目をUIで確認してから承認する。許容予算内の設定ができなければ作成しない。hard limitは同workspace全停止の可能性があるため既存共有workspaceでは無断設定禁止。
- [Neon通知](https://neon.com/docs/introduction/spending-notifications)：A閾値$2、B $20を候補。80/100%・15分周期の通知であり、総ドル額の強制停止ではない。per-project compute/storage/transfer quota、max CU、branch数、保持量を別に設定・試験する。quotaに到達してもstorage等の全請求停止を保証しない。
- Aは日次に加え各検証終了時、Bは毎日担当が請求を確認。予算50%で延長レビュー、80%または予測超過・通知不達・担当不在で新規build/試験を中断し撤収判断。請求遅延分を見込み、総予算をhard capで保証したとは言わない。
- Railway/Neonそれぞれの権限・最低額・対象費目が未確認のまま「上限設定済み」にしない。無料trialの一回限りcreditを予算へ算入しない。

### 撤収と削除後請求

撤収対象のworkspace/project/environment/service/branch/DB IDとownerを記録し、production・ローカル/M5-14ではないことを二者確認する。停止/削除の明示承認を先に得る。
自動deploy/preview/cronを止め、証跡と必要なtest backupを保全→staging deployment/診断service停止→接続0→不要resource削除→Secret/token失効→請求/残resource確認の順に行う計画。
Neon computeを止めるだけではDB/storage/historyを残す。restore branch・snapshot・外部bucket等の保持/削除を個別判断する。必要backupを復旧確認前に消さない。
全project削除・plan取消は他環境に影響し得るため、専用対象と承認がなければ実行しない。Railway取消はworkspaceの全deploymentを停止し、既発生usageの後払いは残る。[課金FAQ](https://docs.railway.com/pricing/faqs)
削除した瞬間の費用ゼロ、当月plan料の日割返金、請求書未到着＝無料とは扱わない。翌課金確認まで担当を残し、cloudに何も残さない場合も外部backup保管費・失効漏れ・別契約domain更新を点検する。

## 6. Phase別実行ゲート（今回は全て操作未実施）

| Phase | 実行内容/操作案 | 前提・成功条件 | 失敗時 | 費用枠/必要承認 |
| --- | --- | --- | --- | --- |
| 1 仕様確認 | M5-15英文案を承認窓口へ送信し回答を保存 | 上書き/peer/迂回経路のsupported保証。回答だけで実測合格としない | 追加照会/代替案へ。ヘッダー信頼を変更しない | 閲覧無料、support plan要否未確認。送信・有料supportは個別承認 |
| 2 決定 | Hobby/Launch、SG、担当、期間、予算、制限値を確定 | 料金画面と試算照合、データ地域・到達制限・撤収を承認 | 条件不足なら契約しない | A $15/B $45案。契約/課金/MFA/Secret設定の承認 |
| 3 空環境 | 専用project/環境/空DB、DBなしdiagnosticを作成 | ID manifest、既存resourceと分離。正規peer/headerとstaff制限を診断で実証 | appはdeployせず停止。失敗resourceを無断削除しない | 各社従量開始。作成とdiagnostic deployの明示承認 |
| 4 Neon | N0～N4、role/TLS/Flyway/ACL/Hibernate事前確認 | PG16、検証TLS、owner/SET ROLE、history4成功、拒否テスト | 状態保全、再実行/追加GRANT禁止 | DB compute/保管は総予算内。SQL/migration/staging app起動の承認 |
| 5 app deploy | CI済み同一SHAからBackend→Frontend | Phase3の境界実証、Phase4合格、API Origin/CSP/PORT/ready正しい | 後続停止。DB互換性確認後の旧artifact切戻しも承認 | app RAM/CPU課金、service buildは現仕様で無料。deploy/Secret設定承認 |
| 6 実E2E | HTTPS/CORS、登録/login/401、IP/429、nonce、筋トレCRUD | テストデータのみ、2回線分離/偽装不可、操作/Console合格 | 一般公開せず、原因/範囲/修正案を報告 | 同総予算内。専用データ操作と必要な後片付けの承認 |
| 7 復旧 | N5、暗号化dump、別DB restore、PITR | 件数/値/owner/ACL/constraints/checksum/Hibernate一致、RPO/RTO記録 | 元DB保全、restore上書き禁止 | restore compute/backup保管を計上。別DB/branch作成・復旧操作承認 |
| 8 運用/撤収 | log/通知受信/予算/残resourceを確認 | 期限内・超過見込みなし、継続/撤収が明示決定 | 運用担当へ停止判断を依頼、無断延長なし | A48時間/B月末に再承認。停止/削除/plan取消/backup保持の承認 |

Phase4のHibernateは隔離migration確認用runnerのstaging DB接続で行い、一般公開serviceではない。Phase3でpeer境界を実証できない限りPhase5へ進まない。
build/start案（**今回未実行**、staging Secret注入済みの承認runner/serviceのみ）：

```sh
# repo root / Java17
./mvnw -B -DskipTests package
java -XX:MaxRAMPercentage=60.0 -jar target/demo-0.0.1-SNAPSHOT.jar --server.address=0.0.0.0 --server.port="${PORT:-8080}"
# frontend / Node22。API/CSP用HTTPS Originをbuild前に設定
(
  cd frontend
  npm ci && npm run build && npm run start -- --hostname 0.0.0.0 --port "${PORT:-3000}"
)
```

## 7. 最初にユーザーへ求める決定

1. Phase1の問い合わせ送信先・送信担当・送信承認（今回未送信、文案変更なし）。
2. 回答が得られた後、A/Bどちらか、USD予算、SG、期限、担当、通知先・不在時対応・停止許容を決定。
3. diagnosticと空Neon staging作成、Secret登録、SQL/Flyway、deploy、test data、restoreをPhase別に承認する。
4. 撤収対象・保持するbackup/証跡・期限・削除承認者を決める。既存/M5-14環境の撤収は別件。

本書だけを根拠に問い合わせ・契約・staging・DB・デプロイを実行しない。アプリコード/既存env/保護対象は変更しない。計画作成時はコミット・Push・PR作成も対象外であり、今回は別途承認された文書1件のGitHub保存のみを行う。
