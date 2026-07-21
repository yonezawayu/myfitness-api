"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  fetchFoods,
  fetchMealItems,
  fetchMealLogs,
  deleteMealLog,
  Food,
  MealItemResponse,
  MealLogResponse,
  TOKEN_STORAGE_KEY
} from "@/lib/api";

type MealWithItems = MealLogResponse & {
  items: MealItemResponse[];
};

type NutritionSummary = {
  calories: number;
  protein: number;
  fat: number;
  carbs: number;
};

function formatNumber(value: number, maximumFractionDigits = 1) {
  return new Intl.NumberFormat("ja-JP", {
    maximumFractionDigits
  }).format(value);
}

export default function MealsPage() {
  const router = useRouter();
  const [meals, setMeals] = useState<MealWithItems[]>([]);
  const [foods, setFoods] = useState<Food[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [deletingMealId, setDeletingMealId] = useState<number | null>(null);

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

  function itemQuantity(item: MealItemResponse) {
    return item.quantityG ?? item.quantityGrams ?? item.amountG ?? 0;
  }

  function itemNutrition(item: MealItemResponse): NutritionSummary {
    const food = foods.find((currentFood) => currentFood.id === item.foodId);
    const quantity = itemQuantity(item);
    const ratio = quantity / 100;

    return {
      calories: item.calories ?? (food ? food.caloriesPer100g * ratio : 0),
      protein: item.protein ?? (food ? food.proteinPer100g * ratio : 0),
      fat: item.fat ?? (food ? food.fatPer100g * ratio : 0),
      carbs: item.carbs ?? (food ? food.carbPer100g * ratio : 0)
    };
  }

  function mealSummary(meal: MealWithItems): NutritionSummary {
    const itemTotals = meal.items.reduce(
      (totals, item) => {
        const nutrition = itemNutrition(item);

        return {
          calories: totals.calories + nutrition.calories,
          protein: totals.protein + nutrition.protein,
          fat: totals.fat + nutrition.fat,
          carbs: totals.carbs + nutrition.carbs
        };
      },
      { calories: 0, protein: 0, fat: 0, carbs: 0 }
    );

    return {
      calories: meal.totalCalories ?? itemTotals.calories,
      protein: meal.totalProtein ?? itemTotals.protein,
      fat: meal.totalFat ?? itemTotals.fat,
      carbs: meal.totalCarbs ?? itemTotals.carbs
    };
  }

  async function handleDelete(mealId: number) {
    if (!window.confirm("削除しますか？")) {
      return;
    }

    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      router.replace("/");
      return;
    }

    setError("");
    setDeletingMealId(mealId);

    try {
      await deleteMealLog(token, mealId);
      setMeals((currentMeals) => currentMeals.filter((meal) => meal.id !== mealId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Mealの削除に失敗しました。");
      if (err instanceof Error && err.message.includes("ログイン")) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      }
    } finally {
      setDeletingMealId(null);
    }
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
            {meals.map((meal) => {
              const summary = mealSummary(meal);

              return (
                <article key={meal.id} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <h2 className="text-lg font-semibold text-gray-950">{meal.mealName}</h2>
                      <p className="mt-1 text-sm text-gray-500">{meal.date}</p>
                    </div>
                    <button
                      className="rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:bg-gray-400"
                      type="button"
                      onClick={() => handleDelete(meal.id)}
                      disabled={deletingMealId === meal.id}
                    >
                      {deletingMealId === meal.id ? "削除中..." : "削除"}
                    </button>
                  </div>

                  <dl className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
                    <div className="rounded-md bg-gray-50 px-3 py-2">
                      <dt className="text-xs font-medium text-gray-500">totalCalories</dt>
                      <dd className="mt-1 text-xl font-semibold text-gray-950">{formatNumber(summary.calories)}</dd>
                    </div>
                    <div className="rounded-md bg-gray-50 px-3 py-2">
                      <dt className="text-xs font-medium text-gray-500">totalProtein</dt>
                      <dd className="mt-1 text-xl font-semibold text-gray-950">{formatNumber(summary.protein)}g</dd>
                    </div>
                    <div className="rounded-md bg-gray-50 px-3 py-2">
                      <dt className="text-xs font-medium text-gray-500">totalFat</dt>
                      <dd className="mt-1 text-xl font-semibold text-gray-950">{formatNumber(summary.fat)}g</dd>
                    </div>
                    <div className="rounded-md bg-gray-50 px-3 py-2">
                      <dt className="text-xs font-medium text-gray-500">totalCarbs</dt>
                      <dd className="mt-1 text-xl font-semibold text-gray-950">{formatNumber(summary.carbs)}g</dd>
                    </div>
                  </dl>

                  {meal.items.length > 0 ? (
                    <div className="mt-4 overflow-x-auto rounded-md border border-gray-200">
                      <table className="w-full min-w-[720px] text-left text-sm">
                        <thead className="bg-gray-50 text-gray-600">
                          <tr>
                            <th className="px-3 py-2 font-medium">food</th>
                            <th className="px-3 py-2 font-medium">quantityGrams</th>
                            <th className="px-3 py-2 font-medium">calories</th>
                            <th className="px-3 py-2 font-medium">protein</th>
                            <th className="px-3 py-2 font-medium">fat</th>
                            <th className="px-3 py-2 font-medium">carbs</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                          {meal.items.map((item) => {
                            const nutrition = itemNutrition(item);

                            return (
                              <tr key={item.id}>
                                <td className="px-3 py-2 text-gray-950">{foodName(item.foodId)}</td>
                                <td className="px-3 py-2 text-gray-700">{formatNumber(itemQuantity(item))}g</td>
                                <td className="px-3 py-2 text-gray-700">{formatNumber(nutrition.calories)}</td>
                                <td className="px-3 py-2 text-gray-700">{formatNumber(nutrition.protein)}g</td>
                                <td className="px-3 py-2 text-gray-700">{formatNumber(nutrition.fat)}g</td>
                                <td className="px-3 py-2 text-gray-700">{formatNumber(nutrition.carbs)}g</td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <p className="mt-4 rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-600">食品明細がありません。</p>
                  )}
                </article>
              );
            })}
          </section>
        ) : null}
      </div>
    </main>
  );
}
