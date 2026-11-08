# M5-11 本番構成比較・費用・実現性

調査日：2026-11-08。対象：develop / `c9a928ab54cbaf615af29bfe0f117131b9aa9b5a`（PR #52）。
この文書は計画であり、契約・サービス作成・デプロイ・DB接続・migrationは実施していない。

関連文書：

- [本番DB・デプロイ・復旧手順](M5-11-production-runbook.md)
- [公開前チェックリスト・ロードマップ](M5-11-security-and-roadmap.md)

## 1. 結論と公開条件

第一候補は **Railway Hobby（Backend / Frontend各1サービス）＋Neon Launch（PostgreSQL 16）**。
双方Singaporeを選び、アプリのローカルディスクには永続データを置かない。
ユーザーデータ保全を最優先し、有料の復旧履歴と別保管バックアップを組み合わせる。
最安の無料構成ではなく、管理負荷とデータ復旧のバランスによる推奨である。

ただし、現状のコードをそのまま公開できるという判定ではない。
特にプロキシ越しのクライアントIP判定、ヘルスチェック、managed DB上の権限・TLS検証、XSS対策の公開判断が未完了。
これらの実装・隔離検証を終えてから、別承認で契約・ステージング構築へ進む。

## 2. 現在のアプリ：確認できた事実

| 対象 | コード・設定から確認した内容 |
| --- | --- |
| Backend | Spring Boot 3.4.12、Java 17、Maven Wrapper、実行可能JARの構成。Spring Web / JPA / Security / JWT |
| ビルド・起動 | ルートで `./mvnw -B -DskipTests package`、生成予定JAR `target/demo-0.0.1-SNAPSHOT.jar` を `java -jar` で起動。今回は未ビルド |
| Frontend | Next.js 16.4.5（lockfile）、React / react-dom 18.3.1、Node.js 22をCIで指定 |
| Frontend操作 | frontendで `npm ci` → `npm run build` → `npm run start`。本番で `npm run dev` は使用しない |
| 描画 | App Router、ログイン・記録画面等はClient Components。動的セグメントの編集画面あり。クライアントAPI通信中心だが、Nextサーバー不要とは扱わない |
| standalone | 現在 `output: 'standalone'` は未設定。後続タスクで採用可能。採用時はpublic / .next/staticの同梱も検証。現構成はnext startで運用可能 |
| Docker | docker-compose.ymlはPostgreSQLのみ。Backend / Frontendの本番Dockerfile・Compose・公開proxy設定は存在しない |
| DB | PostgreSQLドライバー、Hibernate `ddl-auto: validate`、Flyway自動実行false、手動Maven Flyway V1～V4 |
| 接続・秘密値 | `MYFITNESS_DB_URL/USERNAME/PASSWORD`、`MYFITNESS_JWT_SECRET`。今回実envの値・実DBの状態は読んでいない |
| pool / JVM | Hikariサイズ・JVMメモリの明示設定なし。デフォルト依存を本番設計として固定しない |
| health | Actuator依存なし。運用用の専用healthエンドポイント未実装。401応答は疎通確認であり、正常なhealth 200ではない |
| CI | main/developへのpush・PR。Java17 / Node22、Backend test/compile、Frontend test/lint/tsc/build。contents:read、checkout資格情報非永続化、20分timeout。デプロイjobなし |

前回の102 / 34テスト成功・本番npm脆弱性0件は過去の結果として扱う。
今回は調査・文書のみのため再実行せず、新しいテスト成功や本番での正常起動を主張しない。

### 必須の本番要件

HTTPS、永続PostgreSQL、復旧可能なバックアップ、環境ごとの資格情報分離、最小権限、CI合格後の承認デプロイ、障害時のログ・通知・切り戻しを必須とする。
本番DBは空から構築し、ローカル既存ユーザー・TrainingSessionをコピーしない。

## 3. A / B / C 比較

価格はUSD（CのみJPY）を基本とし、税・カード換算・ドメイン・超過従量料金を別扱いとする。
「最低」は利用可能性の下限であり、実用性・安全性を保証しない。契約画面での再確認が必要。

| 比較 | A：Railway×2＋Neon | B：Render＋Vercel＋Neon | C：VPS＋Compose＋Caddy |
| --- | --- | --- | --- |
| 最低構成 | Railway Hobby最低$5（利用料込み）＋Neon Free $0。ただし両アプリの利用料が$5に収まる保証なし | 無料3サービスも可能だがRender sleep / Vercel用途制限 / DB復旧窓が課題。常時稼働Backendは$7～ | 例：さくら東京2GB 月払1,958円＋別バックアップ。全アプリ同居はメモリ検証が条件 |
| 実用構成の目安 | 本文試算：小規模$13～45/月、Neon Launch＋別保管込み | Backend2GB $25＋Vercel Hobby $0（非商用限定）またはPro $20＋Neon約$4～21＋backup $1～5 → 約$30～71/月。超過料・workspace増額は別 | 東京4GB 月払3,960円＋backup予備150～750円 → 約4,110～4,710円/月。保守人件費別 |
| 初期費用 | 料金表に別の開設料なし。ドメイン等は別 | 同左。Render workspaceの有料化を選ぶ場合は別途固定費 | さくらVPSは初期費用無料、最低利用期間3か月に注意 |
| 無料枠 | Railwayは試用30日$5、その後Free $1/月・service RAM0.5GB。Neon Freeは容量・compute・復旧窓制限 | Render Freeは15分無通信でsleep、workspace月750時間。Vercel Hobbyは個人・非商用のみ | 無期限無料ではない。試用を本番予算に含めない |
| Java | 任意のJava17コンテナ/JAR、使用RAM/CPU課金。まずRAM上限1GiBで検証 | JavaはDocker方式を計画。512MBはOOMリスクがあり2GBを安全寄りの比較条件とする | JRE17コンテナ、heapと総RAMを自主管理 |
| Next | Node22でnext start。定常RAMの従量課金あり | Next向けmanaged runtime/CDN。SSRは実行量等に応じた課金・制限 | Node22コンテナ。ビルドはCI側で行いVPS RAM競合を避ける |
| DB容量・接続 | A/BともNeon：Free 1GB/project、Launch容量従量。直接接続上限はcompute依存、poolerの10,000受付枠とDB同時実行上限は別 | 同左。BackendとDBをSingaporeに揃える | ディスク100/200GBはOS・image・log・DB共有。max_connections/メモリを自分で設計 |
| 永続性 | DBはNeon、アプリ再デプロイと分離。アプリFSをDB保存先にしない | 同左。Vercel関数FSに記録を保存しない | PG専用named volumeを維持。ただし同じVPSの故障・誤操作からは守れない |
| sleep / cold start | Railwayは初期は常時稼働、任意のsleep設定は別検証。Neon idle停止からの復帰を考慮 | Render有料はFreeのidle停止を回避、Vercel実行系cold start、Neon復帰の影響 | 計画停止以外は常駐。OS再起動の復旧を自主管理 |
| HTTPS / domain | managed TLS＋独自domain。証明書発行・HTTP redirectは実環境で確認 | managed TLS/CDN＋独自domain。previewのCORSは本番と分離 | Caddy ACME＋DNS＋80/443。証明書更新・FW・復旧を自主管理 |
| backup / restore | Neon PITR＋別保管pg_dump。Launchの履歴窓は7日へ明示設定 | 同左。ホスティング側のrollbackはDB復旧ではない | off-host dump＋必要に応じWAL/PITRを自分で設計。volumeだけをbackupと呼ばない |
| GitHub / rollback | Git連携あり。CI待ち＋承認releaseを設計。前imageへ戻してもDBは戻らない | Git連携・preview・以前deploymentへ切り戻し。3サービス間のrelease順を管理 | Actionsでimage作成・転送・承認。以前image/digest保持。運用手順の作成量が多い |
| log / monitor | Railway Hobbyログ7日、Pro30日。Neon Launch UI3日。必要ログは機密除去して別保管 | 各dashboardで分断、保持期間は選択planで再確認 | disk増大・rotation・通知・uptimeすべて自主管理 |
| 保守・security | OS/DB保守の負担小。ただしproxy信頼・Secrets・IAMはアプリ側責任 | 運用負担小～中。商用判定、3社の権限・請求・障害対応が必要 | OS/CVE/SSH/FW/PG更新/backup鍵を個人で管理。低固定費と引換えに保守負担大 |
| scale / failure | アプリ増強・DB増強を独立実施。Railway障害で両Web停止、DBは別社に残る。複数replicaには共有rate limiterが必要 | Web/Backend/DBの障害が独立、利用者体験はどれか1つの障害でも劣化 | 1台が全体の単一障害点。将来DB分離・冗長化には設計変更 |
| 日本からの位置 | Railway SG＋Neon SG。東京リージョンは確認できず。日本→SGの遅延を実測して判断 | VercelはTokyo computeあり。Render/NeonはSG。browser→APIの距離は変わらない | 東京VPSなら日本ユーザーとの距離は短い。実測は未実施 |

価格根拠：[Railway料金](https://railway.com/pricing)、[Neon料金](https://neon.com/pricing)、[Render compute仕様](https://render.com/docs/compute-plans)、[Render公式価格記載](https://render.com/articles/top-heroku-alternatives-agencies)、[Vercel料金](https://vercel.com/pricing)、[さくらVPS料金](https://vps.sakura.ad.jp/specification/)。Renderの料金ページ本文は表を取得できなかったため、$7/$25は公式記事とcompute仕様の照合による。契約直前に料金画面で再確認する。

無料枠・リージョン根拠：[Render Free](https://render.com/docs/free)、[Vercel Hobby利用条件](https://vercel.com/docs/plans/hobby)、[Railway regions](https://docs.railway.com/deployments/regions)、[Render regions](https://render.com/docs/regions)、[Vercel regions](https://vercel.com/docs/regions)、[Neon regions](https://neon.com/docs/introduction/regions)。リージョンが同じ都市でもprivate network共有ではない。Neon接続はTLS必須、転送料も別計上する。

### 課金制御の相違

- Railway：soft通知とCompute hard limitを区別する。hard到達時はworkload停止。可用性との交換条件であり、他社Neonの請求や税・別商品を止めるものではない。[公式Cost Control](https://docs.railway.com/pricing/cost-control)
- Neon：Launch/Scaleのspending通知は80%/100%、15分ごとに確認。現行公式文書では自動停止は未提供。**通知額を厳格な請求上限と扱わない**。[公式Spending notifications](https://neon.com/docs/introduction/spending-notifications)
- Vercel：金額設定だけでなくPause Production Deploymentsを有効にする必要がある。検知遅延・別請求項目も確認する。[公式Spend Management](https://vercel.com/docs/spend-management)
- Render：workspace feeとcompute/転送料を分離。新Hobbyは5GB、超過$0.15/GB。build pipeline予算はbuild停止の制限であり、請求全体のhard capではない。全体上限・帯域通知の現行提供状況は未確認として契約前に照会する。[新workspace料金](https://render.com/docs/new-workspace-plans)、[Build Pipeline](https://render.com/docs/build-pipeline)
- VPS：基本料金は固定でもbackup/domain/追加disk等は別。予算はproviderごとに管理し、月末だけでなく毎日確認できる担当を決める。

## 4. 第一候補の費用モデル

見積もりであり請求保証ではない。為替未取得のため **1 USD = 150円** を計画用に仮定。
税・為替手数料・domain代を含めない。domainは選択TLDの取得/更新料を別見積もりし、既存domainがなければ利用者承認後に取得する。

概算単価：Railway通常サービスRAM約$10/GB月、CPU約$20/vCPU月、egress $0.05/GB。
Hobby請求は概ね `max($5, 対象リソース利用料)`、Proは `max($20, 利用料)`。
最低料金に同額usageを二重加算しない。秒単価の月換算・丸めで差が出る。
Neon Launchはcompute $0.106/CU時＋storage $0.35/GB月。
復旧履歴$0.20/GB月、snapshot $0.09/GB月も予算へ含める。[料金](https://railway.com/pricing)、[Neonプラン詳細](https://neon.com/docs/introduction/plans)

| 仮定 / 概算 | 最低限MVP・間欠利用 | 少人数の日常利用 | 利用増加の例（人数保証ではない） |
| --- | --- | --- | --- |
| Railway平均RAM（両アプリ合計） | 0.75GB | 1.5GB | 3GB |
| 平均CPU / egress月 | 0.03vCPU / 5GB | 0.1vCPU / 20GB | 0.3vCPU / 100GB |
| Railway利用料 | $8.35程度、Hobby最低$5内包 | $18程度、Hobby | $41程度、Pro最低$20内包 |
| Neon compute | 0.25CU×120稼働時間＝$3.18 | 0.25CU×730時間＝$19.35 | 平均1CU×730時間＝$77.38 |
| DB / 履歴 / snapshot課金容量 | 各1GB＝$0.64 | 各2GB＝$1.28 | DB10GB/履歴5GB/snapshot10GB＝$5.40 |
| Neon合計 | $3.82程度 | $20.63程度 | $82.78程度 |
| 別保管backup予備費（サービス未選定） | $1～5 | $1～5 | $3～10 |
| 想定合計（丸め・小幅予備込み） | **$13～18/月** | **$40～45/月** | **$125～140/月** |
| 日本円仮換算 | **1,950～2,700円/月** | **6,000～6,750円/月** | **18,750～21,000円/月** |

固定費：Railwayの最低利用額のみがfloor、Neon Launchは従量であり「月$15定額」ではない。
backup予備費は公式見積もりではなく未選定サービスへの予算枠。必要な保持量・転送料が増えれば不足する。
snapshotは保持世代の課金容量合計であり、DBサイズと常に等しいわけではない。

間欠モデルはidle停止が実際に成立することが条件。Hikariのidle接続、keepalive、監視SQL、アクセス継続等の影響を検証する。
0.25CUが常時稼働するとMVPモデルでもNeon computeが約$16追加され、合計は約$29～34になる。
実測前の予算として日常利用モデルを採用し、Railway soft $20 / hard $30、Neon通知 $25、backup上限予算$5等を承認案とする（合算の強制上限ではない）。

コスト増要因：平均RAM、SSR化、bot/API負荷、DB稼働時間・autoscale、履歴/WAL・snapshot世代、接続維持、egress、preview環境の放置。
節約順：不要環境停止→buildとruntime分離→pool/idle調整→計測後RAM調整→CDN/静的部分最適化。
backup削減・権限緩和・無料sleepへの無検証移行で帳尻を合わせない。

## 5. リソース要件：事実と推定を分離

| 項目 | 事実 | 初期検証用の推定・計画値 |
| --- | --- | --- |
| Java RAM | 現行コードにXmx等なし。Java17を採用 | runtime RSS 0.5～1GiB程度を仮置き。limit1GiB、heap上限50～60%から測定。metaspace/direct/thread/native分を残す |
| JVM container | Java17のcontainer対応を前提にできるが、使用imageとcgroup認識は未検証 | 固定JRE17 imageでOS/JVM認識・OOM・heapを隔離試験。最大heap＝process RAMとは扱わない |
| Next RAM | 本番next startと開発next devは異なる。standalone未採用 | runtime 0.25～0.5GiB、build 1～2GiB以上を仮置き。nonce SSR化で増える可能性 |
| Backend build | Maven packageが必要 | build用1～2GiB以上を別枠。runtimeと同居させない |
| pool | datasource Hikariの明示上限なし | 初期max5/minIdle0、idleTimeout300000ms、connectionTimeout30000msを検証案。deploy重複中は2×5＋migration/adminを予算化 |
| DB | 接続上限はcompute依存 | 0.25CUから負荷検証。上限は将来の `SHOW max_connections` で確認。10,000pooler枠をアプリpoolサイズにしない |
| startup | 本番起動時間を今回は測定していない | 目標60秒以内、health timeout120～180秒は仮設定。Neon復帰・cold startを含めて測る |
| VPS | 同居するOS/PG/proxyもRAMを使用 | 4GBを初期推奨。2GBは外部build・厳密な制限・OOM試験を満たす場合だけ再検討 |

上記は実測結果ではなく容量計画。稼働中環境の停止・再起動・DB接続による測定はしていない。
standaloneの出力仕様は[Next.js公式](https://nextjs.org/docs/app/api-reference/config/next-config-js/output)、pool方式は[Neon公式](https://neon.com/docs/connect/connection-pooling)を参照。

## 6. 本番構成図（承認後の目標、未構築）

```text
日本の利用者 / HTTPS
  ├─ app.example.com → Railway公開edge/TLS → Frontend Node22・1 instance
  │                      build時 NEXT_PUBLIC_API_BASE_URL
  └─ api.example.com → Railway公開edge/TLS → Backend Java17・1 instance
                         信頼済みedgeのIP正規化【公開前に要実装・検証】
                         app資格情報のみ / JDBC TLS / Hikari小pool
                                  ↓
                   Neon Singapore / PostgreSQL16
                   owner(NOLOGIN) / app / migrator / provider管理者
                       ├─ 有料PITR・snapshot
                       └─ 定期pg_dump → 暗号化した別保管先

GitHub Actions → tests/build → 承認付きmigration job（direct接続・SET ROLE owner）
                          → 同じcommitのBackend → Frontend → E2E
```

Railwayのprivate DNSとNeonは同一private networkではない。
独自domainを追加する場合も、APIを無認証の別経路から迂回できないproxy設計にする。

## 7. 今回のGit・既存差分の記録

- develop、HEAD上記、origin/developとの差分0/0。remoteの再fetchは今回の再開では行っておらず、取得済みremote-tracking refとの比較。
- `frontend/next-env.d.ts`：routes参照が `.next/types/` → `.next/dev/types/`、root-params型参照が追加された既存差分。
- `frontend/AGENTS.md`：未追跡。Next内部docs参照と、next devが文面を生成する旨の記載。
- インストール済み `next/dist/server/lib/generate-agent-files.js` に同じ文面、`start-server.js` にdev起動時の同期処理を確認。型定義生成関数にも上記参照を書き込む処理がある。
- **事実はコードの一致まで**。今回の変更を作ったPID・時刻の因果関係・実行履歴は未確定であり、自動生成と断定しない。推定はnext devによる更新。
- 両ファイルの削除・復元・上書き・stage・commitは行わない。AGENTSのcommit推奨文は今回のユーザー指示より優先しない。

今回の新規作成はdocsの3文書のみ。アプリコード・env・DB・稼働環境は変更しない。
