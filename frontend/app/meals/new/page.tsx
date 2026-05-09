"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { createMealItem, createMealLog, fetchFoods, Food, TOKEN_STORAGE_KEY } from "@/lib/api";

function todayString() {
  return new Date().toISOString().slice(0, 10);
}

export default function NewMealPage() {
  const router = useRouter();
  const [mealName, setMealName] = useState("");
  const [date, setDate] = useState(todayString());
  const [foodId, setFoodId] = useState("");
  const [amountG, setAmountG] = useState("");
  const [foods, setFoods] = useState<Food[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    if (!token) {
      router.replace("/");
      return;
    }

    fetchFoods(token)
      .then((data) => {
        setFoods(data);
        if (data.length > 0) {
          setFoodId(String(data[0].id));
        }
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

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      router.replace("/");
      return;
    }

    const selectedFoodId = Number(foodId);
    const amountValue = Number(amountG);

    if (!mealName.trim()) {
      setError("mealNameを入力してください。");
      return;
    }

    if (!date) {
      setError("dateを入力してください。");
      return;
    }

    if (!selectedFoodId || Number.isNaN(selectedFoodId)) {
      setError("foodを選択してください。");
      return;
    }

    if (!amountValue || amountValue <= 0 || Number.isNaN(amountValue)) {
      setError("amountGは0より大きい数値で入力してください。");
      return;
    }

    setIsSubmitting(true);

    try {
      const mealLog = await createMealLog(token, {
        mealName: mealName.trim(),
        date
      });

      await createMealItem(token, {
        mealLogId: mealLog.id,
        foodId: selectedFoodId,
        amountG: amountValue
      });

      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "食事の保存に失敗しました。");
      if (err instanceof Error && err.message.includes("ログイン")) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-xl">
        <header className="mb-8 border-b border-gray-200 pb-6">
          <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
          <h1 className="mt-2 text-2xl font-semibold text-gray-950">食事を追加</h1>
        </header>

        <form className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm" onSubmit={handleSubmit}>
          <div className="space-y-5">
            <label className="block">
              <span className="text-sm font-medium text-gray-700">mealName</span>
              <input
                className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                type="text"
                value={mealName}
                onChange={(event) => setMealName(event.target.value)}
                required
              />
            </label>

            <label className="block">
              <span className="text-sm font-medium text-gray-700">date</span>
              <input
                className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                type="date"
                value={date}
                onChange={(event) => setDate(event.target.value)}
                required
              />
            </label>

            <label className="block">
              <span className="text-sm font-medium text-gray-700">food</span>
              <select
                className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                value={foodId}
                onChange={(event) => setFoodId(event.target.value)}
                required
                disabled={isLoading || foods.length === 0}
              >
                {foods.length === 0 ? <option value="">Foodがありません</option> : null}
                {foods.map((food) => (
                  <option key={food.id} value={food.id}>
                    {food.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="block">
              <span className="text-sm font-medium text-gray-700">amountG</span>
              <input
                className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                type="number"
                min="0.1"
                step="0.1"
                value={amountG}
                onChange={(event) => setAmountG(event.target.value)}
                required
              />
            </label>
          </div>

          {error ? (
            <p className="mt-5 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
          ) : null}

          <div className="mt-6 flex flex-col gap-3 sm:flex-row">
            <button
              className="rounded-md bg-emerald-700 px-4 py-2.5 font-medium text-white transition hover:bg-emerald-800 disabled:cursor-not-allowed disabled:bg-gray-400"
              type="submit"
              disabled={isSubmitting || isLoading || foods.length === 0}
            >
              {isSubmitting ? "保存中..." : "保存"}
            </button>
            <button
              className="rounded-md border border-gray-300 bg-white px-4 py-2.5 font-medium text-gray-800 transition hover:bg-gray-50"
              type="button"
              onClick={() => router.push("/dashboard")}
            >
              キャンセル
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}
