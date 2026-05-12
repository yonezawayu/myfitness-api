"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  fetchFoods,
  fetchMealItems,
  fetchMealLogs,
  Food,
  MealItemResponse,
  MealLogResponse,
  TOKEN_STORAGE_KEY
} from "@/lib/api";

type MealWithItems = MealLogResponse & {
  items: MealItemResponse[];
};

export default function MealsPage() {
  const router = useRouter();
  const [meals, setMeals] = useState<MealWithItems[]>([]);
  const [foods, setFoods] = useState<Food[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    if (!token) {
      router.replace("/");
      return;
    }

    const authToken = token;

    async function loadMeals() {
      try {
        const [mealLogs, foodList] = await Promise.all([fetchMealLogs(authToken), fetchFoods(authToken)]);
        const mealsWithItems = await Promise.all(
          mealLogs.map(async (meal) => ({
            ...meal,
            items: await fetchMealItems(authToken, meal.id)
          }))
        );

        setFoods(foodList);
        setMeals(mealsWithItems);
      } catch (err) {
        setError(err instanceof Error ? err.message : "食事履歴の取得に失敗しました。");
        if (err instanceof Error && err.message.includes("ログイン")) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
        }
      } finally {
        setIsLoading(false);
      }
    }

    loadMeals();
  }, [router]);

  function foodName(foodId: number) {
    return foods.find((food) => food.id === foodId)?.name ?? `Food #${foodId}`;
  }

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">食事履歴</h1>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              className="rounded-md bg-emerald-700 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-800"
              type="button"
              onClick={() => router.push("/meals/new")}
            >
              食事を追加
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

        {!isLoading && !error && meals.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white p-6 text-gray-600 shadow-sm">食事履歴がありません。</div>
        ) : null}

        {meals.length > 0 ? (
          <section className="space-y-4">
            {meals.map((meal) => (
              <article key={meal.id} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
                  <h2 className="text-lg font-semibold text-gray-950">{meal.mealName}</h2>
                  <p className="text-sm text-gray-500">{meal.date}</p>
                </div>

                {meal.items.length > 0 ? (
                  <div className="mt-4 overflow-hidden rounded-md border border-gray-200">
                    <table className="w-full text-left text-sm">
                      <thead className="bg-gray-50 text-gray-600">
                        <tr>
                          <th className="px-3 py-2 font-medium">food</th>
                          <th className="px-3 py-2 font-medium">amountG</th>
                          <th className="px-3 py-2 font-medium">calories</th>
                          <th className="px-3 py-2 font-medium">protein</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100">
                        {meal.items.map((item) => (
                          <tr key={item.id}>
                            <td className="px-3 py-2 text-gray-950">{foodName(item.foodId)}</td>
                            <td className="px-3 py-2 text-gray-700">{item.quantityG}</td>
                            <td className="px-3 py-2 text-gray-700">{item.calories}</td>
                            <td className="px-3 py-2 text-gray-700">{item.protein}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="mt-4 rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-600">食品明細がありません。</p>
                )}
              </article>
            ))}
          </section>
        ) : null}
      </div>
    </main>
  );
}
