import assert from "node:assert/strict";
import test from "node:test";

import { resolveApiBaseUrl } from "./api-base-url.ts";

test("開発時は未設定ならlocalhost:8080を使用する", () => {
  assert.equal(resolveApiBaseUrl(undefined, "development"), "http://localhost:8080");
});

test("設定URLの末尾スラッシュを除去し、ベースパスは維持する", () => {
  assert.equal(resolveApiBaseUrl(" https://api.example.com/v1/// ", "production"), "https://api.example.com/v1");
});

test("本番ビルドでは未設定を拒否する", () => {
  assert.throws(() => resolveApiBaseUrl(undefined, "production"), /must be set/);
});

test("本番ビルドではlocalhostとloopbackを拒否する", () => {
  for (const value of ["https://localhost:8080", "https://127.0.0.1:8080", "https://[::1]:8080"]) {
    assert.throws(() => resolveApiBaseUrl(value, "production"), /must not target localhost/);
  }
});

test("本番ビルドではhttps以外を拒否する", () => {
  assert.throws(() => resolveApiBaseUrl("http://api.example.com", "production"), /must use https/);
});

test("認証情報・query・fragmentを含むURLを拒否する", () => {
  for (const value of [
    "https://user:password@api.example.com",
    "https://api.example.com?debug=true",
    "https://api.example.com#fragment"
  ]) {
    assert.throws(() => resolveApiBaseUrl(value, "production"));
  }
});
