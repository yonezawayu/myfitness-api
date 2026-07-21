"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { createMealItem, createMealLog, fetchFoods, Food, TOKEN_STORAGE_KEY } from "@/lib/api";

type SelectedMealItem = {
  id: number;
  food: Food;
  quantityGrams: string;
};

const mealTypes = ["朝食", "昼食", "夕食", "間食"];

let nextSelectedItemId = 1;

function todayString() {
  return new Date().toISOString().slice(0, 10);
}

function nutritionFor(food: Food, quantityGrams: number) {
  const ratio = quantityGrams / 100;

  return {
    calories: food.caloriesPer100g * ratio,
    protein: food.proteinPer100g * ratio,
    fat: food.fatPer100g * ratio,
    carbs: food.carbPer100g * ratio
  };
}

function formatNumber(value: number, maximumFractionDigits = 1) {
  return new Intl.NumberFormat("ja-JP", {
    maximumFractionDigits
  }).format(value);
}

export default function NewMealPage() {
  const router = useRouter();
  const [mealName, setMealName] = useState(mealTypes[0]);
  const [date, setDate] = useState(todayString());
  const [foods, setFoods] = useState<Food[]>([]);
  const [searchText, setSearchText] = useState("");
  const [selectedItems, setSelectedItems] = useState<SelectedMealItem[]>([]);
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
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Food一覧の取得に失敗しました。");
        if (err instanceof Error && err.message.includes("ログイン")) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          router.replace("/");
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [router]);

  const filteredFoods = useMemo(() => {
    const keyword = searchText.trim().toLowerCase();

    if (!keyword) {
      return foods;
    }

    return foods.filter((food) => food.name.toLowerCase().includes(keyword));
  }, [foods, searchText]);

  const totals = useMemo(
    () =>
      selectedItems.reduce(
        (currentTotals, item) => {
          const quantity = Number(item.quantityGrams);
          const nutrition = nutritionFor(item.food, Number.isNaN(quantity) ? 0 : quantity);

          return {
            calories: currentTotals.calories + nutrition.calories,
            protein: currentTotals.protein + nutrition.protein,
            fat: currentTotals.fat + nutrition.fat,
            carbs: currentTotals.carbs + nutrition.carbs
          };
        },
        { calories: 0, protein: 0, fat: 0, carbs: 0 }
      ),
    [selectedItems]
  );

  function handleAddFood(food: Food) {
    setSelectedItems((currentItems) => [
      ...currentItems,
      {
        id: nextSelectedItemId++,
        food,
        quantityGrams: "100"
      }
    ]);
  }

  function updateQuantity(itemId: number, quantityGrams: string) {
    setSelectedItems((currentItems) =>
      currentItems.map((item) => (item.id === itemId ? { ...item, quantityGrams } : item))
    );
  }

  function removeItem(itemId: number) {
    setSelectedItems((currentItems) => currentItems.filter((item) => item.id !== itemId));
  }

  function validateForm() {
    if (!mealName.trim()) {
      return "食事区分を選択してください。";
    }

    if (!date) {
      return "dateを入力してください。";
    }

    if (selectedItems.length === 0) {
      return "Foodを1つ以上追加してください。";
    }

    for (const item of selectedItems) {
      const quantity = Number(item.quantityGrams);

      if (Number.isNaN(quantity) || quantity <= 0) {
        return `${item.food.name} のquantityGramsは0より大きい数値で入力してください。`;
      }
    }

    return "";
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      router.replace("/");
      return;
    }

    const validationMessage = validateForm();
    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setIsSubmitting(true);

    try {
      const mealLog = await createMealLog(token, {
        mealName,
        date
      });

      for (const item of selectedItems) {
        await createMealItem(token, {
          mealLogId: mealLog.id,
          foodId: item.food.id,
          amountG: Number(item.quantityGrams)
        });
      }

      router.push("/meals");
    } catch (err) {
      setError(err instanceof Error ? err.message : "食事の保存に失敗しました。");
      if (err instanceof Error && err.message.includes("ログイン")) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        router.replace("/");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-6xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">食事を追加</h1>
          </div>
          <button
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 transition hover:bg-gray-50"
            type="button"
            onClick={() => router.push("/meals")}
          >
            食事履歴へ戻る
          </button>
        </header>

        <form className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_380px]" onSubmit={handleSubmit}>
          <div className="space-y-5">
            <section className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-semibold text-gray-950">Meal</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <label className="block">
                  <span className="text-sm font-medium text-gray-700">食事区分</span>
                  <select
                    className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                    value={mealName}
                    onChange={(event) => setMealName(event.target.value)}
                    required
                  >
                    {mealTypes.map((mealType) => (
                      <option key={mealType} value={mealType}>
                        {mealType}
                      </option>
                    ))}
                  </select>
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
              </div>
            </section>

            <section className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <h2 className="text-lg font-semibold text-gray-950">Food検索</h2>
                  <p className="mt-1 text-sm text-gray-500">食品を選んでMealに追加します。</p>
                </div>
                <label className="block sm:w-72">
                  <span className="text-sm font-medium text-gray-700">食品名</span>
                  <input
                    className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                    type="search"
                    value={searchText}
                    onChange={(event) => setSearchText(event.target.value)}
                    placeholder="鶏胸肉"
                  />
                </label>
              </div>

              {isLoading ? <p className="mt-5 text-gray-600">Foodを読み込み中...</p> : null}

              {!isLoading && filteredFoods.length === 0 ? (
                <p className="mt-5 rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-600">該当するFoodがありません。</p>
              ) : null}

              {filteredFoods.length > 0 ? (
                <div className="mt-5 grid gap-3 md:grid-cols-2">
                  {filteredFoods.map((food) => (
                    <article key={food.id} className="rounded-md border border-gray-200 p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <h3 className="font-semibold text-gray-950">{food.name}</h3>
                          <p className="mt-1 text-xs text-gray-500">per 100g</p>
                        </div>
                        <button
                          className="rounded-md bg-emerald-700 px-3 py-2 text-sm font-medium text-white transition hover:bg-emerald-800"
                          type="button"
                          onClick={() => handleAddFood(food)}
                        >
                          +
                        </button>
                      </div>
                      <dl className="mt-4 grid grid-cols-4 gap-2 text-sm">
                        <div>
                          <dt className="text-xs font-medium text-gray-500">kcal</dt>
                          <dd className="mt-1 font-semibold text-gray-950">{formatNumber(food.caloriesPer100g)}</dd>
                        </div>
                        <div>
                          <dt className="text-xs font-medium text-gray-500">P</dt>
                          <dd className="mt-1 font-semibold text-gray-950">{formatNumber(food.proteinPer100g)}g</dd>
                        </div>
                        <div>
                          <dt className="text-xs font-medium text-gray-500">F</dt>
                          <dd className="mt-1 font-semibold text-gray-950">{formatNumber(food.fatPer100g)}g</dd>
                        </div>
                        <div>
                          <dt className="text-xs font-medium text-gray-500">C</dt>
                          <dd className="mt-1 font-semibold text-gray-950">{formatNumber(food.carbPer100g)}g</dd>
                        </div>
                      </dl>
                    </article>
                  ))}
                </div>
              ) : null}
            </section>
          </div>

          <aside className="space-y-5">
            <section className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-semibold text-gray-950">Meal Items</h2>

              {selectedItems.length === 0 ? (
                <p className="mt-4 rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-600">Foodを追加してください。</p>
              ) : null}

              {selectedItems.length > 0 ? (
                <div className="mt-4 space-y-3">
                  {selectedItems.map((item) => {
                    const quantity = Number(item.quantityGrams);
                    const nutrition = nutritionFor(item.food, Number.isNaN(quantity) ? 0 : quantity);

                    return (
                      <article key={item.id} className="rounded-md border border-gray-200 p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <h3 className="font-semibold text-gray-950">{item.food.name}</h3>
                            <p className="mt-1 text-sm text-gray-500">
                              {formatNumber(nutrition.calories)}kcal / P{formatNumber(nutrition.protein)}g
                            </p>
                          </div>
                          <button
                            className="rounded-md bg-white px-2 py-1 text-sm font-medium text-red-600 ring-1 ring-red-200 transition hover:bg-red-50"
                            type="button"
                            onClick={() => removeItem(item.id)}
                          >
                            削除
                          </button>
                        </div>

                        <label className="mt-3 block">
                          <span className="text-sm font-medium text-gray-700">quantityGrams</span>
                          <input
                            className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                            type="number"
                            min="0.1"
                            step="0.1"
                            value={item.quantityGrams}
                            onChange={(event) => updateQuantity(item.id, event.target.value)}
                            required
                          />
                        </label>

                        <dl className="mt-3 grid grid-cols-4 gap-2 text-sm">
                          <div>
                            <dt className="text-xs font-medium text-gray-500">kcal</dt>
                            <dd className="mt-1 font-semibold text-gray-950">{formatNumber(nutrition.calories)}</dd>
                          </div>
                          <div>
                            <dt className="text-xs font-medium text-gray-500">P</dt>
                            <dd className="mt-1 font-semibold text-gray-950">{formatNumber(nutrition.protein)}g</dd>
                          </div>
                          <div>
                            <dt className="text-xs font-medium text-gray-500">F</dt>
                            <dd className="mt-1 font-semibold text-gray-950">{formatNumber(nutrition.fat)}g</dd>
                          </div>
                          <div>
                            <dt className="text-xs font-medium text-gray-500">C</dt>
                            <dd className="mt-1 font-semibold text-gray-950">{formatNumber(nutrition.carbs)}g</dd>
                          </div>
                        </dl>
                      </article>
                    );
                  })}
                </div>
              ) : null}
            </section>

            <section className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-semibold text-gray-950">合計</h2>
              <dl className="mt-4 grid grid-cols-2 gap-3">
                <div className="rounded-md bg-gray-50 px-3 py-2">
                  <dt className="text-xs font-medium text-gray-500">kcal</dt>
                  <dd className="mt-1 text-xl font-semibold text-gray-950">{formatNumber(totals.calories)}</dd>
                </div>
                <div className="rounded-md bg-gray-50 px-3 py-2">
                  <dt className="text-xs font-medium text-gray-500">Protein</dt>
                  <dd className="mt-1 text-xl font-semibold text-gray-950">{formatNumber(totals.protein)}g</dd>
                </div>
                <div className="rounded-md bg-gray-50 px-3 py-2">
                  <dt className="text-xs font-medium text-gray-500">Fat</dt>
                  <dd className="mt-1 text-xl font-semibold text-gray-950">{formatNumber(totals.fat)}g</dd>
                </div>
                <div className="rounded-md bg-gray-50 px-3 py-2">
                  <dt className="text-xs font-medium text-gray-500">Carbs</dt>
                  <dd className="mt-1 text-xl font-semibold text-gray-950">{formatNumber(totals.carbs)}g</dd>
                </div>
              </dl>

              {error ? (
                <p className="mt-5 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                  {error}
                </p>
              ) : null}

              <div className="mt-6 flex flex-col gap-3">
                <button
                  className="rounded-md bg-emerald-700 px-4 py-2.5 font-medium text-white transition hover:bg-emerald-800 disabled:cursor-not-allowed disabled:bg-gray-400"
                  type="submit"
                  disabled={isSubmitting || isLoading || selectedItems.length === 0}
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
            </section>
          </aside>
        </form>
      </div>
    </main>
  );
}
