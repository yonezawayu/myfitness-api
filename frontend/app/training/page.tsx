"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { deleteTrainingLog, fetchTrainingLogs, TOKEN_STORAGE_KEY, TrainingLogResponse } from "@/lib/api";

export default function TrainingPage() {
  const router = useRouter();
  const [trainingLogs, setTrainingLogs] = useState<TrainingLogResponse[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [deletingTrainingId, setDeletingTrainingId] = useState<number | null>(null);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    if (!token) {
      router.replace("/");
      return;
    }

    fetchTrainingLogs(token)
      .then((data) => {
        setTrainingLogs(data);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "トレーニング履歴の取得に失敗しました。");
        if (err instanceof Error && err.message.includes("ログイン")) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [router]);

  async function handleDelete(trainingLogId: number) {
    if (!window.confirm("削除しますか？")) {
      return;
    }

    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      router.replace("/");
      return;
    }

    setError("");
    setDeletingTrainingId(trainingLogId);

    try {
      await deleteTrainingLog(token, trainingLogId);
      setTrainingLogs((currentLogs) => currentLogs.filter((log) => log.id !== trainingLogId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "トレーニング記録の削除に失敗しました。");
      if (err instanceof Error && err.message.includes("ログイン")) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      }
    } finally {
      setDeletingTrainingId(null);
    }
  }

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">トレーニング履歴</h1>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              className="rounded-md bg-gray-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-gray-800"
              type="button"
              onClick={() => router.push("/training/new")}
            >
              トレーニング記録を追加
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

        {!isLoading && !error && trainingLogs.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white p-6 text-gray-600 shadow-sm">
            トレーニング履歴がありません。
          </div>
        ) : null}

        {trainingLogs.length > 0 ? (
          <section className="grid gap-4 md:grid-cols-2">
            {trainingLogs.map((trainingLog) => (
              <article key={trainingLog.id} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <h2 className="text-lg font-semibold text-gray-950">{trainingLog.trainingName}</h2>
                    <p className="mt-1 text-sm text-gray-500">{trainingLog.date}</p>
                  </div>
                  <button
                    className="rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:bg-gray-400"
                    type="button"
                    onClick={() => handleDelete(trainingLog.id)}
                    disabled={deletingTrainingId === trainingLog.id}
                  >
                    {deletingTrainingId === trainingLog.id ? "削除中..." : "削除"}
                  </button>
                </div>

                <div className="mt-5">
                  <p className="text-sm font-medium text-gray-500">caloriesBurned</p>
                  <p className="mt-1 text-3xl font-semibold text-gray-950">{trainingLog.caloriesBurned}</p>
                </div>
              </article>
            ))}
          </section>
        ) : null}
      </div>
    </main>
  );
}
