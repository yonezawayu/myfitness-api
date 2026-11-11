import Link from "next/link";

// Avoid the framework's default inline-style error page under style-src-attr 'none'.
export default function NotFound() {
  return (
    <main className="flex min-h-screen items-center justify-center px-5 py-10">
      <section className="rounded-lg border border-gray-200 bg-white p-8 text-center shadow-sm">
        <h1 className="text-2xl font-semibold text-gray-950">404 ページが見つかりません</h1>
        <Link className="mt-5 inline-block font-medium text-emerald-700" href="/">
          ログイン画面へ戻る
        </Link>
      </section>
    </main>
  );
}
