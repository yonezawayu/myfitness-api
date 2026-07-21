"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  DashboardResponse,
  fetchFoods,
  fetchMealItems,
  fetchMealLogs,
  fetchTodayDashboard,
  Food,
  MealItemResponse,
  TOKEN_STORAGE_KEY
} from "@/lib/api";

const cards: Array<{
  key: "totalCalories" | "totalProtein" | "totalFat" | "totalCarbs" | "todayWeight";
  label: string;
  unit: string;
}> = [
  { key: "totalCalories", label: "摂取カロリー", unit: "kcal" },
  { key: "totalProtein", label: "Protein", unit: "g" },
  { key: "totalFat", label: "Fat", unit: "g" },
  { key: "totalCarbs", label: "Carbs", unit: "g" },
  { key: "todayWeight", label: "今日の体重", unit: "kg" }
];

type NutritionSummary = {
  totalCalories: number;
  totalProtein: number;
  totalFat: number;
  totalCarbs: number;
};

function todayString() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function itemQuantity(item: MealItemResponse) {
  return item.quantityG ?? item.quantityGrams ?? item.amountG ?? 0;
}

function itemNutrition(item: MealItemResponse, foods: Food[]) {
  const food = foods.find((currentFood) => currentFood.id === item.foodId);
  const ratio = itemQuantity(item) / 100;

  return {
    calories: item.calories ?? (food ? food.caloriesPer100g * ratio : 0),
    protein: item.protein ?? (food ? food.proteinPer100g * ratio : 0),
    fat: item.fat ?? (food ? food.fatPer100g * ratio : 0),
    carbs: item.carbs ?? (food ? food.carbPer100g * ratio : 0)
  };
}

export default function DashboardPage() {
  const router = useRouter();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [nutritionSummary, setNutritionSummary] = useState<NutritionSummary | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    if (!token) {
      router.replace("/");
      return;
    }

    const authToken = token;

    async function loadDashboard() {
      const [dashboardData, mealLogs, foods] = await Promise.all([
        fetchTodayDashboard(authToken),
        fetchMealLogs(authToken),
        fetchFoods(authToken)
      ]);
      const today = todayString();
      const todayMeals = mealLogs.filter((meal) => meal.date === today);
      const mealItemsByMeal = await Promise.all(
        todayMeals.map(async (meal) => fetchMealItems(authToken, meal.id))
      );
      const items = mealItemsByMeal.flat();
      const itemTotals = items.reduce(
        (totals, item) => {
          const nutrition = itemNutrition(item, foods);

          return {
            totalCalories: totals.totalCalories + nutrition.calories,
            totalProtein: totals.totalProtein + nutrition.protein,
            totalFat: totals.totalFat + nutrition.fat,
            totalCarbs: totals.totalCarbs + nutrition.carbs
          };
        },
        { totalCalories: 0, totalProtein: 0, totalFat: 0, totalCarbs: 0 }
      );

      setDashboard(dashboardData);
      setNutritionSummary({
        totalCalories: dashboardData.totalCalories ?? itemTotals.totalCalories,
        totalProtein: dashboardData.totalProtein ?? itemTotals.totalProtein,
        totalFat: dashboardData.totalFat ?? itemTotals.totalFat,
        totalCarbs: dashboardData.totalCarbs ?? itemTotals.totalCarbs
      });
    }

    loadDashboard()
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

  function cardValue(key: (typeof cards)[number]["key"]) {
    if (key === "totalCalories" || key === "totalProtein" || key === "totalFat" || key === "totalCarbs") {
      return nutritionSummary?.[key] ?? dashboard?.[key] ?? null;
    }

    return dashboard?.[key] ?? null;
  }

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">今日のダッシュボード</h1>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:justify-end">
            <button
              className="w-full rounded-md bg-emerald-700 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-800 sm:w-auto"
              type="button"
              onClick={() => router.push("/records/new")}
            >
              体重記録を追加
            </button>
            <button
              className="w-full rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-950 ring-1 ring-gray-200 transition hover:bg-gray-50 sm:w-auto"
              type="button"
              onClick={() => router.push("/records")}
            >
              体重履歴
            </button>
            <button
              className="w-full rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700 sm:w-auto"
              type="button"
              onClick={() => router.push("/meals/new")}
            >
              食事を追加
            </button>
            <button
              className="w-full rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-950 ring-1 ring-gray-200 transition hover:bg-gray-50 sm:w-auto"
              type="button"
              onClick={() => router.push("/meals")}
            >
              食事履歴
            </button>
            <button
              className="w-full rounded-md bg-gray-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-gray-800 sm:w-auto"
              type="button"
              onClick={() => router.push("/training/new")}
            >
              トレーニング記録を追加
            </button>
            <button
              className="w-full rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-950 ring-1 ring-gray-200 transition hover:bg-gray-50 sm:w-auto"
              type="button"
              onClick={() => router.push("/training")}
            >
              トレーニング履歴
            </button>
            <button
              className="w-full rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-950 ring-1 ring-gray-200 transition hover:bg-gray-50 sm:w-auto"
              type="button"
              onClick={() => router.push("/training-sessions")}
            >
              本格トレーニング履歴
            </button>
            <button
              className="w-full rounded-md bg-white px-4 py-2 text-sm font-medium text-emerald-700 ring-1 ring-emerald-200 transition hover:bg-emerald-50 sm:w-auto"
              type="button"
              onClick={() => router.push("/foods/new")}
            >
              食品を追加
            </button>
            <button
              className="w-full rounded-md bg-white px-4 py-2 text-sm font-medium text-emerald-700 ring-1 ring-emerald-200 transition hover:bg-emerald-50 sm:w-auto"
              type="button"
              onClick={() => router.push("/foods")}
            >
              食品一覧
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
          <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {cards.map((card) => {
              const value = cardValue(card.key);

              return (
                <article key={card.key} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                  <p className="text-sm font-medium text-gray-500">{card.label}</p>
                  <p className="mt-4 flex items-baseline gap-2">
                    <span className="text-3xl font-semibold text-gray-950">
                      {typeof value === "number" ? new Intl.NumberFormat("ja-JP", { maximumFractionDigits: 1 }).format(value) : "-"}
                    </span>
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
