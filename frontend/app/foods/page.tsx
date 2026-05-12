"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchFoods, Food, TOKEN_STORAGE_KEY } from "@/lib/api";

export default function FoodsPage() {
  const router = useRouter();
  const [foods, setFoods] = useState<Food[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    if (!token) {
      router.replace("/");
      return;
    }

    fetchFoods(token)
      .then((data) => {
        setFoods(data);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Food一覧の取得に失敗しました。");
        if (err instanceof Error && err.message.includes("ログイン")) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [router]);

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">食品一覧</h1>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              className="rounded-md bg-emerald-700 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-800"
              type="button"
              onClick={() => router.push("/foods/new")}
            >
              食品を追加
            </button>
            <button
              className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 transition hover:bg-gray-50"
              type="button"
              onClick={() => router.push("/dashboard")}
            >
              Dashboardへ戻る
            </button>
          </div>
        </header>

        {isLoading ? <p className="text-gray-600">読み込み中...</p> : null}

        {error ? (
          <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-red-700">{error}</div>
        ) : null}

        {!isLoading && !error && foods.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white p-6 text-gray-600 shadow-sm">食品がありません。</div>
        ) : null}

        {foods.length > 0 ? (
          <section className="grid gap-4 md:grid-cols-2">
            {foods.map((food) => (
              <article key={food.id} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                <h2 className="text-lg font-semibold text-gray-950">{food.name}</h2>
                <dl className="mt-4 grid grid-cols-2 gap-4">
                  <div>
                    <dt className="text-sm font-medium text-gray-500">caloriesPer100g</dt>
                    <dd className="mt-1 text-2xl font-semibold text-gray-950">{food.caloriesPer100g}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500">proteinPer100g</dt>
                    <dd className="mt-1 text-2xl font-semibold text-gray-950">{food.proteinPer100g}</dd>
                  </div>
                </dl>
              </article>
            ))}
          </section>
        ) : null}
      </div>
    </main>
  );
}
