import assert from "node:assert/strict";
import test from "node:test";

import {
  networkRegistrationError,
  registrationErrorForStatus,
  validateRegistration
} from "./user-registration.ts";

test("正常な登録入力を正規化する", () => {
  const result = validateRegistration({
    email: "  user@example.com  ",
    password: "password123",
    passwordConfirmation: "password123"
  });

  assert.equal(result.email, "user@example.com");
  assert.deepEqual(result.errors, {});
});

test("必須入力を検証する", () => {
  const result = validateRegistration({ email: "", password: "", passwordConfirmation: "" });

  assert.deepEqual(Object.keys(result.errors).sort(), ["email", "password", "passwordConfirmation"]);
});

test("メール形式とパスワード文字数を検証する", () => {
  const result = validateRegistration({
    email: "invalid-email",
    password: "short",
    passwordConfirmation: "short"
  });

  assert.match(result.errors.email ?? "", /有効なメールアドレス/);
  assert.match(result.errors.password ?? "", /8文字以上72文字以内/);
});

test("パスワード不一致を検証する", () => {
  const result = validateRegistration({
    email: "user@example.com",
    password: "password123",
    passwordConfirmation: "different123"
  });

  assert.match(result.errors.passwordConfirmation ?? "", /一致しません/);
});

test("メール重複を利用者向けエラーへ変換する", () => {
  const error = registrationErrorForStatus(409);

  assert.equal(error?.code, "duplicate-email");
  assert.match(error?.message ?? "", /既に登録/);
});

test("入力不正とサーバーエラーを利用者向けメッセージへ変換する", () => {
  const validationError = registrationErrorForStatus(400);
  const serverError = registrationErrorForStatus(500);

  assert.equal(validationError?.code, "validation");
  assert.match(validationError?.message ?? "", /入力内容/);
  assert.equal(serverError?.code, "server");
  assert.match(serverError?.message ?? "", /時間をおいて/);
});

test("通信失敗を利用者向けエラーへ変換する", () => {
  const error = networkRegistrationError();

  assert.equal(error.code, "network");
  assert.match(error.message, /通信環境/);
});
