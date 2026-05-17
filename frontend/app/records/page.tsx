"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { deleteRecord, fetchRecords, RecordResponse, TOKEN_STORAGE_KEY } from "@/lib/api";

export default function RecordsPage() {
  const router = useRouter();
  const [records, setRecords] = useState<RecordResponse[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [deletingRecordId, setDeletingRecordId] = useState<number | null>(null);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    if (!token) {
      router.replace("/");
      return;
    }

    fetchRecords(token)
      .then((data) => {
        setRecords(data);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "体重履歴の取得に失敗しました。");
        if (err instanceof Error && err.message.includes("ログイン")) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [router]);

  async function handleDelete(recordId: number) {
    if (!window.confirm("削除しますか？")) {
      return;
    }

    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      router.replace("/");
      return;
    }

    setError("");
    setDeletingRecordId(recordId);

    try {
      await deleteRecord(token, recordId);
      setRecords((currentRecords) => currentRecords.filter((record) => record.id !== recordId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "体重記録の削除に失敗しました。");
      if (err instanceof Error && err.message.includes("ログイン")) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      }
    } finally {
      setDeletingRecordId(null);
    }
  }

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">体重履歴</h1>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              className="rounded-md bg-emerald-700 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-800"
              type="button"
              onClick={() => router.push("/records/new")}
            >
              体重記録を追加
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

        {!isLoading && !error && records.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white p-6 text-gray-600 shadow-sm">体重履歴がありません。</div>
        ) : null}

        {records.length > 0 ? (
          <section className="grid gap-4 md:grid-cols-2">
            {records.map((record) => (
              <article key={record.id} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm text-gray-500">{record.date}</p>
                    <p className="mt-3 flex items-baseline gap-2">
                      <span className="text-3xl font-semibold text-gray-950">{record.weight}</span>
                      <span className="text-sm text-gray-500">kg</span>
                    </p>
                  </div>
                  <button
                    className="rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:bg-gray-400"
                    type="button"
                    onClick={() => handleDelete(record.id)}
                    disabled={deletingRecordId === record.id}
                  >
                    {deletingRecordId === record.id ? "削除中..." : "削除"}
                  </button>
                </div>
              </article>
            ))}
          </section>
        ) : null}
      </div>
    </main>
  );
}
