"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { DashboardResponse, fetchTodayDashboard, TOKEN_STORAGE_KEY } from "@/lib/api";

const cards: Array<{
  key: keyof DashboardResponse;
  label: string;
  unit: string;
}> = [
  { key: "totalCalories", label: "摂取カロリー", unit: "kcal" },
  { key: "totalProtein", label: "タンパク質", unit: "g" },
  { key: "todayWeight", label: "今日の体重", unit: "kg" },
  { key: "totalTrainingCalories", label: "消費カロリー", unit: "kcal" }
];

export default function DashboardPage() {
  const router = useRouter();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    if (!token) {
      router.replace("/");
      return;
    }

    fetchTodayDashboard(token)
      .then((data) => {
        setDashboard(data);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "ダッシュボードの取得に失敗しました。");
        if (err instanceof Error && err.message.includes("ログイン")) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [router]);

  function handleLogout() {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    router.push("/");
  }

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">今日のダッシュボード</h1>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              className="w-full rounded-md bg-emerald-700 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-800 sm:w-auto"
              type="button"
              onClick={() => router.push("/records/new")}
            >
              体重記録を追加
            </button>
            <button
              className="w-full rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700 sm:w-auto"
              type="button"
              onClick={() => router.push("/meals/new")}
            >
              食事を追加
            </button>
            <button
              className="w-full rounded-md bg-gray-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-gray-800 sm:w-auto"
              type="button"
              onClick={() => router.push("/training/new")}
            >
              トレーニング記録を追加
            </button>
            <button
              className="w-full rounded-md bg-white px-4 py-2 text-sm font-medium text-emerald-700 ring-1 ring-emerald-200 transition hover:bg-emerald-50 sm:w-auto"
              type="button"
              onClick={() => router.push("/foods/new")}
            >
              食品を追加
            </button>
            <button
              className="w-full rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 transition hover:bg-gray-50 sm:w-auto"
              type="button"
              onClick={handleLogout}
            >
              ログアウト
            </button>
          </div>
        </header>

        {isLoading ? <p className="text-gray-600">読み込み中...</p> : null}

        {error ? (
          <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-red-700">{error}</div>
        ) : null}

        {dashboard ? (
          <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {cards.map((card) => {
              const value = dashboard[card.key];

              return (
                <article key={card.key} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                  <p className="text-sm font-medium text-gray-500">{card.label}</p>
                  <p className="mt-4 flex items-baseline gap-2">
                    <span className="text-3xl font-semibold text-gray-950">{value ?? "-"}</span>
                    <span className="text-sm text-gray-500">{card.unit}</span>
                  </p>
                </article>
              );
            })}
          </section>
        ) : null}
      </div>
    </main>
  );
}
