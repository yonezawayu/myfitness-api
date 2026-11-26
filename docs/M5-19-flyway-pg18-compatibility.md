# M5-19：Flyway PostgreSQL 18対応の隔離検証

調査・検証：2026-11-26 JST。採用版：**11.20.3**。

**更新は隔離側だけ。稼働元リポジトリのpom.xml・テストコードへ反映していない。追加承認により独立Git cloneで検証・コミット・Push・PR作成まで行う。**
VS Codeの自動ビルドによる既存BackendのDevTools再起動は許可されていない。
Neon/既存ローカルDB接続、Docker、アプリ手動起動・停止、既存env変更、課金は実施しない。初回隔離検証ではcommit/Push/PRも未実施だったが、今回の追加作業ではGitHubへの保存だけを許可された。PRはマージしない。

## 1. Gitと隔離環境

- origin/developをfetchしてc8e24428fc87c6c0a29c1575b1bc7f13db4355b4を確認。
- その最新refを基点にfeature/flyway-postgresql18-supportを新規作成。以前のHEADとファイル差分がないことを確認してから切り替えた。
- 実リポジトリの既存next-env.d.ts、未追跡AGENTS.mdとM5-18文書2件を保持。保護対象2件のSHAは変化なし。
- 作業場所：`/private/tmp/myfitness-m519-flyway.wdfpyw`。固定refのpom/mvnw/.mvn/src/.githubをgit archiveで展開。
- 既存.env・Neon資格情報・frontend保護対象・node_modulesはコピーしない。
- 隔離環境のみにpom変更・テスト6件・本書を追加。target/JAR/ログ/オフライン検証helperも同じ隔離場所へ保存。
- /private/tmpは永続保管の保証がない。成果物を今回勝手に削除せず、反映時はファイルの存在・内容を再照合する。

### GitHub保存用の独立clone（追加承認分）

- 作業先：`/private/tmp/myfitness-m519-pr.8jIbfd/repo`。GitHubのdevelopから新規cloneし、基点c8e24428を確認。
- ブランチ：`feature/flyway-pg18-pr-isolated`。元featureブランチと競合せず、.git/objects/refsも元リポジトリと共有しない。
- 初回の隔離成果物3件を再利用。本文書だけは追加承認と今回の隔離方式を追記する。
- Maven local repositoryも`/private/tmp/myfitness-m519-pr.8jIbfd/m2`へ分離し、専用の空settingsで公開Central依存だけを解決する。build target・ログは独立clone側に置く。
- 実DB接続環境変数なし、実.envファイルなし、datasourceは未解決placeholderのみであることを確認してからH2テストを実行する。
- 稼働元のpom/VS Code設定・target/classes/test-classesの内容とmtime/ctime・Git差分・保護対象/M5-18文書のSHAを前後比較する。元プロセスは操作しない。
- 今回の独立cloneでもJava17で全175テスト・compile・JAR生成・dependency:tree・plugin説明確認が成功。JAR内Core/PG moduleは11.20.3で、V1〜V4の固定SHA/Flyway checksum回帰検査も成功した。ログはclone外の専用作業場所に保持し、コミットしない。

| 方式 | DevTools/VS Code/targetのリスク | 既存差分と作業量 |
| --- | --- | --- |
| A：元pom更新＋Maven出力分離 | Mavenを分離してもVS Codeが元pomを検知するリスクが残る | 元の未コミット差分に触れる。今回不採用 |
| B：git worktree | 外部worktreeのtargetは分離可能。元VS Code workspaceへ追加しなければ低リスクだが.git管理情報は共有 | 元checkoutは維持できるが共有refs/worktree管理を要する。今回不採用 |
| C：独立clone | 元pom/target/設定もGit管理情報も変更しない。既存Backendの監視対象を更新しない | 新規cloneと専用branchで3件だけをcommit。最も分離できるため採用 |

GitHubに公開しても、稼働元へのpull/checkout/再ビルドは行わない。元リポジトリの依存は10.20.1のままであり、稼働Backendの新Flyway反映を意味しない。

## 2. 現行版の問題と候補比較

実リポジトリのBoot BOM管理値はFlyway10.20.1。PostgreSQLDatabase.ensureSupported()の検証上限は17。
PG18への実接続が必ず例外になるとは限らないが、未検証版警告を無視してNeon18.7へ適用する方針にはしない。

公式リリースノート、固定tagソース、公開Maven Centralメタデータを確認した。
Core/PG module/Maven pluginの11.20.3・13.13.0がCentralに存在することも確認した。

| 候補 | PG18とJava | Boot3.4.12との判断 | 採否 |
| --- | --- | --- | --- |
| 11.14.1 | PG18検証上限の実装を公式tagで確認 | 11系APIの候補だが今回実行試験なし | 11.17.1等の後続修正より前なので選ばない |
| **11.20.3** | PG18検証上限18。Core class major61、実際にJava17で実行成功 | Boot3.4.12のAutoConfiguration/H2 migration/validate/失敗時停止を試験成功 | **採用**。11系の後続修正を含み、12以降のAPI削除を避ける |
| 13.13.0 | 調査時のCentral release。公式資料上Java17系列 | 12以降でcleanOnValidationError APIが削除。Boot3.4.12の固定sourceは当該呼出しを持つ。最新なら無条件に動くとは言えない | Boot改修/更新まで広げず不採用。今回は候補の実行試験なし |

11系でも`cleanOnValidationError=true`は使わない。デフォルトfalseを維持し、失敗時の自動clean/repairを行わない。
新しいメジャーのCLI/provisioner/有料機能を導入せず、従来のSQL migration/validate/API/Maven方式を維持する。
CLI同梱JREの版とMaven/APIに必要なJavaを混同しない。

### セキュリティ確認の範囲

公式リリースノートとflyway/flywayの公開Security Advisories（公開advisoryなし）を確認。
本構成を止める既知の重大問題は今回の確認・テストで見つからなかったが、**脆弱性ゼロや今後の保守を保証するものではない**。
全推移依存の網羅的SCAは未実施。11.20.3は調査時の最新全体版ではなく、Boot連携を優先した固定採用版。
公開前は採用版・Java runtime・推移依存の最新advisoryを再確認する。

## 3. 最小の依存変更

隔離pom.xmlのpropertiesに次だけを追加した。

```xml
<!-- PostgreSQL 18 support; keep Core, database module and Maven plugin aligned. -->
<flyway.version>11.20.3</flyway.version>
```

Boot BOMを同名propertyで上書きする。既存のplugin versionとPG module versionも同じpropertyを参照するため、
Core/PG module/Maven pluginを同時に11.20.3へ揃えられる。個別versionの二重管理やBOM全体更新はしない。

| 要素 | 実リポジトリ（未反映） | 検証済み隔離コピー |
| --- | --- | --- |
| Boot | 3.4.12 | 3.4.12 |
| Flyway Core/PG module/plugin | 10.20.1 | **11.20.3** |
| pgJDBC | 42.7.8 | 42.7.8 |
| Hibernate | 6.6.36.Final | 6.6.36.Final |
| Java target/runtime | 17 / 17.0.17 | 同じ |

JDBC・Hibernateを今回更新する必然性は確認されず、変更しない。アプリmainソース・application.yml・V1〜V4も未変更。
PostgreSQL moduleのServiceLoader検出とCore/PG module双方のMaven versionをテストで確認した。
生成JARのBOOT-INF/libも上表どおり（旧Flyway混在なし）。JARは起動していない。

## 4. Spring Boot連携・起動安全性

- 現行application.ymlの`spring.flyway.enabled=false`と`ddl-auto=validate`は維持。新規テストで実設定を読み、デフォルトでFlyway beanが作成されないことを確認。
- 明示的enabled=trueの専用H2メモリDBだけで、Boot3.4.12 FlywayAutoConfigurationからmigration/validateが成功。
- Hibernate validateがFlyway初期化後に実行できることも、専用test entityとH2 probe tableで確認。
- 不正SQLではFlywayMigrateExceptionによりcontext起動失敗。clean/repair/自動再送は行わない。
- MYFITNESS_DB_*とMaven用FLYWAY_*の設定名・意味は変更なし。実envは読み込まない。
- 手動Maven migrationはアプリのenabled=falseとは独立。pluginの設定はhelp:describeで確認しただけで、実DBへgoalは実行していない。
- 本番DBのV1〜V4やNeon認証/SET ROLEを保証する試験ではない。H2のcreate/validate成功をPG18実機検証の代わりにしない。

## 5. 自動テスト・ビルド結果

追加ファイル：`src/test/java/com/myfitness/api/common/config/FlywayCompatibilityTest.java`。
全データ接続は固定H2メモリURL、又はDB接続なしのファイル解析。既存テストもH2/Mockito/DataJpaTestであることを実行前に確認した。
一部既存HTTPテストは隔離JVMのランダムポートで実行する。既存3000/8080等は使用・停止しない。

追加6件：

1. application設定で自動migration無効・Hibernate validateを維持。
2. 明示有効時のBoot AutoConfigurationとH2 migration/validate成功、target/baseline/clean設定。
3. 不正migrationによる起動停止。
4. Flyway後のHibernate validate成功。
5. PostgreSQL module検出・Coreとのversion一致。
6. V1〜V4のSHA-256/旧Flyway checksum一致・PGパーサー26文のtransaction判定。

`@TestConfiguration`で補助JPA設定が本体component scanへ混入しないようにした。
最初の検証ではJARのImplementation-Versionが未設定でversion検査だけ失敗したため、
Maven埋込みpom.propertiesから取得するテストに修正。ライブラリ自体の不具合ではない。

| 検証 | 結果 |
| --- | --- |
| Maven Central依存解決・dependency:tree | 成功、対象3成果物11.20.3で整合 |
| Java17 compile | 成功 |
| Backend全テスト最終版 | **175成功、失敗0、エラー0、skip0**（既存169＋追加6） |
| Maven plugin help:describe | 成功。plugin dependencies解決・設定項目確認。migrate自体は未実行 |
| package / Boot repackage | 成功。JAR未起動 |
| V1〜V4パーサー | 26文、全transaction判定true |
| 旧10.20.1と新11.20.3のchecksum計算比較 | 全4件一致 |

実行例（**隔離ディレクトリだけで実施済み**、ログへ既存資格情報を渡さない）：

```sh
cd /private/tmp/myfitness-m519-flyway.wdfpyw
JAVA_HOME=/usr/local/opt/openjdk@17 ./mvnw -B -DskipTests compile dependency:tree \
  '-Dincludes=org.flywaydb:*,org.postgresql:postgresql,org.hibernate.orm:hibernate-core'
JAVA_HOME=/usr/local/opt/openjdk@17 ./mvnw -o -B test
JAVA_HOME=/usr/local/opt/openjdk@17 ./mvnw -o -B -DskipTests package
```

既存@MockBeanのdeprecated warning等は残るが、無関係な変更で消していない。

## 6. V1〜V4の固定checksum

| 版 | Flyway CRC（旧版＝新版） | SHA-256（SQL本文不変） |
| --- | --- | --- |
| V1 | 1495888521 | 602f6581b854e23cb66cda3fb5d9cd95325c9f43accea44be017186d697a9e4d |
| V2 | -256376701 | 0b2a08945d19d916a31c4aede77c13f80eb553f56cb9e2036416a6ad0a82b00c |
| V3 | 1159973059 | d611f77d37728318a0333f090608414c61a36210a5005a2edf622b960a96d06f |
| V4 | 257425531 | d23ef6a1f55445c645732e4301bd1a140d53bf72be58d00e3d13a209eb96eecc |

SHAとFlyway CRCは別物。migrationを書き換えたり、checksum差をrepairで隠したりしない。
オフライン比較用ChecksumComparison.javaとchecksums-old/new.txtを隔離場所に保持している。

## 7. 未実施：PostgreSQL 18のサーバー実行

標準Homebrew配置にpostgres/initdb/pg_ctlがなく、既存と独立したPG18実行環境は未確保。
Docker操作・Neon接続・既存DB流用・自動インストールはせず、**PG18実migrate/validate/Hibernate validate/GRANT/SET ROLEは未実施**。
パーサーはサーバーの構文・権限・制約実行を検証するものではない。

次回の費用0円・ローカル案：別途承認後にPG18バイナリを確保し、
専用の新規data directory/socket directory、未使用loopback port（例15438、実行前に要確認）で一時serverを起動する。
brew servicesを使わず、既存5432・Docker・Neon資格情報は一切使わない。
既存DBと区別した架空DB/検証roleを準備し、管理→owner/migrator/app分離、V1〜V4、validate、Hibernate、権限拒否、失敗時rollbackを検証する。
新規バイナリ取得・一時server/DB作成・検証データ・撤収を承認し、対象PID/data directoryを特定するまで実行しない。
PG18.7を同じ版で確保できなければ、major18での試験とNeon18.7実環境試験を区別する。

## 8. 実リポジトリへの反映手順（未実施、再承認が必要）

対象repo：`/Users/takeshiyonezawa/demo/demo`。
検証成功は、稼働Backendの自動再起動を許可するものではない。**今回反映しない**。

1. 次回Git状態・保護対象2件のSHA・M5-18成果物・M5-19隔離成果物を再確認し、追加差分があれば止める。
2. BackendのDevToolsとVS Codeの自動設定更新を再確認する。ユーザーが安全なメンテナンス方法又は反映に付随する自動再起動リスクを明示承認するまでpom/testを反映しない。無断でVS Code設定も変更しない。
3. 承認後、pom.propertiesのflyway.version追加ではなく、**pom.xmlのproperties**へ第3節のflyway.versionを追加する。既存依存・pluginはそのpropertyを参照するので他versionは触らない。
4. 検証済みFlywayCompatibilityTest.javaと本書を、それぞれrepoの同じ相対パスへ追加する。M5-18や保護対象を上書きしない。
5. 差分はpom.xml／追加テスト／M5-19文書の3件だけであることを確認し、checksumとdiff --checkを確認する。commit/Push/PRは別承認。
6. 検証コードから変更がなければ隔離結果を再利用できるが、ref/依存/設定に差があれば新しい隔離コピーで全テスト・ビルドを再実行する。稼働repoのtargetを無断で更新しない。

反映用の差分は隔離pomと元repoのpomを比較して確認できる。生成済みJARやtarget/log/helperは反映対象外。
不要な全面置換をせず、他の作業差分に同じ変更がないか確認した上で適用する。

## 9. Neonへ進むための別承認ゲート

1. 依存反映の承認と独立PG18サーバー検証。
2. Free維持・残量・Direct/verify-full・対象branch/neondbを再確認した上で、M5-18のPhase A読み取り棚卸し。
3. **別承認**でowner/migrator/appのNOLOGIN作成・限定ACL/default ACL。DB/publicの所有者やNeon管理roleは変更しない。
4. 専用資格情報・LOGIN/SET ROLE可否の別承認。初期neondb_ownerをappやFlyway常用に流用しない。
5. **さらに別承認**で、固定版の隔離checkoutからmigrator/direct、initSql=SET ROLE myfitness_owner、target4、baselineOnMigrate=false、cleanDisabled=trueでinfo→migrate1回→validate。
6. schema/owner/ACL/history/checksum確認後にのみ、別承認でmyfitness_appによるHibernate validate/HTTP試験へ進む。

本書にNeon実接続コマンド・秘密値は含めない。migration失敗時は自動再実行・repair・clean・履歴修正をせず状態保全して停止。
Railwayプロキシ境界・公開環境E2E・backup/restore等の公開ブロッカーは別途残る。
アプリのFlyway無効設定を維持するため、依存更新の反映だけでmigrationを開始する設計ではない。

## 10. 公式根拠（2026-11-26確認）

- [Flyway release notes](https://documentation.red-gate.com/flyway/release-notes-and-older-versions/release-notes-for-flyway-engine)
- [Flyway PG対応](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database)
- [11.20.3 PG18の固定tag](https://raw.githubusercontent.com/flyway/flyway/flyway-11.20.3/flyway-database/flyway-database-postgresql/src/main/java/org/flywaydb/database/postgresql/PostgreSQLDatabase.java)
- [11.14.1の固定tag](https://raw.githubusercontent.com/flyway/flyway/flyway-11.14.1/flyway-database/flyway-database-postgresql/src/main/java/org/flywaydb/database/postgresql/PostgreSQLDatabase.java)
- [Maven Central Core 11.20.3 POM](https://repo.maven.apache.org/maven2/org/flywaydb/flyway-core/11.20.3/flyway-core-11.20.3.pom)
- [Maven Central PG module 11.20.3 POM](https://repo.maven.apache.org/maven2/org/flywaydb/flyway-database-postgresql/11.20.3/flyway-database-postgresql-11.20.3.pom)
- [Maven Central plugin 11.20.3 POM](https://repo.maven.apache.org/maven2/org/flywaydb/flyway-maven-plugin/11.20.3/flyway-maven-plugin-11.20.3.pom)
- [BootのFlyway11連携注意点](https://github.com/spring-projects/spring-boot/issues/43770)
- [BootとFlyway12以降のAPI削除](https://github.com/spring-projects/spring-boot/issues/49038)
- [Flyway Security Advisories](https://github.com/flyway/flyway/security/advisories)
