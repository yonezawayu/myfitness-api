# M5-11 公開前セキュリティ・残タスク・調査証跡

2026-11-08、計画のみ。現段階で「そのまま一般公開可能」とは判定しない。
関連：[構成・費用](M5-11-deployment-comparison.md)、[実行前レビュー用Runbook](M5-11-production-runbook.md)。

M5-12の実装・隔離検証：[信頼プロキシ・healthの仕様と反映状況](M5-12-proxy-health.md)。
以下はM5-11時点の棚卸し。M5-12は2026-11-09に承認後のソース反映・ローカルhealth応答確認まで実施したが、Railway ingress識別は引き続き公開ブロッカー。

M5-13の隔離実装：[Frontend CSPとJWT保存方式の評価](M5-13-frontend-security.md)。nonce方式・動的描画の影響、実ブラウザ未検証、実repo反映承認待ちを分けて記録する。

## 1. M5-10で確認できた対策と限界

| 対策 | 現行コードでの確認 | 残る条件 |
| --- | --- | --- |
| Swagger | prod / productionでspringdoc無効、Securityも拒否、再有効化時startup guard | profileと環境上書きを実公開先で確認 |
| auth rate limit | POST /auth/login 10回、POST /users 5回、各300秒、429/Retry-After/no-store | 全試行に適用、IP×種別、同プロセス内のみ。再起動でリセット |
| encoded URL | permitAllとfilterで同じMvcRequestMatcher | public proxyでのpath変換も隔離再現が必要 |
| メモリ保護 | max10,000枠、期限切れ掃除、synchronized処理。満杯時は安全側429 | botnet/IPv6アドレス変化による枠埋め、NAT同一IP巻込みは残る |
| IP | socket peerのIPv4/IPv6数値を正規化、任意の転送headerを直接読まない | proxy配下は全利用者同じpeerになり得る。現状のままhosted公開不可 |
| CORS | 公開HTTPS Originの明示、Authorization/Content-Type/Idempotency-Key、Retry-After expose | preview Originをwildcard追加しない。localhostは本番許可しない |
| error/log | DEBUG/TRACE false、request details抑制、error message/binding/stack非公開、認証DTO保護 | providerアクセスlog・例外・CI traceも検証。既存対策で無漏えいを無条件保証しない |
| headers | nosniff、Referrer-Policy、Permissions-Policy、X-Frame-Options、限定CSP | script-src/style-src/connect-src未設定、XSS対策は未完成 |
| HSTS | production build＋明示HTTPS frontend hostでmax-age=31536000、subdomains/preloadなし | TLS終端後の実応答、HTTP redirect、API hostも確認 |
| JWT | 現行はlocalStorage保存、60分、有効なJWTでAPI認証 | XSSでtokenが盗まれるリスク。cookie移行は今回未実装・別設計 |

## 2. Proxy / IP：構成別の具体策

### A Railway（第一候補）

1. provider公式仕様でpublic edgeのX-Real-IPの供給は確認。ただしユーザー指定headerの上書き・複数値・private/TCP経路の保証を本調査では確定できない。
2. providerへ信頼境界とheader sanitize保証を照会し、正式仕様/回答を証跡化する。
3. public edge経由だけで使うcanonical client IP resolverを別実装。任意のXFF先頭を採用しない。private/TCP/別proxy経路のアクセスを排除するか、未信頼経路ではpeerにfallback/拒否する。
4. X-Real-IP / XFF / Forwarded偽装、IPv4/IPv6、重複header、複数proxy、直接接続、別hostで隔離攻撃テスト。正常2クライアントが枠を共有しないことも確認。
5. 検証できなければ認証APIを一般公開しない。edge gatewayを追加する場合も、そのgatewayまでのIP信頼問題を先に解く必要があり、単にhopを増やせば解決とは扱わない。

根拠：[Railway header仕様](https://docs.railway.com/networking/public-networking/specs-and-limits)。

### B Render / Vercel

Backendへの実経路はbrowser→Renderであり、FrontendがVercelだからVercel headerを信用できるわけではない。
Render側ingressの上書き保証・直接経路を確認し、Aと同等のtrusted peer/headerテストを実施する。
FrontendをAPI proxyに変える場合はCORS/認証/CSRF/転送headerを再設計し、今回の計画へ無断で混ぜない。

### C VPS / Caddy

最初はCDNを前段に置かず、Caddyだけを80/443へ公開する。Backend/DBのhost portは公開しない。
専用Docker networkのproxy送信元だけを信頼し、Caddyで外部からのIP headerを破棄・正規値へ置換する。
Tomcatまたは専用resolverで固定proxy範囲とright-to-leftの信頼連鎖を検証。Dockerの広いRFC1918全体を無条件に信用しない。
将来CDNを入れるときはCDN以外のorigin直アクセス遮断、provider IP範囲更新、二段proxy偽装試験が必要。

全案とも初期Backendは1 replica。rolling releaseの一時二重稼働でも枠が別になるため、瞬間的な制限の緩みを残存リスクとして記録する。
複数instanceを常用する前にedge共通制限または共有storeへ移行する。DBテーブルを安易にrate counterへ転用しない。

## 3. CSP / tokenの公開判断

現在のCSPはframe/object/base/formに限られ、スクリプト実行やAPI送信先を縛っていない。
localStorage JWTと組み合わせたXSS時の被害が残るため、一般公開前にstrict CSPの隔離PoCを優先する。

Next.js nonce方式では動的render・キャッシュ・サーバー負荷が変わる。インラインstyleも現行画面に存在する。
nonceを付けるだけでstyle属性を全て安全に許可できるわけではない。外部CSS化と違反収集・主要画面E2Eが必要。
script-srcを安易なunsafe-inline/unsafe-evalにして「厳格CSP完了」と扱わない。
Report-Onlyは移行時の観測であり、強制制御の代わりではない。[Next.js公式CSP](https://nextjs.org/docs/app/guides/content-security-policy)

対応案：nonce＋動的render＋必要最小connect-src、style移行を検証。静的生成維持が必要ならhash方式の適合性を別調査し、実験的SRIだけでinline制御完了としない。
HttpOnly/Secure cookieへのtoken移行は別候補だがCSRF・refresh・cross-origin設計が必要。今回は方式を変えない。
一般公開前にCSP/token方針を決定する。未解決なら限定betaへの縮小と明示的リスク承認が必要であり、担当者判断だけで公開しない。

## 4. 公開前必須チェックリスト

- [ ] 構成、月額予算、リージョン、課金停止時の可用性、運用担当の承認。
- [ ] Java17 / Node22 / artifact digest / lockfile固定、image更新手順。Backend/Next依存の公開直前auditとサポート状態確認。
- [ ] 本番build/port/healthを隔離で検証。DevToolsなし、Hibernate validate、Flyway自動実行なし。
- [ ] 信頼proxy/IP resolver実装・偽装テスト、単一instance運用、rate limit429・回復・容量保護。
- [ ] prod profile、Swagger拒否、500内部情報なし、DEBUG/TRACE/log levelの安全性。
- [ ] HTTPS、証明書更新、HTTP redirect、CORSの明示Origin・3header preflight、HSTS host制限。
- [ ] CSP/tokenリスクへの実装・検証または限定公開の明示承認。モバイルを含むE2E。
- [ ] 空の新規PG16・SQL作成の制限付きrole、owner NOLOGIN、app管理属性なし・所属なし。
- [ ] direct TLS検証、migrator SET ROLE owner、V1～V4、checksum、owner/ACL/制約、app拒否権限。
- [ ] 本番独立JWT/DB秘密値、MFA、Secretsの最小スコープ、変更・漏えい時のrotation手順。
- [ ] PITR保持設定、暗号化別保管dump、restore訓練、RPO/RTOの実測合格。
- [ ] CI合格の承認release、migrationの一重化、rollback artifact、DB互換性確認。
- [ ] 監視/backup失敗/429/5xx/メモリ/課金アラートと受信・対応テスト。
- [ ] 利用規約・プライバシー説明、海外保管、保持/削除/問い合わせ手順。必要な法的確認は別途行う。
- [ ] 登録APIの悪用対策方針。初期は限定募集を検討し、bot負荷・メール所有確認・password resetの不足を公開仕様に反映する。
- [ ] 公開先で専用アカウントE2E、既存データに触れない後片付け、実ログの秘密値なし。

## 5. 公開後へ回せる改善（条件付き）

| 改善 | 後回しにできる条件 |
| --- | --- |
| 分散rate limiter・複数replica | 1 replica限定を守り、再起動/rolling中の緩みを受容できる |
| 高度なAPM・trace・長期監査 | 最低限のuptime/error/backup/課金通知と対応者が存在 |
| 自動scale・多地域・read replica | 現行負荷・遅延・復旧目標を単一地域で満たす |
| WAF / CAPTCHA / メール確認 | 限定betaで登録悪用監視・停止手順があり、未提供仕様の合意がある。匿名一般公開では必要性を再評価 |
| CSP改善の追加最適化 | 公開前に受容可能なXSS/token対策が完成している。現行部分CSPの放置を意味しない |

## 6. 推奨ロードマップ

1. **次の実装タスク**：deployment-ready化（trusted proxy/IP、live/ready、固定image/port）。契約せず隔離テスト。
2. strict CSPとJWTリスク方針のPoC。主要画面・mobile・build/SSRコストを再検証。
3. Neon managed権限向け初期化SQL・検証SQL・backup/restore jobの実装レビュー。ローカル実DBには使わない。
4. 構成A、予算（日常利用$40～45目安）、ドメイン、データ地域、RPO/RTO、限定/一般公開の承認。
5. 別承認でサービス契約・空の隔離staging構築。role/TLS/migration/proxy/health/課金を実証。継続運用費を測定。
6. 承認付きCD/Secrets/rollback・復旧訓練。合格後に本番サービス作成・V1～V4適用を個別承認。
7. HTTPS実E2E、限定公開、監視・費用を観測して一般公開の可否を再判定。

契約・サービス作成・Secrets登録・migration・deployment・本番test dataは今回承認されていない。
既存ローカル13ユーザー等の移行も別承認。文書の承認だけでこれらを実行しない。

## 7. 今回の検証範囲と未確認事項

実施：Git/コード/config/CI/migrationの読み取り、公式料金・技術仕様閲覧、新規docs作成、差分・Markdownリンク・秘密情報の静的確認。
未実施：新しいBackend/Frontend test・build・audit、cloud signup/接続、DB query、providerでのrole作成、実RAM/起動時間/遅延測定、実デプロイ。
前回の102/34テスト等は過去結果であり、新しい成功記録として流用していない。

未確定事項：Render料金画面の全表と請求全体hard cap、Railway ingress sanitizeの保証、Neonでの具体的管理role/DB owner操作、TLS truststore実接続、正確なcold start/RSS、CDのprovider token権限、backup保存先の実料金。
公式料金は変更され得る。契約前にplan画面とこの文書との差を確認し、差があれば予算・手順を改訂して承認を取り直す。
