"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  fetchTrainingSessions,
  TOKEN_STORAGE_KEY,
  TrainingExerciseResponse,
  TrainingSessionResponse,
  TrainingSetResponse
} from "@/lib/api";

function setVolume(trainingSet: TrainingSetResponse) {
  return (trainingSet.weightKg ?? 0) * trainingSet.reps;
}

function estimatedOneRepMax(trainingSet: TrainingSetResponse) {
  if (trainingSet.weightKg === null) {
    return null;
  }

  return trainingSet.weightKg * (1 + trainingSet.reps / 30);
}

function exerciseVolume(exercise: TrainingExerciseResponse) {
  return exercise.sets.reduce((total, trainingSet) => total + setVolume(trainingSet), 0);
}

function sessionStats(session: TrainingSessionResponse) {
  return session.exercises.reduce(
    (stats, exercise) => {
      exercise.sets.forEach((trainingSet) => {
        stats.totalSets += 1;
        stats.totalReps += trainingSet.reps;
        stats.totalVolume += setVolume(trainingSet);
      });

      return stats;
    },
    { totalSets: 0, totalReps: 0, totalVolume: 0 }
  );
}

function formatNumber(value: number, maximumFractionDigits = 1) {
  return new Intl.NumberFormat("ja-JP", {
    maximumFractionDigits
  }).format(value);
}

export default function TrainingSessionsPage() {
  const router = useRouter();
  const [sessions, setSessions] = useState<TrainingSessionResponse[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    if (!token) {
      router.replace("/");
      return;
    }

    fetchTrainingSessions(token)
      .then((data) => {
        setSessions(data);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "本格トレーニング履歴の取得に失敗しました。");
        if (err instanceof Error && err.message.includes("ログイン")) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          router.replace("/");
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
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">本格トレーニング履歴</h1>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              className="rounded-md bg-gray-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-gray-800"
              type="button"
              onClick={() => router.push("/training-sessions/new")}
            >
              新規作成
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

        {!isLoading && !error && sessions.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white p-6 text-gray-600 shadow-sm">
            本格トレーニング履歴がありません。
          </div>
        ) : null}

        {sessions.length > 0 ? (
          <section className="space-y-5">
            {sessions.map((session) => {
              const stats = sessionStats(session);

              return (
                <article key={session.id} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-500">Session</p>
                      <h2 className="mt-1 text-xl font-semibold text-gray-950">{session.date}</h2>
                      {session.memo ? <p className="mt-2 text-sm text-gray-600">{session.memo}</p> : null}
                    </div>
                    <div className="grid grid-cols-3 gap-2 sm:min-w-80">
                      <div className="rounded-md bg-gray-50 px-3 py-2">
                        <p className="text-xs font-medium text-gray-500">Sets</p>
                        <p className="mt-1 text-lg font-semibold text-gray-950">{stats.totalSets}</p>
                      </div>
                      <div className="rounded-md bg-gray-50 px-3 py-2">
                        <p className="text-xs font-medium text-gray-500">Reps</p>
                        <p className="mt-1 text-lg font-semibold text-gray-950">{stats.totalReps}</p>
                      </div>
                      <div className="rounded-md bg-gray-50 px-3 py-2">
                        <p className="text-xs font-medium text-gray-500">Volume</p>
                        <p className="mt-1 text-lg font-semibold text-gray-950">
                          {formatNumber(stats.totalVolume, 0)}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="mt-5 space-y-4">
                    {session.exercises.length === 0 ? (
                      <p className="rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-600">種目がありません。</p>
                    ) : null}

                    {session.exercises.map((exercise) => (
                      <section key={exercise.id} className="rounded-md border border-gray-200">
                        <div className="flex flex-col gap-2 border-b border-gray-200 bg-gray-50 px-4 py-3 sm:flex-row sm:items-end sm:justify-between">
                          <div>
                            <h3 className="text-base font-semibold text-gray-950">{exercise.exerciseName}</h3>
                            {exercise.memo ? <p className="mt-1 text-sm text-gray-600">{exercise.memo}</p> : null}
                          </div>
                          <p className="text-sm font-medium text-gray-700">
                            Volume {formatNumber(exerciseVolume(exercise), 0)}
                          </p>
                        </div>

                        {exercise.sets.length > 0 ? (
                          <div className="overflow-x-auto">
                            <table className="w-full min-w-[520px] text-left text-sm">
                              <thead className="text-gray-500">
                                <tr>
                                  <th className="px-4 py-2 font-medium">Set</th>
                                  <th className="px-4 py-2 font-medium">Weight</th>
                                  <th className="px-4 py-2 font-medium">Reps</th>
                                  <th className="px-4 py-2 font-medium">Est.1RM</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-gray-100">
                                {exercise.sets.map((trainingSet) => {
                                  const oneRepMax = estimatedOneRepMax(trainingSet);

                                  return (
                                    <tr key={trainingSet.id}>
                                      <td className="px-4 py-3 font-medium text-gray-950">
                                        {trainingSet.setNumber}
                                      </td>
                                      <td className="px-4 py-3 text-gray-700">
                                        {trainingSet.weightKg === null
                                          ? "-"
                                          : `${formatNumber(trainingSet.weightKg)} kg`}
                                      </td>
                                      <td className="px-4 py-3 text-gray-700">{trainingSet.reps}</td>
                                      <td className="px-4 py-3 text-gray-700">
                                        {oneRepMax === null ? "-" : `${formatNumber(oneRepMax)} kg`}
                                      </td>
                                    </tr>
                                  );
                                })}
                              </tbody>
                            </table>
                          </div>
                        ) : (
                          <p className="px-4 py-3 text-sm text-gray-600">セットがありません。</p>
                        )}
                      </section>
                    ))}
                  </div>
                </article>
              );
            })}
          </section>
        ) : null}
      </div>
    </main>
  );
}
