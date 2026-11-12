# Railway support inquiry draft: trusted ingress and client IP

作成・最終確認：2026-11-12（JST）。**英文の問い合わせ案のみ。未送信。**
送信先・プラン上のサポート窓口は未確定。送信、契約、staging作成、デプロイは別承認とする。
実IP、サービスID、資格情報、JWT、ログの本文は含めない。回答を得るまで未確認事項を保証済みへ変更しない。
背景と公開条件：[M5-15技術評価](M5-15-railway-neon-readiness.md)。

## Subject

Clarification requested: authoritative client IP and trusted ingress boundary for Spring Boot rate limiting

## Message

Hello Railway Support,

We are evaluating Railway for a single-instance Java 17 / Spring Boot application with IP-based limits on login and user registration. We have not deployed it to Railway yet.

Our current implementation uses the actual socket peer address by default. It accepts X-Forwarded-For only when that peer matches explicitly configured trusted proxy IPs/CIDRs, and walks the chain from right to left until the first untrusted hop. Spring Boot's forward-headers-strategy is set to none, so forwarding headers do not rewrite the peer address before the trust check. We do not currently trust X-Real-IP or Forwarded.

The public networking specifications describe X-Real-IP as the client's remote IP. Before relying on this for security-sensitive rate limiting, could you clarify the supported guarantees below and point us to current official documentation?

1. **X-Real-IP authority and normalization**
   Does Railway always generate or overwrite X-Real-IP on every supported public HTTP ingress path, including generated domains and custom domains? Is the value a single validated numeric IPv4/IPv6 address? Are caller-supplied copies removed, including duplicate header fields, case variations and malformed values, rather than forwarded or concatenated?

2. **X-Forwarded-For construction**
   Does the edge replace, append to, preserve, or reject caller-supplied X-Forwarded-For? What is the exact ordering and meaning of the resulting hops? Are additional internal proxy hops inserted? What guarantees apply to duplicated headers, malformed input and IPv4/IPv6 representations? Is Forwarded sanitized as well, or should applications ignore it entirely?

3. **The peer visible to the application**
   Which socket peer address does a Spring Boot service observe after the request passes through Railway's edge and internal routing? Are authoritative ingress source IPs/CIDRs published and stable across deploys, regions and infrastructure changes? If they are not, what supported mechanism establishes that a request came through the trusted HTTP edge rather than another reachable path? We do not want to trust all private networks.

4. **Recommended Spring Boot configuration**
   For authentication rate limiting, do you recommend peer-validated X-Forwarded-For, an edge-sanitized X-Real-IP approach under a defined network boundary, or another supported solution? Can this be configured without enabling unrestricted forwarding-header trust? Please distinguish client-IP resolution from trusted HTTPS scheme/host handling.

5. **Bypass prevention and private networking**
   How can we ensure that the application cannot be reached through an alternate public port, a TCP proxy, or a private/internal route that bypasses HTTP header sanitization? Can public HTTP ingress be enforced while limiting which internal services can reach the backend? Do internal callers receive the same sanitization guarantees, or must their forwarding headers always be treated as untrusted? Are X-Railway-Edge or similar headers safe to authenticate ingress, or can a caller supply them?

6. **Additional proxies and CDN configurations**
   If a CDN or another proxy is later placed in front of Railway, how is the authoritative end-user address determined? What validation of that upstream is performed, and which configuration or published allowlist is required? Please clarify whether any guarantees differ for direct public access, CDN access, private networking and TCP forwarding.

7. **Verification and change management**
   Is there a recommended staging test to verify these properties using two client networks and spoofed forwarding headers? How are changes to ingress peer ranges or sanitization behavior communicated? Which of the above behaviors are supported platform guarantees rather than current implementation details?

Until the trust boundary is established, we plan to keep forwarding headers untrusted and not release the authentication endpoints publicly. That conservative mode may group users behind the same edge peer into one rate-limit bucket, so confirming a supported approach is a release prerequisite.

Thank you for your guidance.

## 回答後の確認ゲート

- 回答が適用されるdomain・HTTP/private/TCP/CDN経路・IPv4/IPv6の範囲を記録する。
- ヘッダー名があるという回答だけでは解決扱いにしない。上書き保証と迂回遮断/peer識別を両方確認する。
- 現resolverと異なる方式が必要ならコード・設定を別レビューする。無条件ヘッダー信頼や広いCIDRへ自動変更しない。
- 予算・地域・期限・担当・観測データの保管/削除方針の承認後にだけstaging試験を実施する。
- この文書の保存・PR作成は、問い合わせ送信や実環境操作の承認を意味しない。
