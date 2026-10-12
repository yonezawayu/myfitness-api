"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { TOKEN_STORAGE_KEY } from "@/lib/api";
import { TRAINING_EXERCISE_GROUPS } from "@/lib/training-exercises";

export default function SelectTrainingExercisesPage() {
  const router = useRouter();
  const [selectedExercises, setSelectedExercises] = useState<string[]>([]);

  useEffect(() => {
    if (!localStorage.getItem(TOKEN_STORAGE_KEY)) {
      router.replace("/");
    }
  }, [router]);

  function toggleExercise(exerciseName: string) {
    setSelectedExercises((currentExercises) =>
      currentExercises.includes(exerciseName)
        ? currentExercises.filter((currentExercise) => currentExercise !== exerciseName)
        : [...currentExercises, exerciseName]
    );
  }

  function createTrainingSession() {
    const query = new URLSearchParams();
    selectedExercises.forEach((exerciseName) => query.append("exercise", exerciseName));
    router.push(`/training-sessions/new?${query.toString()}`);
  }

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">トレーニング種目を選択</h1>
            <p className="mt-2 text-sm text-gray-600">実施する順番に種目を選択してください。</p>
          </div>
          <button
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 transition hover:bg-gray-50"
            type="button"
            onClick={() => router.push("/training-sessions")}
          >
            履歴へ戻る
          </button>
        </header>

        <div className="grid gap-5 md:grid-cols-2">
          {TRAINING_EXERCISE_GROUPS.map((group) => (
            <section key={group.bodyPart} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-semibold text-gray-950">{group.bodyPart}</h2>
              <div className="mt-4 space-y-2">
                {group.exercises.map((exerciseName) => {
                  const selectedIndex = selectedExercises.indexOf(exerciseName);
                  const isSelected = selectedIndex >= 0;

                  return (
                    <button
                      key={exerciseName}
                      className={`flex w-full items-center justify-between rounded-md border px-4 py-3 text-left font-medium transition ${
                        isSelected
                          ? "border-emerald-600 bg-emerald-50 text-emerald-900"
                          : "border-gray-200 bg-white text-gray-800 hover:border-emerald-300 hover:bg-emerald-50/50"
                      }`}
                      type="button"
                      onClick={() => toggleExercise(exerciseName)}
                      aria-pressed={isSelected}
                    >
                      <span>{exerciseName}</span>
                      {isSelected ? (
                        <span className="ml-3 flex items-center gap-2 text-sm text-emerald-700">
                          <span className="rounded-full bg-emerald-700 px-2 py-0.5 text-xs font-semibold text-white">
                            {selectedIndex + 1}
                          </span>
                          ✓
                        </span>
                      ) : null}
                    </button>
                  );
                })}
              </div>
            </section>
          ))}
        </div>

        <div className="sticky bottom-4 mt-8 rounded-lg border border-gray-200 bg-white/95 p-4 shadow-lg backdrop-blur">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="font-medium text-gray-800">選択中: {selectedExercises.length}種目</p>
            <button
              className="rounded-md bg-emerald-700 px-5 py-3 font-medium text-white transition hover:bg-emerald-800 disabled:cursor-not-allowed disabled:bg-gray-400"
              type="button"
              onClick={createTrainingSession}
              disabled={selectedExercises.length === 0}
            >
              選択した種目で作成
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}
