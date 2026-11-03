"use client";

import Link from "next/link";
import { FormEvent, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { registerUser } from "@/lib/api";
import {
  RegistrationFieldErrors,
  validateRegistration
} from "@/lib/user-registration";

const inputClassName =
  "mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100";

export default function RegisterPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [showPasswords, setShowPasswords] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<RegistrationFieldErrors>({});
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const submittingRef = useRef(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (submittingRef.current) {
      return;
    }

    setError("");
    const validation = validateRegistration({ email, password, passwordConfirmation });
    setFieldErrors(validation.errors);

    if (Object.keys(validation.errors).length > 0) {
      return;
    }

    submittingRef.current = true;
    setIsSubmitting(true);

    try {
      await registerUser(validation.email, validation.password);
      router.push("/?registered=1");
    } catch (err) {
      setError(err instanceof Error ? err.message : "ユーザー登録に失敗しました。もう一度お試しください。");
    } finally {
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-5 py-10">
      <section className="w-full max-w-md rounded-lg border border-gray-200 bg-white p-8 shadow-sm">
        <div className="mb-8">
          <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
          <h1 className="mt-2 text-2xl font-semibold text-gray-950">新規ユーザー登録</h1>
          <p className="mt-2 text-sm text-gray-600">筋トレ記録を始めるためのアカウントを作成します。</p>
        </div>

        <form className="space-y-5" onSubmit={handleSubmit} noValidate>
          <label className="block">
            <span className="text-sm font-medium text-gray-700">メールアドレス</span>
            <input
              className={inputClassName}
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              maxLength={255}
              autoComplete="email"
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? "register-email-error" : undefined}
            />
            {fieldErrors.email ? (
              <span id="register-email-error" className="mt-1 block text-sm text-red-700">
                {fieldErrors.email}
              </span>
            ) : null}
          </label>

          <label className="block">
            <span className="text-sm font-medium text-gray-700">パスワード</span>
            <input
              className={inputClassName}
              type={showPasswords ? "text" : "password"}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? "register-password-error" : "register-password-hint"}
            />
            <span id="register-password-hint" className="mt-1 block text-xs text-gray-500">
              8文字以上72文字以内で入力してください。
            </span>
            {fieldErrors.password ? (
              <span id="register-password-error" className="mt-1 block text-sm text-red-700">
                {fieldErrors.password}
              </span>
            ) : null}
          </label>

          <label className="block">
            <span className="text-sm font-medium text-gray-700">パスワード（確認）</span>
            <input
              className={inputClassName}
              type={showPasswords ? "text" : "password"}
              value={passwordConfirmation}
              onChange={(event) => setPasswordConfirmation(event.target.value)}
              required
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              aria-invalid={Boolean(fieldErrors.passwordConfirmation)}
              aria-describedby={fieldErrors.passwordConfirmation ? "register-confirmation-error" : undefined}
            />
            {fieldErrors.passwordConfirmation ? (
              <span id="register-confirmation-error" className="mt-1 block text-sm text-red-700">
                {fieldErrors.passwordConfirmation}
              </span>
            ) : null}
          </label>

          <button
            className="text-sm font-medium text-emerald-700 hover:text-emerald-800"
            type="button"
            onClick={() => setShowPasswords((current) => !current)}
            aria-pressed={showPasswords}
          >
            パスワードを{showPasswords ? "非表示" : "表示"}
          </button>

          {error ? (
            <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {error}
            </p>
          ) : null}

          <button
            className="w-full rounded-md bg-emerald-700 px-4 py-2.5 font-medium text-white transition hover:bg-emerald-800 disabled:cursor-not-allowed disabled:bg-gray-400"
            type="submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? "登録中..." : "登録する"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-600">
          すでにアカウントをお持ちですか？{" "}
          <Link className="font-medium text-emerald-700 hover:text-emerald-800" href="/">
            ログイン
          </Link>
        </p>
      </section>
    </main>
  );
}
