import assert from "node:assert/strict";
import test from "node:test";
import { randomBytes } from "node:crypto";
import { productionContentSecurityPolicy } from "./content-security-policy.ts";
import { resolveApiBaseUrl } from "./api-base-url.ts";

test("本番CSPはnonceと厳格なresource制限を持つ", () => {
  const nonce = randomBytes(16).toString("base64");
  const policy = productionContentSecurityPolicy("https://api.example.invalid/v1/", nonce);
  assert.ok(policy.includes(`script-src 'self' 'nonce-${nonce}' 'strict-dynamic'`));
  for (const directive of ["default-src 'none'", "script-src-attr 'none'", "style-src-attr 'none'",
    "frame-src 'none'", "worker-src 'none'", "object-src 'none'", "base-uri 'none'",
    "frame-ancestors 'none'", "img-src 'self'", "font-src 'self'", "form-action 'self'"]) {
    assert.ok(policy.includes(directive));
  }
  assert.ok(policy.includes("connect-src 'self' https://api.example.invalid"));
  assert.ok(!policy.includes("/v1"));
  assert.doesNotMatch(policy, /unsafe-inline|unsafe-eval|\*/);
});

test("nonceごとに異なるCSPになる", () => {
  const first = productionContentSecurityPolicy("https://api.example.invalid", randomBytes(16).toString("base64"));
  const second = productionContentSecurityPolicy("https://api.example.invalid", randomBytes(16).toString("base64"));
  assert.notEqual(first, second);
});

test("不正nonce・CSP injectionを拒否する", () => {
  for (const value of ["", "fixed", "abc'; script-src *", "a".repeat(22) + "\r\nInjected: yes"]) {
    assert.throws(() => productionContentSecurityPolicy("https://api.example.invalid", value));
  }
});

test("API originはHTTPS固定でcredentials・localhost・header injectionを拒否", () => {
  for (const value of ["http://api.example.invalid", "javascript:alert(1)", "https://localhost",
    "https://127.0.0.1", "https://[::1]", "https://user:pass@api.example.invalid", "https://api.example.invalid/?x=1",
    "https://api.example.invalid/#test", "https://api.example.invalid/'; script-src *"]) {
    // A path cannot affect connect-src; invalid URLs/settings otherwise fail closed.
    if (value.includes("/';")) {
      assert.ok(!productionContentSecurityPolicy(value, randomBytes(16).toString("base64")).includes("script-src *"));
    } else {
      assert.throws(() => productionContentSecurityPolicy(value, randomBytes(16).toString("base64")));
    }
  }
});

test("既存API URL検証もjavascript/data等の実行スキームを拒否", () => {
  for (const value of ["javascript:alert(1)", "data:text/html,<script>alert(1)</script>", "file:///tmp/test"]) {
    assert.throws(() => resolveApiBaseUrl(value, "production"));
  }
});
