"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createTrainingExercise,
  createTrainingSession,
  createTrainingSet,
  TOKEN_STORAGE_KEY
} from "@/lib/api";

type SetForm = {
  id: number;
  setNumber: string;
  weight: string;
  reps: string;
};

type ExerciseForm = {
  id: number;
  exerciseName: string;
  sets: SetForm[];
};

let nextFormId = 1;

function createSetForm(setNumber: number): SetForm {
  return {
    id: nextFormId++,
    setNumber: String(setNumber),
    weight: "",
    reps: ""
  };
}

function createExerciseForm(): ExerciseForm {
  return {
    id: nextFormId++,
    exerciseName: "",
    sets: [createSetForm(1)]
  };
}

function todayString() {
  return new Date().toISOString().slice(0, 10);
}

export default function NewTrainingSessionPage() {
  const router = useRouter();
  const [date, setDate] = useState(todayString());
  const [memo, setMemo] = useState("");
  const [exercises, setExercises] = useState<ExerciseForm[]>([createExerciseForm()]);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateExerciseName(exerciseId: number, exerciseName: string) {
    setExercises((currentExercises) =>
      currentExercises.map((exercise) => (exercise.id === exerciseId ? { ...exercise, exerciseName } : exercise))
    );
  }

  function updateSet(exerciseId: number, setId: number, field: keyof Omit<SetForm, "id">, value: string) {
    setExercises((currentExercises) =>
      currentExercises.map((exercise) => {
        if (exercise.id !== exerciseId) {
          return exercise;
        }

        return {
          ...exercise,
          sets: exercise.sets.map((trainingSet) =>
            trainingSet.id === setId ? { ...trainingSet, [field]: value } : trainingSet
          )
        };
      })
    );
  }

  function addSet(exerciseId: number) {
    setExercises((currentExercises) =>
      currentExercises.map((exercise) => {
        if (exercise.id !== exerciseId) {
          return exercise;
        }

        return {
          ...exercise,
          sets: [...exercise.sets, createSetForm(exercise.sets.length + 1)]
        };
      })
    );
  }

  function addExercise() {
    setExercises((currentExercises) => [...currentExercises, createExerciseForm()]);
  }

  function validateForm() {
    if (!date) {
      return "dateを入力してください。";
    }

    for (const exercise of exercises) {
      if (!exercise.exerciseName.trim()) {
        return "exerciseNameを入力してください。";
      }

      for (const trainingSet of exercise.sets) {
        const setNumber = Number(trainingSet.setNumber);
        const weight = Number(trainingSet.weight);
        const reps = Number(trainingSet.reps);

        if (!Number.isInteger(setNumber) || setNumber < 1) {
          return "setNumberは1以上の整数で入力してください。";
        }

        if (Number.isNaN(weight) || weight < 0) {
          return "weightは0以上の数値で入力してください。";
        }

        if (!Number.isInteger(reps) || reps < 1) {
          return "repsは1以上の整数で入力してください。";
        }
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
      const createdSession = await createTrainingSession(token, {
        date,
        memo: memo.trim() || null
      });

      for (const exercise of exercises) {
        const createdExercise = await createTrainingExercise(token, createdSession.id, {
          exerciseName: exercise.exerciseName.trim(),
          memo: null
        });

        for (const trainingSet of exercise.sets) {
          await createTrainingSet(token, createdSession.id, createdExercise.id, {
            setNumber: Number(trainingSet.setNumber),
            weightKg: Number(trainingSet.weight),
            reps: Number(trainingSet.reps),
            memo: null
          });
        }
      }

      router.push("/training-sessions");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Training Sessionの保存に失敗しました。");
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
      <div className="mx-auto max-w-4xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">Training Session作成</h1>
          </div>
          <button
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 transition hover:bg-gray-50"
            type="button"
            onClick={() => router.push("/training-sessions")}
          >
            履歴へ戻る
          </button>
        </header>

        <form className="space-y-5" onSubmit={handleSubmit}>
          <section className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-950">Session</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
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
                <span className="text-sm font-medium text-gray-700">memo</span>
                <input
                  className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                  type="text"
                  value={memo}
                  onChange={(event) => setMemo(event.target.value)}
                />
              </label>
            </div>
          </section>

          {exercises.map((exercise, exerciseIndex) => (
            <section key={exercise.id} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <label className="block flex-1">
                  <span className="text-sm font-medium text-gray-700">exerciseName</span>
                  <input
                    className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                    type="text"
                    value={exercise.exerciseName}
                    onChange={(event) => updateExerciseName(exercise.id, event.target.value)}
                    placeholder={exerciseIndex === 0 ? "Bench Press" : ""}
                    required
                  />
                </label>
                <button
                  className="rounded-md bg-gray-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-gray-800"
                  type="button"
                  onClick={() => addSet(exercise.id)}
                >
                  + セット追加
                </button>
              </div>

              <div className="mt-4 overflow-x-auto">
                <table className="w-full min-w-[620px] text-left text-sm">
                  <thead className="text-gray-500">
                    <tr>
                      <th className="px-3 py-2 font-medium">setNumber</th>
                      <th className="px-3 py-2 font-medium">weight</th>
                      <th className="px-3 py-2 font-medium">reps</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {exercise.sets.map((trainingSet) => (
                      <tr key={trainingSet.id}>
                        <td className="px-3 py-2">
                          <input
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                            type="number"
                            min="1"
                            step="1"
                            value={trainingSet.setNumber}
                            onChange={(event) => updateSet(exercise.id, trainingSet.id, "setNumber", event.target.value)}
                            required
                          />
                        </td>
                        <td className="px-3 py-2">
                          <input
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                            type="number"
                            min="0"
                            step="0.5"
                            value={trainingSet.weight}
                            onChange={(event) => updateSet(exercise.id, trainingSet.id, "weight", event.target.value)}
                            placeholder={exerciseIndex === 0 ? "107.5" : ""}
                            required
                          />
                        </td>
                        <td className="px-3 py-2">
                          <input
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                            type="number"
                            min="1"
                            step="1"
                            value={trainingSet.reps}
                            onChange={(event) => updateSet(exercise.id, trainingSet.id, "reps", event.target.value)}
                            placeholder={exerciseIndex === 0 ? "5" : ""}
                            required
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          ))}

          {error ? (
            <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
          ) : null}

          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              className="rounded-md bg-white px-4 py-2.5 font-medium text-gray-950 ring-1 ring-gray-200 transition hover:bg-gray-50"
              type="button"
              onClick={addExercise}
            >
              + 種目追加
            </button>
            <button
              className="rounded-md bg-emerald-700 px-4 py-2.5 font-medium text-white transition hover:bg-emerald-800 disabled:cursor-not-allowed disabled:bg-gray-400"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? "保存中..." : "保存"}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}
