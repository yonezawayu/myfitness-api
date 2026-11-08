import assert from "node:assert/strict";
import test from "node:test";
import { securityHeaders } from "./security-headers.mjs";
import config from "../next.config.mjs";

test("Next.js設定が本番用ヘッダー生成へ接続されている", async (context) => {
  const previous = process.env.NODE_ENV;
  const origin = process.env.MYFITNESS_FRONTEND_PUBLIC_ORIGIN;
  context.after(() => {
    if (previous === undefined) delete process.env.NODE_ENV; else process.env.NODE_ENV = previous;
    if (origin === undefined) delete process.env.MYFITNESS_FRONTEND_PUBLIC_ORIGIN;
    else process.env.MYFITNESS_FRONTEND_PUBLIC_ORIGIN = origin;
  });
  process.env.NODE_ENV = "production";
  delete process.env.MYFITNESS_FRONTEND_PUBLIC_ORIGIN;
  assert.deepEqual(await config.headers(), securityHeaders("production"));
});

test("開発環境ではCSP/HSTSを追加しない", () => {
  assert.deepEqual(securityHeaders("development", "https://app.example.com"), []);
});
test("本番ヘッダーはフレーム・MIME・不要な機能を制限しAPI通信を変更しない", () => {
  const [{headers}] = securityHeaders("production");
  const values = Object.fromEntries(headers.map(h => [h.key, h.value]));
  assert.equal(values["X-Content-Type-Options"], "nosniff");
  assert.equal(values["X-Frame-Options"], "DENY");
  assert.equal(values["Referrer-Policy"], "strict-origin-when-cross-origin");
  assert.equal(values["Permissions-Policy"], "camera=(), microphone=(), geolocation=()");
  assert.match(values["Content-Security-Policy"], /frame-ancestors 'none'/);
  assert.doesNotMatch(values["Content-Security-Policy"], /unsafe-|script-src|style-src|connect-src/);
  assert.equal(values["Strict-Transport-Security"], undefined);
});
test("HSTSは設定済み公開HTTPSホストのみ・サブドメインとpreloadは対象外", () => {
  const rules = securityHeaders("production", "https://app.example.com");
  const hsts = rules[1];
  assert.equal(hsts.headers[0].value, "max-age=31536000");
  const pattern = new RegExp(`^(?:${hsts.has[0].value})$`);
  assert.ok(pattern.test("app.example.com"));
  assert.ok(!pattern.test("appXexample.com"));
  assert.ok(!pattern.test("localhost:3000"));
});
test("HSTSのHTTP・localhost・不正な公開Originを拒否", () => {
  for (const origin of ["http://app.example.com", "https://localhost", "https://127.0.0.1",
    "https://[::1]", "https://app.example.com/path", "https://user:pass@app.example.com", "bad"])
    assert.throws(() => securityHeaders("production", origin));
});
