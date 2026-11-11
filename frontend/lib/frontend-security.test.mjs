import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { Script } from "node:vm";
import ts from "typescript";
import { renderToStaticMarkup } from "react-dom/server";
import { productionContentSecurityPolicy } from "./content-security-policy.ts";

const require = createRequire(import.meta.url);
const source = path => readFileSync(new URL(path, import.meta.url), "utf8");
function evaluateModule(path, imports, globals = {}) {
  const exports = {};
  const compiled = ts.transpileModule(source(path), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, target: ts.ScriptTarget.ES2020 }
  }).outputText;
  new Script(compiled, { filename: path }).runInNewContext({
    Error,
    URL,
    exports,
    require: name => name in imports ? imports[name] : require(name),
    ...globals
  });
  return exports;
}

// Actual page component with isolated hook/storage/API adapters; no browser or network access.
async function historyHarness({ token = "TEST_ONLY_TOKEN", sessions = [], error } = {}) {
  const storage = new Map(token ? [["myfitness_access_token", token]] : []);
  const navigation = [];
  const states = [];
  const effects = [];
  let index = 0;
  let first = true;
  const page = evaluateModule("../app/training-sessions/page.tsx", {
    react: {
      useState(initial) {
        const slot = index++;
        if (first) states[slot] = typeof initial === "function" ? initial() : initial;
        return [states[slot], value => { states[slot] = typeof value === "function" ? value(states[slot]) : value; }];
      },
      useEffect(effect) { if (first) effects.push(effect); }
    },
    "next/navigation": { useRouter: () => ({ replace: path => navigation.push(path), push: path => navigation.push(path) }) },
    "@/lib/api": {
      TOKEN_STORAGE_KEY: "myfitness_access_token",
      fetchTrainingSessions: async () => { if (error) throw error; return sessions; }
    }
  }, {
    localStorage: { getItem: key => storage.get(key) ?? null, removeItem: key => storage.delete(key) }
  });
  let tree = page.default();
  first = false;
  effects.forEach(effect => effect());
  await new Promise(resolve => setImmediate(resolve));
  index = 0;
  tree = page.default();
  return { tree, html: renderToStaticMarkup(tree), storage, navigation };
}

function findButton(element, label) {
  if (!element || typeof element !== "object") return undefined;
  if (element.type === "button" && element.props.children === label) return element;
  for (const child of [element.props?.children].flat(Infinity)) {
    const found = findButton(child, label);
    if (found) return found;
  }
}

test("実際の履歴コンポーネントでHTML風Sessionメモ・種目名・種目メモをテキスト表示", async () => {
  const payload = "<img src=x onerror=alert(1)><script>alert(2)</script>";
  const { html } = await historyHarness({ sessions: [{
    id: 1, date: "2026-11-11", memo: payload,
    exercises: [{ id: 1, exerciseName: payload, memo: payload, sets: [] }]
  }] });
  assert.match(html, /&lt;img/);
  assert.match(html, /&lt;script&gt;/);
  assert.doesNotMatch(html, /<script|<img/);
});

test("HTML風APIエラーも履歴画面ではテキスト表示", async () => {
  const { html } = await historyHarness({ error: new Error("<svg onload=alert(1)>") });
  assert.match(html, /&lt;svg/);
  assert.doesNotMatch(html, /<svg/);
});

test("未認証ならログインへ誘導する", async () => {
  const { navigation } = await historyHarness({ token: null });
  assert.deepEqual(navigation, ["/"]);
});

test("認証失敗時に保存トークンを削除しログインへ戻す", async () => {
  const { storage, navigation } = await historyHarness({ error: new Error("もう一度ログインしてください。") });
  assert.equal(storage.size, 0);
  assert.deepEqual(navigation, ["/"]);
});

test("ログアウト操作で既存localStorageトークンを削除", async () => {
  const result = await historyHarness();
  const button = findButton(result.tree, "ログアウト");
  assert.ok(button);
  button.props.onClick();
  assert.equal(result.storage.size, 0);
  assert.deepEqual(result.navigation, ["/"]);
});

test("通常の再描画でトークン永続性を変更しない", async () => {
  const { storage, navigation } = await historyHarness();
  assert.equal(storage.size, 1);
  assert.deepEqual(navigation, []);
});

test("実APIクライアントはBearerを維持し認証情報をconsoleへ出力しない", async () => {
  const logs = [];
  const calls = [];
  const password = "TEST_ONLY_PASSWORD_SENTINEL";
  const token = "TEST_ONLY_TOKEN_SENTINEL";
  const api = evaluateModule("./api.ts", {
    "./api-base-url": { API_BASE_URL: "https://api.example.invalid" },
    "./training-session-create": {}, "./user-registration": {}
  }, {
    fetch: async (url, options) => {
      calls.push({ url, options });
      return { ok: true, status: 200, json: async () => url.endsWith("/auth/login") ? { accessToken: token } : [] };
    },
    console: Object.fromEntries(["log", "warn", "error", "info", "debug"].map(method => [method, (...args) => logs.push(args)]))
  });
  await api.login("test@example.invalid", password);
  await api.fetchTrainingSessions(token);
  assert.equal(JSON.parse(calls[0].options.body).password, password);
  assert.equal(calls[1].options.headers.Authorization, `Bearer ${token}`);
  assert.deepEqual(logs, []);
});

test("実APIクライアントの401は認証解除可能な日本語エラーを返す", async () => {
  const api = evaluateModule("./api.ts", {
    "./api-base-url": { API_BASE_URL: "https://api.example.invalid" },
    "./training-session-create": {}, "./user-registration": {}
  }, { fetch: async () => ({ status: 401, ok: false }) });
  await assert.rejects(api.fetchTrainingSessions("TEST_ONLY_TOKEN"), /ログイン/);
});

function loadProxy(environment) {
  return evaluateModule("../proxy.ts", {
    "./lib/api-base-url": { API_BASE_URL: "https://api.example.invalid" },
    "./lib/content-security-policy": { productionContentSecurityPolicy }
  }, { process: { env: { NODE_ENV: environment } }, Headers });
}

test("実Proxyはリクエストnonce/CSP偽装を上書きし同じnonceをSSRと応答へ渡す", () => {
  const { NextRequest } = require("next/server");
  const { proxy } = loadProxy("production");
  const request = new NextRequest("http://localhost/training-sessions", {
    headers: { "x-nonce": "ATTACKER_NONCE", "Content-Security-Policy": "script-src *" }
  });
  const response = proxy(request);
  const csp = response.headers.get("Content-Security-Policy");
  assert.ok(csp);
  assert.doesNotMatch(csp, /ATTACKER|script-src \*/);
  const nonce = response.headers.get("x-middleware-request-x-nonce");
  assert.ok(nonce);
  assert.ok(csp.includes(`'nonce-${nonce}'`));
  assert.equal(response.headers.get("x-middleware-request-content-security-policy"), csp);
  assert.equal(response.headers.get("Cache-Control"), "private, no-store");
  assert.notEqual(proxy(request).headers.get("Content-Security-Policy"), csp);
});

test("開発ProxyにCSP制約を追加せず旧画面redirectを維持", () => {
  const { NextRequest } = require("next/server");
  const { proxy } = loadProxy("development");
  const response = proxy(new NextRequest("http://localhost/register"));
  assert.equal(response.headers.get("Content-Security-Policy"), null);
  const legacy = proxy(new NextRequest("http://localhost/records/new"));
  assert.equal(legacy.status, 307);
  assert.equal(legacy.headers.get("Location"), "http://localhost/training-sessions");
});
