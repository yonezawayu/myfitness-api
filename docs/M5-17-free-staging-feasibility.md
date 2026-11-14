# M5-17：0円stagingの実現性評価

調査日：2026-11-14（JST）。公式Web資料と現コードの静的確認による評価。サービス登録・作成・API接続・DB接続・Flyway・Docker・起動試験は未実施。
PR前の最終レビューでも公式の無料条件・超過時動作を照合した。無料利用資格・カード要求・残quotaはアカウント画面で未確認であり、0円での稼働を無条件に保証しない。

## 1. 結論とGit・環境の保全

- **短期のBackend＋Neon実環境検証は0円で行える候補がある。ただしMyFitnessの起動・接続・安全性が合格済みという意味ではない。**
- 初回の技術検証は **AのRailway Full Trial（利用資格と未使用creditがある場合）＋Neon Free＋別のローカルHTTPS Frontend** を第一候補とする。1GB枠で512MB起動問題を切り分けやすく、予定するRailwayの信頼境界も観察できる。
- Trialを利用できない場合は、継続無料の **B：Render Free＋Neon Free＋別ローカルHTTPS Frontend** を次候補とする。ただし512MB適合、Java用imageの準備、Renderの信頼境界も未検証。無料だから一般公開可とはしない。
- **Railway継続Freeの月$1 creditは常時稼働保証ではない。** CのVercel追加はFrontend自体のホスティング試験が必要になってから承認する。今回は外部アカウント数・Secret・規約確認を増やさない。
- 無料枠で不足した場合は停止・結果報告とし、有料upgradeで解決しない。既存M5-16の$15/$45予算案は今回の承認ではなく、今回は支出予算0円。

Git確認時の作業ブランチは `feature/staging-execution-plan-docs`、HEADは `f926f8f8c5f3d5bd51ba2847f43dc4df9ecd6217`。作業ツリーは切り替えず、develop参照だけをPR #57の `339690ce6d0d10bdfab2bf169b8b3ff286ceb7f7` へfast-forward同期した。origin/developと0/0、HEADと最新developのファイル差分は0件。

保護対象2ファイルは内容・SHAを維持しstageしない。今回追加するのは本書のみ。既存3000/8080とM5-14の33114/34414/34415は停止・再設定しない。M5-16の[実行計画](M5-16-staging-execution-plan.md)は有料案の履歴として保持する。

## 2. 無料条件・課金の比較

0円とは「選択した無料枠の範囲でクラウド請求0円」を意味する。既存PC・回線・電気代は追加試算外。独自domainの購入、外部addon、商用試用、有料supportは含まない。登録画面の実条件は未確認であり、カード入力・upgradeが必要になった時点で停止する。

| サービス | カード・月額最低 | 無料枠終了/超過と0円条件 |
| --- | --- | --- |
| Railway Trial | 公式料金ページはカード不要。初回$5相当、最大30日 | 30日またはcredit消費でFreeへ移行。Full TrialはGitHub等の自動検証次第。再Trial資格・現在残高は未確認。Hobbyへのupgrade・カード追加をしない |
| Railway Free | $0、毎月$1 credit（繰越なし） | creditが足りなければ継続稼働を諦める。利用継続のための有料upgradeは手動承認が必要。Free切替直後の残高・再開時期は画面確認事項 |
| Neon Free | $0、カード不要 | CU時/転送超過はcompute停止、storage上限では増加する書込みが失敗。Freeの超過請求なし。Launchへ移行・prepaid製品購入をしない |
| Render Hobby workspace＋Free Web Service | workspace $0とservice $0を両方選ぶ。支払方法なしで利用する場合の停止動作が公式に記載される | 無料時間超過は停止。帯域/ビルド超過は、支払方法ありなら追加請求になり得る。支払方法なしなら停止/新build無効。登録時の本人確認等でカードを要求される可能性は未確認。その場合は進めない |
| Vercel Hobby | $0。今回確認した公式料金/開始資料では、カード不要の明示的な保証は確認できなかった | 通常の枠超過は機能停止・回復待ち（多くは30日）。Pro Trial・有料連携・domain購入を選ばない。カード不要で進めることを実画面で確認してから利用する |

根拠：[Railway料金](https://railway.com/pricing)、[Trial](https://docs.railway.com/pricing/free-trial)、[規約の無料credit条件](https://railway.com/legal/terms)、[Neon料金](https://neon.com/pricing)、[Neon Free FAQ](https://neon.com/faqs/free-plan-limits-and-quotas)、[Render Free](https://render.com/docs/free)、[Vercel Hobby](https://vercel.com/docs/plans/hobby)。規約の恒久性・アカウント個別の審査結果は保証しない。

### リソース・継続性

| サービス | CPU/RAM・容量/使用量 | 休止・遅延・永続性 |
| --- | --- | --- |
| Railway Trial | resource表の上限は2 vCPU/1GB。Trial説明ではCPU共有。5 services/project、image4GB | credit期限あり。DBはNeonへ置き、Trial volumeをデータ保管先にしない |
| Railway Free | 1 vCPU/0.5GB、image4GB、ephemeral1GB、volume0.5GB | 月$1枠。sleep設定時もDB pool等の通信があると休止しない。初回復帰で遅延/502の可能性 |
| Neon Free | 100 CU時/project/月、最大2CU≒8GB RAM。0.25CUなら約400起動時間/月。DB1GB/project、合計20GB。転送5GB/project/月 | 5分idleでscale-to-zero、無効化不可。復帰遅延あり。6時間/最大1GB変更履歴のrestore、手動snapshot1件。有料の長期PITR/backupの代用にしない |
| Render Free | RAM512MB。現公式料金表ではFreeの正確なCPU量は未確認（0.1 CPUを保証値として採用しない）。750時間/workspace/月 | 15分無通信で休止、復帰目安約1分。任意restartあり。ローカルdiskは一時的。Neonに永続化する。Free Render Postgresは30日期限があるため採用しない |
| Vercel Hobby | 常駐VMではない。Functionsは2GB/1vCPU、月4 CPU時・360 GB時・100万invocations。build2vCPU/8GB | 実行/初期化遅延はあり得る。常駐Spring Bootの置き場にはしない。nonce SSRは毎回実行するためquotaを消費する |

Railway数値は[プランresource表](https://docs.railway.com/pricing/plans)、sleepは[Serverless仕様](https://docs.railway.com/deployments/serverless)。Neonは[plans](https://neon.com/docs/introduction/plans)と上記FAQ。Renderの[料金表](https://render.com/pricing)はHobby帯域5GB、[build pipeline](https://render.com/docs/build-pipeline)は500分/月・build用2CPU/8GBを記載する。runtime512MBとbuild RAMは別。
Vercelは[Functionsメモリ](https://vercel.com/docs/functions/configuring-functions/memory)とHobby使用量表を参照。Fast Data Transfer100GB/月等の枠もあり、CPU/RAM枠だけで利用可能量を判断しない。

### runtime・HTTPS・リージョン・用途

| サービス | Java17 / Next.js16 | HTTPS・region | 商用/非商用 |
| --- | --- | --- | --- |
| Railway | RailpackでJava17を明示。NextはNode22指定のNode service候補 | 提供domainのHTTPSを利用。Singapore/米国/欧州等。無料アカウントでの実選択可否は未確認 | Vercel Hobby同様の一律非商用限定は今回の確認資料で特定できず。Freeは小規模向け、Trial規約/公正利用を別確認 |
| Neon | PostgreSQL。Java/Nextは接続する側。role・TLS・Flywayは下記の検証対象 | DBはTLS。Singaporeを含むAWS8地域。Webサイト用HTTPS serviceではない | Freeでのstaging候補。商用向けの可用性・support保証があるとは扱わない。個別用途は規約確認 |
| Render | Javaのnative runtimeはない。Java17のDocker imageが必要。NextはNode runtime候補 | managed TLS/提供domain。Oregon/Ohio/Virginia/Frankfurt/Singapore | 公式はFreeをproduction用途に使わないよう注意。商用productionの推奨構成ではない |
| Vercel | Next.js対応。16.4.5＋Node22＋proxy/nonceは実deploy未検証。現Spring Boot常駐方式には非適合 | 提供domainのHTTPS/CDN。FunctionsはHobby1地域、Tokyo/SG等の候補。既定は米国なので選択を確認 | **personal/non-commercialのみ**。広告・有料提供等へ移行する前に再判定。stagingという名前だけで制約免除にならない |

根拠：[Railpack Java](https://railpack.com/languages/java/)、[Railway regions](https://docs.railway.com/deployments/regions)、[Neon regions](https://neon.com/docs/introduction/regions)、[Render runtime](https://render.com/docs/native-runtimes)、[Render regions](https://render.com/docs/regions)、[Vercel Next](https://vercel.com/docs/frameworks/full-stack/nextjs)、[Vercel地域](https://vercel.com/docs/functions/configuring-functions/region)、[地域一覧](https://vercel.com/docs/regions)、[Hobby利用規約](https://vercel.com/legal/terms)。新しいdomainを購入せず無料提供domainを使う。

## 3. MyFitnessの必要メモリ：確認事実と未検証事項

コード上の事実：Spring Boot3.4.12、Java17、JPA/Hibernate/Tomcat/Securityを使用。ddl-auto=validate、起動時Flyway=false。本番profileはprod/production。Hikariサイズの明示設定はない。Java17はコンテナのメモリ制約をheap設計に利用できるが、**heap上限はプロセス総メモリ上限ではない**。

2026-11-14の読み取りsnapshot（macOS `ps`、RSS KiBを1024で除算）：

| プロセス | PID | 現時点RSS | この値では分からないもの |
| --- | --- | --- | --- |
| 既存Java Backend | 75372 | 50,784KiB≒49.6MiB | 起動ピーク、負荷時、swap/compression分、Linux cgroup使用量 |
| M5-14 Next Production | 17183 | 4,936KiB≒4.8MiB | 初期化/SSR/buildピーク・同時処理メモリ |
| 既存Next dev | 1115 | 6,632KiB≒6.5MiB | 本番用メモリの根拠には使えない |

長時間idleのresident値であり、仮想メモリ・swap・圧縮等を含む総占有量ではない。**49.6MiBなので512MBで十分、とは判断しない。** 起動時RSS/起動時間/本番steady-state/最大使用量は未測定。停止・再起動やJVMへのattachを行っていない。コマンド行上に-Xmx/-Xms等の明示memory flagは見つからなかったが、環境変数・実効flagの全確認はしていない。

| 上限 | 実現性評価 | 次回承認後の試験案（今回は設定しない） |
| --- | --- | --- |
| Railway Free512MB / Render Free512MB | **起動する可能性はあるが未検証・OOMリスクあり** | heap256MiB、初期heap64MiB等を候補に、metaspace/code cache/direct buffer/thread/nativeの余裕を確保。総量とGCを測って決める |
| Railway Trial1GB | 512MBより余裕があり、短期検証の第一候補。ただし合格保証ではない | heap512MiB等を仮案とし、全プロセスのピーク・起動時間・load/Neon復帰を測定 |
| Nextローカル | 既存PCの別コピーでProduction起動できる見込み | 既存3000・M5-14を変えず新しいport/専用設定を別承認。build時とSSR時を別測定 |
| NextをRailway Freeへ同居 | 今回のA/Cでは不要 | BackendとNodeを同一512MB containerへ詰めない。別serviceでもcreditは共有消費する |

[Java17のメモリ指定仕様](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html)を参照。既存M5-16のMaxRAMPercentage=60は試験案であり、512MBに適合済みの設定ではない。単純に-Xmxを増やしてOOMを解決しない。

次回測定の合格案：起動時と代表CRUD/並列処理の総メモリをLinuxのcontainer指標で測り、quotaに十分な余裕（初期基準20%）があること、OOM/再起動/過大なGC/health失敗がないこと。512MBで基準を満たさなければTrial資格を確認するか中止する。有料に変更しない。Renderのbuildは別computeなので、Mavenのbuild成功はruntime512MB起動成功を意味しない。

## 4. Neon Freeで検証する範囲

公式上、SQLによるrole作成とNOLOGINは利用できる。Console/APIで作ったroleはneon_superuser membershipを持つため、app/migratorは安易に管理UIのrole作成だけで済ませない。[roles](https://neon.com/docs/manage/roles)

実現見込みはあるが、**このアプリのowner/migrator/app分離・SET ROLE・ACL・V1〜V4・Hibernate・TLS・dump/restoreは実Neonではすべて未検証**。Freeだから確認を省略しない。

承認後の検証順序案（SQLは[既存M5-16](M5-16-staging-execution-plan.md)を再レビューして使用）：

1. 専用Free project/空DB、PG16の選択可否、SG候補、残quotaを確認。既存ローカルdumpや13ユーザーを移行しない。
2. direct接続でTLSのCA/hostname検証と失敗ケース。JDBCはverify-full等の安全な構成を採用する計画で、requireだけで証明書検証済みと扱わない。[TLS](https://neon.com/docs/connect/connect-securely)
3. owner NOLOGIN、migratorだけSET ROLE owner、appに昇格権なし。V4の固定role名と一致させ、appにDDL・不要DMLを拒否する。
4. migration専用資格情報＋directでV1〜V4、info/validate、history4成功・失敗0を確認。自動再試行・repair/cleanはしない。app資格情報でHibernate validate後に専用データのみを試験する。
5. pooled接続はappの別試験。transaction poolerでsession SET ROLEを維持する設計にしない。advisory lock/原子性/並列冪等性を実測する。[pooling](https://neon.com/docs/connect/connection-pooling)
6. direct pg_dumpの新規archiveをGit外へ保存し、別空DBへrestore、件数/値/制約/owner/ACL/sequence/historyを比較する。Freeの少数branch・1GB枠内で実施。復旧できると確認するまでarchiveを削除しない。[backup](https://neon.com/docs/manage/backup-pg-dump)

小さいdirect pool（最大3・minimumIdle0等）は候補であって未反映。DB接続維持/keepalive/監視がNeon休止を妨げる場合がある。0.25CU常時720時間なら180 CU時となりFree100 CU時を超える。試験/restoreのcomputeも合算する。検証時以外に起こし続ける外部uptime監視は使わない。

Freeでできない/代替にならないもの：常時稼働保証、長期の復旧窓、専用の有償support・SLA相当、長期自動backup運用の保証。短い復旧窓内の試験と独立dump/restoreは候補として残す。

## 5. A/B/Cの比較とローカルFrontendの条件

| 構成 | 想定費用・0円条件 | 主な制約/課金リスク | 検証可能な範囲（承認後） | 未確認/不足と移行性 |
| --- | --- | --- | --- | --- |
| A：Railway Trial/Free＋Neon Free＋local | クラウド0円。Full Trialの残credit/期間内、またはFree月$1内。NeonもFree内 | Trial審査・外向き通信制限・credit期限。Free512MB。カード追加/Hobby移行は不可 | Java17起動、Neon TLS/role/migration、API/health/原子性/再送、Railway peer診断 | Frontend cloud/CDNは対象外。Railway信頼境界は未解決。将来予定構成と共通部分が多い |
| B：Render Free＋Neon Free＋local | クラウド0円。workspace/service両方Free、支払方法なし、各quota内 | 512MB、Java Docker image必要、cold start。Neon向け大量外向き通信でFree停止の可能性。カードありでは超過課金リスク | Backend/DBの上記項目、Renderでの復帰/停止、限定E2E | Renderのpeer/header保証も別途必要。SSH/one-off job不可なのでmigrationは別の承認runner。将来Railwayへ移すとplatform検証やり直し |
| C：A＋Vercel Hobby | A条件＋個人非商用Hobby/各quota内なら0円候補 | アカウント/制限増、nonce SSRのquota、利用規約。Pro Trial/addon/商用利用は不可 | A＋外部HTTPS Frontend、CSP/nonce、CORS/実ブラウザ | 認証APIへのbrowser通信は直接Railwayへ向くため、Vercel追加でRailway信頼境界は解決しない。Frontend一般公開も未承認 |

**A/Bの「local」は既存localhost:3000の設定変更を意味しない。** prod/productionのCORSはHTTP/localhost Originを拒否する。prodを外して回避するのではなく、新しい隔離Frontendコピー＋loopback待受＋検証用HTTPS hostname/証明書で、正確なOriginだけを許可する計画が必要。

M5-14はlocaltest.me上のHTTPSモックで確認した履歴があるが、その環境を勝手にクラウドAPIへ差し替えない。別コピー・別port・新規設定・必要ならDNS/hosts/証明書信頼の操作を別承認する。ブラウザから外部APIへ直接接続するためlocalをinternet公開するtunnelや有料domainは不要。自己署名例外でよいと一般化せず、検証用証明書の対象/信頼を確認する。

新コピーのbuild前にNEXT_PUBLIC_API_BASE_URLをクラウドHTTPS APIへ設定し、CSP connect-srcとの一致、nonce/401/CRUDを確認する。実際のAPIアクセスや設定変更は今回は行っていない。

## 6. 信頼プロキシと公開ゲート

- Railway公式はX-Real-IP等を説明するが、callerヘッダー除去/上書き、peer/CIDR安定性、別経路迂回遮断の保証は今回も確定できない。**Railway信頼プロキシは公開ブロッカーを維持**。[公式仕様](https://docs.railway.com/networking/public-networking/specs-and-limits)
- 現ClientIpResolverはforward-headers-strategy=none、明示CIDRからのXFFだけを右側から解析する。空設定ではsocket peerを使用し、X-Real-IPを無条件に採用しない。edge配下では全利用者が同じ枠になる可能性が残る。
- Render公式もedge配下ではproxy IPとなりXFFを使うと説明するが、それだけでは現resolverに必要なpeer/CIDR・偽装除去・迂回遮断の証明にならない。Renderを選ぶ場合も同じ安全性ゲートを別に確認する。[Render edge解説](https://render.com/articles/how-render-handles-ddos-attacks)
- NeonはDBでありHTTP認証レート制限のproxy問題を解決しない。VercelでFrontendをhostするだけでも解決しない。
- まずDB資格情報を持たない診断で信頼境界と到達制限を確認する計画。stagingという名称、推測困難URL、CORSだけを認証endpointの公開制限にしない。安全に限定できなければ一般公開serviceとしてappを作らない。
- 2つの別外部IPの枠分離、同一IPの通常/encoded URL枠共有、転送偽装による回避不可、login11回目/登録6回目429、300秒後回復、不正値400、OPTIONS/CORS/401を将来の合格条件とする。同一NATの枠共有は現IP方式の限界。
- Free/idle restart時にメモリ内の制限枠は消える。単一instanceでも永続/分散制限の保証ではない。複数instance共有、JWT localStorage/XSS、本番CSP/HTTPS、DB疎通を含まないreadiness、監視/復旧も残課題。

## 7. 0円を維持する運用ルールと撤収

1. 課金手段を追加しない。既存の有料workspace/teamへ作らず、現在のplan/請求手段/残quotaをユーザーが確認する。カード入力を要求された場合は停止する。Renderはカード確認時に一時$1取引があり得るため返金予定でも今回の0円方針では許容しない。
2. Hobbyという名称を混同しない。Railway Hobbyは有料、Render Hobby workspaceはservice別課金、Vercel Hobbyは個人非商用無料。Neon Launchも最低額なしでも従量有料なので選ばない。
3. paid Trial、marketplace、domain、object storage/AI等の別製品、有料監視・外部CI有料runnerを使わない。無料credit再取得のための複数アカウントは作らない。
4. 予算は**0円**。通知は強制上限ではない。無料枠の使用率50%でレビュー、使用率80%で検証停止判断、quota不足なら回復待ち/中止。支払方法なし・Free維持を毎回確認し、停止を避けるためupgradeしない。
5. 初回は2～4時間の試験枠、長くても48時間で継続再承認を提案。Railwayはcredit期限前にも終了判断し、Free移行後の512MBで動くと仮定しない。Renderを人工的なpingで起こし続けない。
6. teardownは作成済み対象ID・架空データ・backup保持を確認して別承認する。Secret失効、自動deploy無効化、service/branch等の撤収と最終請求0円の確認までを計画する。停止/削除は今回行わず、M5-14も対象外。

Railway Freeの継続時間目安（費用計算用、実測ではない）：RAM0.5GB、平均CPU0.01、転送なしなら約$0.00723/時、月$1は約138時間。48時間なら約$0.35相当、0.5GB常時720時間なら約$5.20相当で月$1を超える。TrialでRAM1GB/CPU0.02を48時間なら約$0.69相当。credit消費は現金請求と別で、他service・起動CPU・転送・storageにより減る。枠を超えても0円で常時運用できる意味ではない。[従量単価](https://railway.com/pricing)

## 8. 次の承認事項と未検証の境界

次に求めるのは**Aの無料Trial利用資格確認とNeon Freeの登録/作成を行うか**の承認。アカウント・規約・GitHub連携権限・リージョン・0円/カード未登録方針・試験期限・撤収担当を決める。A不成立ならBへ自動移行せず、Render登録とJava image準備を改めて承認する。

以下は一括承認とせず分ける：

- 無料アカウント登録とresource作成（カード必須なら停止）。
- 外部staging DB接続、role/ACL・Flyway・架空テストデータ・backup/別DB restore。
- 診断serviceの実装/deployと到達制限・peer確認。Railwayへの問い合わせ送信は別途承認。
- BackendのJava17 image/ビルド/メモリ設定、local隔離Frontendの作成・HTTPS・CORS設定と限定E2E。
- Cを選ぶ場合はVercelの用途適合・登録条件・GitHub権限・固定Originを再確認。
- 作成した対象だけの停止/削除、証跡/backupの保持期限・Secret失効。

今回確認できたのは公式仕様・静的適合性・既存プロセスsnapshotであり、**無料枠上での起動/実Backend＋Neon E2Eは未実施**。本番相当の常時可用性・負荷/障害・長期復旧・分散制限は無料試験の合格から保証しない。

調査・文書PRの保存は実環境操作の承認を兼ねない。アプリコード・既存env・保護対象を変更せず、サービス登録・アプリAPI/DB接続・Docker・停止/再起動・問い合わせ送信は行わない。今回は別途承認された本書1件のみのコミット・Push・PR作成を対象とする。
