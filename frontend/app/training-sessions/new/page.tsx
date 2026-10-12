"use client";

import { FormEvent, Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  createTrainingExercise,
  createTrainingSession,
  createTrainingSet,
  TOKEN_STORAGE_KEY
} from "@/lib/api";
import { TRAINING_EXERCISES } from "@/lib/training-exercises";

type SetForm = {
  id: number;
  weight: string;
  reps: string;
};

type RestTimerStatus = "idle" | "running" | "paused" | "completed";

type ExerciseForm = {
  id: number;
  exerciseName: string;
  canSelectExercise: boolean;
  sets: SetForm[];
};

let nextFormId = 1;

const REST_TIMER_PRESETS = [60, 90, 120, 180];

function createSetForm(): SetForm {
  return {
    id: nextFormId++,
    weight: "",
    reps: ""
  };
}

function createExerciseForm(exerciseName: string, canSelectExercise = false): ExerciseForm {
  return {
    id: nextFormId++,
    exerciseName,
    canSelectExercise,
    sets: [createSetForm()]
  };
}

function todayString() {
  return new Date().toISOString().slice(0, 10);
}

export default function NewTrainingSessionPage() {
  return (
    <Suspense fallback={<p className="px-5 py-8 text-gray-600">読み込み中...</p>}>
      <NewTrainingSessionForm />
    </Suspense>
  );
}

function NewTrainingSessionForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const selectedExerciseNames = Array.from(
    new Set(searchParams.getAll("exercise").map((exerciseName) => exerciseName.trim()).filter(Boolean))
  );
  const [date, setDate] = useState(todayString());
  const [memo, setMemo] = useState("");
  const [exercises, setExercises] = useState<ExerciseForm[]>(() =>
    selectedExerciseNames.map((exerciseName) => createExerciseForm(exerciseName))
  );
  const [invalidExerciseIds, setInvalidExerciseIds] = useState<Set<number>>(new Set());
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [restDuration, setRestDuration] = useState(90);
  const [remainingSeconds, setRemainingSeconds] = useState(90);
  const [restTimerStatus, setRestTimerStatus] = useState<RestTimerStatus>("idle");
  const restTimerEndTimeRef = useRef<number | null>(null);

  useEffect(() => {
    if (selectedExerciseNames.length === 0) {
      router.replace("/training-sessions/select-exercises");
    }
  }, [router, selectedExerciseNames.length]);

  useEffect(() => {
    if (restTimerStatus !== "running") {
      return;
    }

    function updateRemainingTime() {
      if (restTimerEndTimeRef.current === null) {
        return;
      }

      const nextRemainingSeconds = Math.max(
        0,
        Math.ceil((restTimerEndTimeRef.current - Date.now()) / 1000)
      );
      setRemainingSeconds(nextRemainingSeconds);

      if (nextRemainingSeconds === 0) {
        restTimerEndTimeRef.current = null;
        setRestTimerStatus("completed");
      }
    }

    updateRemainingTime();
    const intervalId = setInterval(updateRemainingTime, 250);
    document.addEventListener("visibilitychange", updateRemainingTime);

    return () => {
      clearInterval(intervalId);
      document.removeEventListener("visibilitychange", updateRemainingTime);
    };
  }, [restTimerStatus]);

  function selectRestDuration(seconds: number) {
    setRestDuration(seconds);

    if (restTimerStatus === "idle" || restTimerStatus === "completed") {
      restTimerEndTimeRef.current = null;
      setRemainingSeconds(seconds);
      setRestTimerStatus("idle");
    }
  }

  function startRestTimer() {
    const secondsToStart = remainingSeconds > 0 ? remainingSeconds : restDuration;
    restTimerEndTimeRef.current = Date.now() + secondsToStart * 1000;
    setRemainingSeconds(secondsToStart);
    setRestTimerStatus("running");
  }

  function pauseRestTimer() {
    if (restTimerEndTimeRef.current === null) {
      return;
    }

    const nextRemainingSeconds = Math.max(
      0,
      Math.ceil((restTimerEndTimeRef.current - Date.now()) / 1000)
    );
    restTimerEndTimeRef.current = null;
    setRemainingSeconds(nextRemainingSeconds);
    setRestTimerStatus(nextRemainingSeconds === 0 ? "completed" : "paused");
  }

  function resumeRestTimer() {
    if (remainingSeconds <= 0) {
      return;
    }

    restTimerEndTimeRef.current = Date.now() + remainingSeconds * 1000;
    setRestTimerStatus("running");
  }

  function resetRestTimer() {
    restTimerEndTimeRef.current = null;
    setRemainingSeconds(restDuration);
    setRestTimerStatus("idle");
  }

  function addRestTime() {
    if (restTimerStatus === "running" && restTimerEndTimeRef.current !== null) {
      restTimerEndTimeRef.current += 30_000;
      setRemainingSeconds(
        Math.max(0, Math.ceil((restTimerEndTimeRef.current - Date.now()) / 1000))
      );
      return;
    }

    setRemainingSeconds((currentSeconds) => currentSeconds + 30);
    if (restTimerStatus === "completed") {
      setRestTimerStatus("paused");
    }
  }

  const restTimerMinutes = Math.floor(remainingSeconds / 60);
  const restTimerSeconds = remainingSeconds % 60;
  const formattedRestTime = `${String(restTimerMinutes).padStart(2, "0")}:${String(restTimerSeconds).padStart(2, "0")}`;

  function updateExerciseName(exerciseId: number, exerciseName: string) {
    setExercises((currentExercises) =>
      currentExercises.map((exercise) => (exercise.id === exerciseId ? { ...exercise, exerciseName } : exercise))
    );

    if (exerciseName.trim()) {
      setInvalidExerciseIds((currentIds) => {
        if (!currentIds.has(exerciseId)) {
          return currentIds;
        }

        const nextIds = new Set(currentIds);
        nextIds.delete(exerciseId);
        return nextIds;
      });
    }
  }

  function updateSet(exerciseId: number, setId: number, field: "weight" | "reps", value: string) {
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

  function copyPreviousSetValue(exerciseId: number, setIndex: number, field: "weight" | "reps") {
    setExercises((currentExercises) =>
      currentExercises.map((exercise) => {
        if (exercise.id !== exerciseId || setIndex === 0) {
          return exercise;
        }

        const previousValue = exercise.sets[setIndex - 1][field];
        if (previousValue === "") {
          return exercise;
        }

        return {
          ...exercise,
          sets: exercise.sets.map((trainingSet, currentSetIndex) =>
            currentSetIndex === setIndex ? { ...trainingSet, [field]: previousValue } : trainingSet
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
          sets: [...exercise.sets, createSetForm()]
        };
      })
    );
  }

  function deleteSet(exerciseId: number, setId: number) {
    setExercises((currentExercises) =>
      currentExercises.map((exercise) => {
        if (exercise.id !== exerciseId || exercise.sets.length <= 1) {
          return exercise;
        }

        return {
          ...exercise,
          sets: exercise.sets.filter((trainingSet) => trainingSet.id !== setId)
        };
      })
    );
  }

  function addExercise() {
    setExercises((currentExercises) => [...currentExercises, createExerciseForm("", true)]);
  }

  function deleteExercise(exerciseId: number) {
    if (exercises.length <= 1 || !window.confirm("この種目を削除しますか？")) {
      return;
    }

    setExercises((currentExercises) =>
      currentExercises.length <= 1
        ? currentExercises
        : currentExercises.filter((exercise) => exercise.id !== exerciseId)
    );
    setInvalidExerciseIds((currentIds) => {
      const nextIds = new Set(currentIds);
      nextIds.delete(exerciseId);
      return nextIds;
    });
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
        const weight = Number(trainingSet.weight);
        const reps = Number(trainingSet.reps);

        if (trainingSet.weight === "" || Number.isNaN(weight) || weight < 0) {
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

    const unselectedExerciseIds = new Set(
      exercises.filter((exercise) => !exercise.exerciseName.trim()).map((exercise) => exercise.id)
    );
    setInvalidExerciseIds(unselectedExerciseIds);
    if (unselectedExerciseIds.size > 0) {
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

        for (let setIndex = 0; setIndex < exercise.sets.length; setIndex += 1) {
          const trainingSet = exercise.sets[setIndex];
          await createTrainingSet(token, createdSession.id, createdExercise.id, {
            setNumber: setIndex + 1,
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

  if (selectedExerciseNames.length === 0) {
    return <p className="px-5 py-8 text-gray-600">種目選択画面へ移動しています...</p>;
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

        <form className="space-y-5" onSubmit={handleSubmit} noValidate>
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

          <section className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-950">Rest Timer</h2>
            <p className="mt-1 text-sm text-gray-500">セット間の休憩時間を計測します。</p>

            <div className="mt-5 flex flex-wrap gap-2">
              {REST_TIMER_PRESETS.map((seconds) => (
                <button
                  key={seconds}
                  className={`rounded-md border px-4 py-2 text-sm font-medium transition ${
                    restDuration === seconds
                      ? "border-emerald-700 bg-emerald-50 text-emerald-800"
                      : "border-gray-300 bg-white text-gray-800 hover:bg-gray-50"
                  }`}
                  type="button"
                  onClick={() => selectRestDuration(seconds)}
                >
                  {seconds}秒
                </button>
              ))}
            </div>

            <div className="mt-6 text-center" aria-live="polite">
              <p className="font-mono text-5xl font-semibold tabular-nums tracking-tight text-gray-950 sm:text-6xl">
                {formattedRestTime}
              </p>
              <p className="mt-3 min-h-6 font-semibold text-emerald-700">
                {remainingSeconds === 0 ? "Rest Complete!" : null}
              </p>
            </div>

            <div className="mt-5 flex flex-wrap justify-center gap-2">
              {restTimerStatus === "idle" || restTimerStatus === "completed" ? (
                <button
                  className="rounded-md bg-emerald-700 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-800"
                  type="button"
                  onClick={startRestTimer}
                >
                  Start
                </button>
              ) : null}
              {restTimerStatus === "running" ? (
                <button
                  className="rounded-md bg-gray-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-gray-800"
                  type="button"
                  onClick={pauseRestTimer}
                >
                  Pause
                </button>
              ) : null}
              {restTimerStatus === "paused" ? (
                <button
                  className="rounded-md bg-gray-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-gray-800"
                  type="button"
                  onClick={resumeRestTimer}
                >
                  Resume
                </button>
              ) : null}
              <button
                className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400"
                type="button"
                onClick={resetRestTimer}
                disabled={restTimerStatus === "idle" && remainingSeconds === restDuration}
              >
                Reset
              </button>
              <button
                className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 transition hover:bg-gray-50"
                type="button"
                onClick={addRestTime}
              >
                +30秒
              </button>
            </div>
          </section>

          {exercises.map((exercise, exerciseIndex) => (
            <section key={exercise.id} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div className="block flex-1">
                  <p className="text-sm font-medium text-gray-700">exerciseName</p>
                  {exercise.canSelectExercise ? (
                    <>
                      <select
                        className={`mt-2 w-full rounded-md border bg-white px-3 py-2 text-gray-950 outline-none transition focus:ring-2 ${
                          invalidExerciseIds.has(exercise.id)
                            ? "border-red-500 focus:border-red-600 focus:ring-red-100"
                            : "border-gray-300 focus:border-emerald-600 focus:ring-emerald-100"
                        }`}
                        value={exercise.exerciseName}
                        onChange={(event) => updateExerciseName(exercise.id, event.target.value)}
                        aria-invalid={invalidExerciseIds.has(exercise.id)}
                        aria-describedby={
                          invalidExerciseIds.has(exercise.id) ? `exercise-error-${exercise.id}` : undefined
                        }
                      >
                        <option value="">種目を選択してください</option>
                        {TRAINING_EXERCISES.filter(
                          (exerciseName) =>
                            exerciseName === exercise.exerciseName ||
                            !exercises.some((currentExercise) => currentExercise.exerciseName === exerciseName)
                        ).map((exerciseName) => (
                          <option key={exerciseName} value={exerciseName}>
                            {exerciseName}
                          </option>
                        ))}
                      </select>
                      {invalidExerciseIds.has(exercise.id) ? (
                        <p id={`exercise-error-${exercise.id}`} className="mt-2 text-sm font-medium text-red-600">
                          種目を選択してください
                        </p>
                      ) : null}
                    </>
                  ) : (
                    <p className="mt-2 rounded-md border border-gray-200 bg-gray-50 px-3 py-2 font-medium text-gray-950">
                      {exercise.exerciseName}
                    </p>
                  )}
                </div>
                <div className="flex flex-wrap gap-2 sm:justify-end">
                  <button
                    className="rounded-md border border-red-200 bg-white px-4 py-2 text-sm font-medium text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:border-gray-200 disabled:text-gray-400"
                    type="button"
                    onClick={() => deleteExercise(exercise.id)}
                    disabled={exercises.length <= 1}
                    aria-label="種目を削除"
                    title="種目を削除"
                  >
                    種目削除
                  </button>
                  <button
                    className="rounded-md bg-gray-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-gray-800"
                    type="button"
                    onClick={() => addSet(exercise.id)}
                  >
                    + セット追加
                  </button>
                </div>
              </div>

              <div className="mt-4 overflow-x-auto">
                <table className="w-full min-w-[680px] table-fixed text-left text-sm">
                  <colgroup>
                    <col style={{ width: "11%" }} />
                    <col style={{ width: "7%" }} />
                    <col style={{ width: "30%" }} />
                    <col style={{ width: "7%" }} />
                    <col style={{ width: "30%" }} />
                    <col style={{ width: "15%" }} />
                  </colgroup>
                  <thead className="text-gray-500">
                    <tr>
                      <th className="px-2 py-2 font-medium">set</th>
                      <th className="px-1 py-2 text-center font-medium">重量</th>
                      <th className="px-2 py-2 font-medium">weight</th>
                      <th className="px-1 py-2 text-center font-medium">回数</th>
                      <th className="px-2 py-2 font-medium">reps</th>
                      <th className="px-2 py-2 text-right font-medium">操作</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {exercise.sets.map((trainingSet, setIndex) => {
                      const previousSet = setIndex > 0 ? exercise.sets[setIndex - 1] : null;

                      return (
                        <tr key={trainingSet.id}>
                          <td className="px-2 py-2">
                            <span className="block px-2 py-2 font-medium text-gray-950">{setIndex + 1}</span>
                          </td>
                          <td className="px-1 py-2 text-center">
                            <button
                              className="w-9 rounded-md border border-gray-300 bg-white px-0 py-2 font-medium text-gray-800 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400"
                              type="button"
                              onClick={() => copyPreviousSetValue(exercise.id, setIndex, "weight")}
                              disabled={!previousSet || previousSet.weight === ""}
                              aria-label="前のセットの重量をコピー"
                              title="前のセットの重量をコピー"
                            >
                              →
                            </button>
                          </td>
                          <td className="px-2 py-2">
                            <input
                              className="w-full min-w-0 rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                              type="number"
                              min="0"
                              step="0.5"
                              value={trainingSet.weight}
                              onChange={(event) => updateSet(exercise.id, trainingSet.id, "weight", event.target.value)}
                              placeholder={exerciseIndex === 0 ? "107.5" : ""}
                              required
                            />
                          </td>
                          <td className="px-1 py-2 text-center">
                            <button
                              className="w-9 rounded-md border border-gray-300 bg-white px-0 py-2 font-medium text-gray-800 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400"
                              type="button"
                              onClick={() => copyPreviousSetValue(exercise.id, setIndex, "reps")}
                              disabled={!previousSet || previousSet.reps === ""}
                              aria-label="前のセットの回数をコピー"
                              title="前のセットの回数をコピー"
                            >
                              →
                            </button>
                          </td>
                          <td className="px-2 py-2">
                            <input
                              className="w-full min-w-0 rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                              type="number"
                              min="1"
                              step="1"
                              value={trainingSet.reps}
                              onChange={(event) => updateSet(exercise.id, trainingSet.id, "reps", event.target.value)}
                              placeholder={exerciseIndex === 0 ? "5" : ""}
                              required
                            />
                          </td>
                          <td className="px-2 py-2 text-right">
                            <button
                              className="rounded-md bg-red-600 px-3 py-2 font-medium text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:bg-gray-300"
                              type="button"
                              onClick={() => deleteSet(exercise.id, trainingSet.id)}
                              disabled={exercise.sets.length <= 1}
                              aria-label="セットを削除"
                              title="セットを削除"
                            >
                              削除
                            </button>
                          </td>
                        </tr>
                      );
                    })}
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
