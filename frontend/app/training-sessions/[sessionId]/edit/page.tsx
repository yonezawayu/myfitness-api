"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  createTrainingExercise,
  createTrainingSet,
  deleteTrainingExercise,
  deleteTrainingSet,
  fetchTrainingSession,
  TOKEN_STORAGE_KEY,
  TrainingSessionNotFoundError,
  updateTrainingSession,
  updateTrainingSet
} from "@/lib/api";
import {
  createMutationProgress,
  createSetOperations,
  reloadLockAfterFetch,
  renumberSets,
  runTrackedMutation,
  shouldLockAfterMutationError,
  toSessionForm,
  weightValue,
  type ExerciseForm,
  type SessionForm,
  type SetForm
} from "@/lib/training-session-edit";
import { TRAINING_EXERCISES } from "@/lib/training-exercises";

type LoadStatus = "loading" | "ready" | "not-found" | "error";

const MAX_MEMO_LENGTH = 500;
const MAX_INTEGER_VALUE = 2_147_483_647;
let nextTemporarySetId = 1;
let nextTemporaryExerciseId = 1;

function createNewSet(setNumber: number): SetForm {
  return {
    id: null,
    formId: `new-${nextTemporarySetId++}`,
    setNumber,
    originalSetNumber: null,
    weight: "",
    reps: "",
    memo: null
  };
}

function createNewExercise(): ExerciseForm {
  return {
    id: null,
    formId: `new-exercise-${nextTemporaryExerciseId++}`,
    exerciseName: "",
    memo: null,
    sets: [createNewSet(1)],
    markedForDeletion: false
  };
}

function createExerciseOperations(currentSession: SessionForm) {
  const exercisesToCreate = currentSession.exercises.filter(
    (exercise) => exercise.id === null && !exercise.markedForDeletion
  );
  const exercisesToDelete = currentSession.exercises.flatMap((exercise) =>
    exercise.id !== null && exercise.markedForDeletion ? [{ exerciseId: exercise.id }] : []
  );

  return { exercisesToCreate, exercisesToDelete };
}

function validateSession(session: SessionForm) {
  if (!session.date) {
    return "日付を入力してください。";
  }

  if (session.memo.length > MAX_MEMO_LENGTH) {
    return `Sessionメモは${MAX_MEMO_LENGTH}文字以内で入力してください。`;
  }

  const activeExercises = session.exercises.filter((exercise) => !exercise.markedForDeletion);
  for (const exercise of activeExercises) {
    if (!exercise.exerciseName.trim()) {
      return "追加する種目を選択してください。";
    }

    if (
      exercise.id === null &&
      !TRAINING_EXERCISES.some((exerciseName) => exerciseName === exercise.exerciseName)
    ) {
      return "種目マスタから追加する種目を選択してください。";
    }

    if (
      exercise.id === null &&
      activeExercises.some(
        (otherExercise) =>
          otherExercise.formId !== exercise.formId && otherExercise.exerciseName === exercise.exerciseName
      )
    ) {
      return `${exercise.exerciseName}はすでに追加されています。`;
    }

    for (let setIndex = 0; setIndex < exercise.sets.length; setIndex += 1) {
      const trainingSet = exercise.sets[setIndex];
      const setLabel = `${exercise.exerciseName}のセット${setIndex + 1}`;
      const weight = weightValue(trainingSet.weight);
      const reps = Number(trainingSet.reps);

      if (weight !== null && (!Number.isFinite(weight) || weight < 0)) {
        return `${setLabel}の重量は0以上の数値、または空欄で入力してください。`;
      }

      if (
        trainingSet.reps.trim() === "" ||
        !Number.isInteger(reps) ||
        reps < 1 ||
        reps > MAX_INTEGER_VALUE
      ) {
        return `${setLabel}の回数は1以上${MAX_INTEGER_VALUE}以下の整数で入力してください。`;
      }

      if (trainingSet.memo !== null && trainingSet.memo.length > MAX_MEMO_LENGTH) {
        return `${setLabel}のメモが${MAX_MEMO_LENGTH}文字を超えています。`;
      }
    }
  }

  return "";
}

export default function EditTrainingSessionPage() {
  const router = useRouter();
  const params = useParams<{ sessionId: string }>();
  const sessionId = Number(params.sessionId);
  const hasValidSessionId = Number.isSafeInteger(sessionId) && sessionId > 0;
  const [session, setSession] = useState<SessionForm | null>(null);
  const [originalSession, setOriginalSession] = useState<SessionForm | null>(null);
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("loading");
  const [error, setError] = useState("");
  const [saveError, setSaveError] = useState("");
  const [saveMessage, setSaveMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isReloading, setIsReloading] = useState(false);
  const [requiresReload, setRequiresReload] = useState(false);

  useEffect(() => {
    if (!hasValidSessionId) {
      return;
    }

    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      router.replace("/");
      return;
    }

    let isCancelled = false;

    fetchTrainingSession(token, sessionId)
      .then((response) => {
        if (isCancelled) {
          return;
        }

        const nextSession = toSessionForm(response);
        setSession(nextSession);
        setOriginalSession(nextSession);
        setLoadStatus("ready");
      })
      .catch((err: unknown) => {
        if (isCancelled) {
          return;
        }

        if (err instanceof TrainingSessionNotFoundError) {
          setLoadStatus("not-found");
          return;
        }

        const message = err instanceof Error ? err.message : "Training Sessionの取得に失敗しました。";
        setError(message);
        setLoadStatus("error");

        if (message.includes("ログイン")) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          router.replace("/");
        }
      });

    return () => {
      isCancelled = true;
    };
  }, [hasValidSessionId, router, sessionId]);

  const sessionChanged =
    session !== null &&
    originalSession !== null &&
    (session.date !== originalSession.date || session.memo !== originalSession.memo);
  const setOperations =
    session && originalSession
      ? createSetOperations(session, originalSession)
      : { setsToCreate: [], setsToUpdate: [], setsToDelete: [] };
  const exerciseOperations = session
    ? createExerciseOperations(session)
    : { exercisesToCreate: [], exercisesToDelete: [] };
  const hasSetChanges =
    setOperations.setsToCreate.length > 0 ||
    setOperations.setsToUpdate.length > 0 ||
    setOperations.setsToDelete.length > 0;
  const hasExerciseChanges =
    exerciseOperations.exercisesToCreate.length > 0 || exerciseOperations.exercisesToDelete.length > 0;
  const hasChanges = sessionChanged || hasSetChanges || hasExerciseChanges;
  const isFormDisabled = isSubmitting || isReloading || requiresReload;
  const hasUnselectedExercise =
    session?.exercises.some((exercise) => !exercise.markedForDeletion && !exercise.exerciseName) ?? false;
  const selectedExerciseNames = new Set(session?.exercises.map((exercise) => exercise.exerciseName).filter(Boolean));
  const canAddExercise =
    !hasUnselectedExercise && TRAINING_EXERCISES.some((exerciseName) => !selectedExerciseNames.has(exerciseName));

  function clearSaveFeedback() {
    setSaveError("");
    setSaveMessage("");
  }

  function updateSet(exerciseFormId: string, setFormId: string, field: "weight" | "reps", value: string) {
    clearSaveFeedback();
    setSession((currentSession) => {
      if (!currentSession) {
        return currentSession;
      }

      return {
        ...currentSession,
        exercises: currentSession.exercises.map((exercise) =>
          exercise.formId === exerciseFormId
            ? {
                ...exercise,
                sets: exercise.sets.map((trainingSet) =>
                  trainingSet.formId === setFormId ? { ...trainingSet, [field]: value } : trainingSet
                )
              }
            : exercise
        )
      };
    });
  }

  function copyPreviousSetValue(exerciseFormId: string, setIndex: number, field: "weight" | "reps") {
    clearSaveFeedback();
    setSession((currentSession) => {
      if (!currentSession || setIndex === 0) {
        return currentSession;
      }

      return {
        ...currentSession,
        exercises: currentSession.exercises.map((exercise) => {
          if (exercise.formId !== exerciseFormId) {
            return exercise;
          }

          const previousValue = exercise.sets[setIndex - 1]?.[field];
          if (previousValue === undefined || previousValue === "") {
            return exercise;
          }

          return {
            ...exercise,
            sets: exercise.sets.map((trainingSet, currentSetIndex) =>
              currentSetIndex === setIndex ? { ...trainingSet, [field]: previousValue } : trainingSet
            )
          };
        })
      };
    });
  }

  function addSet(exerciseFormId: string) {
    clearSaveFeedback();
    setSession((currentSession) => {
      if (!currentSession) {
        return currentSession;
      }

      return {
        ...currentSession,
        exercises: currentSession.exercises.map((exercise) =>
          exercise.formId === exerciseFormId
            ? { ...exercise, sets: [...exercise.sets, createNewSet(exercise.sets.length + 1)] }
            : exercise
        )
      };
    });
  }

  function deleteSet(exerciseFormId: string, setFormId: string) {
    clearSaveFeedback();
    setSession((currentSession) => {
      if (!currentSession) {
        return currentSession;
      }

      return {
        ...currentSession,
        exercises: currentSession.exercises.map((exercise) => {
          if (exercise.formId !== exerciseFormId) {
            return exercise;
          }

          return {
            ...exercise,
            sets: renumberSets(exercise.sets.filter((trainingSet) => trainingSet.formId !== setFormId))
          };
        })
      };
    });
  }

  function addExercise() {
    clearSaveFeedback();
    setSession((currentSession) =>
      currentSession
        ? { ...currentSession, exercises: [...currentSession.exercises, createNewExercise()] }
        : currentSession
    );
  }

  function updateExerciseName(exerciseFormId: string, exerciseName: string) {
    clearSaveFeedback();
    setSession((currentSession) =>
      currentSession
        ? {
            ...currentSession,
            exercises: currentSession.exercises.map((exercise) =>
              exercise.formId === exerciseFormId ? { ...exercise, exerciseName } : exercise
            )
          }
        : currentSession
    );
  }

  function markExerciseForDeletion(exerciseFormId: string) {
    clearSaveFeedback();
    setSession((currentSession) => {
      if (!currentSession) {
        return currentSession;
      }

      const targetExercise = currentSession.exercises.find((exercise) => exercise.formId === exerciseFormId);
      return {
        ...currentSession,
        exercises:
          targetExercise?.id === null
            ? currentSession.exercises.filter((exercise) => exercise.formId !== exerciseFormId)
            : currentSession.exercises.map((exercise) =>
                exercise.formId === exerciseFormId ? { ...exercise, markedForDeletion: true } : exercise
              )
      };
    });
  }

  function restoreExercise(exerciseFormId: string) {
    clearSaveFeedback();
    setSession((currentSession) =>
      currentSession
        ? {
            ...currentSession,
            exercises: currentSession.exercises.map((exercise) =>
              exercise.formId === exerciseFormId ? { ...exercise, markedForDeletion: false } : exercise
            )
          }
        : currentSession
    );
  }

  async function reloadLatestSession() {
    if (isSubmitting || isReloading) {
      return;
    }

    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      router.replace("/");
      return;
    }

    setIsReloading(true);
    setSaveError("");
    setSaveMessage("");

    try {
      const response = await fetchTrainingSession(token, sessionId);
      const nextSession = toSessionForm(response);
      setSession(nextSession);
      setOriginalSession(nextSession);
      setRequiresReload(reloadLockAfterFetch(true));
      setSaveMessage("最新データを再取得しました。");
    } catch (err) {
      if (err instanceof TrainingSessionNotFoundError) {
        setLoadStatus("not-found");
        return;
      }

      const message = err instanceof Error ? err.message : "Training Sessionの再取得に失敗しました。";
      setSaveError(message);
      if (message.includes("ログイン")) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        router.replace("/");
      }
    } finally {
      setIsReloading(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !originalSession || isSubmitting || isReloading || requiresReload) {
      return;
    }

    setSaveError("");
    setSaveMessage("");

    const validationMessage = validateSession(session);
    if (validationMessage) {
      setSaveError(validationMessage);
      return;
    }

    const shouldUpdateSession = session.date !== originalSession.date || session.memo !== originalSession.memo;
    const { setsToCreate, setsToUpdate, setsToDelete } = createSetOperations(session, originalSession);
    const { exercisesToCreate, exercisesToDelete } = createExerciseOperations(session);
    const updateCount =
      (shouldUpdateSession ? 1 : 0) +
      exercisesToDelete.length +
      setsToCreate.length +
      setsToUpdate.length +
      setsToDelete.length +
      exercisesToCreate.reduce((count, exercise) => count + 1 + exercise.sets.length, 0);
    if (updateCount === 0) {
      setSaveMessage("変更はありません。");
      return;
    }

    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      router.replace("/");
      return;
    }

    setIsSubmitting(true);
    const mutationProgress = createMutationProgress();

    try {
      if (shouldUpdateSession) {
        await runTrackedMutation(mutationProgress, () =>
          updateTrainingSession(token, session.id, {
            date: session.date,
            memo: session.memo || null
          })
        );
      }

      for (const { exerciseId } of exercisesToDelete) {
        await runTrackedMutation(mutationProgress, () =>
          deleteTrainingExercise(token, session.id, exerciseId)
        );
      }

      for (const { exerciseId, setId } of setsToDelete) {
        await runTrackedMutation(mutationProgress, () =>
          deleteTrainingSet(token, session.id, exerciseId, setId)
        );
      }

      for (const { exerciseId, setId, trainingSet } of setsToUpdate) {
        await runTrackedMutation(mutationProgress, () =>
          updateTrainingSet(token, session.id, exerciseId, setId, {
            setNumber: trainingSet.setNumber,
            weightKg: weightValue(trainingSet.weight),
            reps: Number(trainingSet.reps),
            memo: trainingSet.memo
          })
        );
      }

      for (const exercise of exercisesToCreate) {
        const createdExercise = await runTrackedMutation(mutationProgress, () =>
          createTrainingExercise(token, session.id, {
            exerciseName: exercise.exerciseName,
            memo: exercise.memo
          })
        );

        for (const trainingSet of exercise.sets) {
          await runTrackedMutation(mutationProgress, () =>
            createTrainingSet(token, session.id, createdExercise.id, {
              setNumber: trainingSet.setNumber,
              weightKg: weightValue(trainingSet.weight),
              reps: Number(trainingSet.reps),
              memo: trainingSet.memo
            })
          );
        }
      }

      for (const { exerciseId, trainingSet } of setsToCreate) {
        await runTrackedMutation(mutationProgress, () =>
          createTrainingSet(token, session.id, exerciseId, {
            setNumber: trainingSet.setNumber,
            weightKg: weightValue(trainingSet.weight),
            reps: Number(trainingSet.reps),
            memo: trainingSet.memo
          })
        );
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "Training Sessionの保存に失敗しました。";
      if (shouldLockAfterMutationError(mutationProgress)) {
        setRequiresReload(true);
      }
      setSaveError(
        mutationProgress.hasStarted
          ? `保存処理中にエラーが発生しました。${mutationProgress.completedCount}件の成功応答を確認しましたが、応答を受信できなかった処理がサーバーに反映された可能性があり、保存結果は不明です。最新データを再取得してください。 ${message}`
          : message
      );

      if (message.includes("ログイン")) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        router.replace("/");
      }
      setIsSubmitting(false);
      return;
    }

    try {
      const response = await fetchTrainingSession(token, session.id);
      const nextSession = toSessionForm(response);
      setSession(nextSession);
      setOriginalSession(nextSession);
      setRequiresReload(reloadLockAfterFetch(true));
      setSaveMessage(`${updateCount}件の変更を保存し、最新データを再取得しました。`);
    } catch (err) {
      const message = err instanceof Error ? err.message : "最新データの再取得に失敗しました。";
      setRequiresReload(reloadLockAfterFetch(false));
      setSaveError(
        `保存は完了しましたが、最新データの再取得に失敗しました。再取得ボタンをお試しください。 ${message}`
      );

      if (message.includes("ログイン")) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        router.replace("/");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!hasValidSessionId || loadStatus === "not-found") {
    return (
      <main className="min-h-screen px-5 py-8">
        <div className="mx-auto max-w-4xl rounded-lg border border-amber-200 bg-amber-50 p-6">
          <h1 className="text-xl font-semibold text-amber-950">Training Sessionが見つかりません</h1>
          <p className="mt-2 text-sm text-amber-800">削除されたか、URLが正しくない可能性があります。</p>
          <button
            className="mt-5 rounded-md border border-amber-300 bg-white px-4 py-2 text-sm font-medium text-amber-900 transition hover:bg-amber-100"
            type="button"
            onClick={() => router.push("/training-sessions")}
          >
            履歴へ戻る
          </button>
        </div>
      </main>
    );
  }

  if (loadStatus === "loading") {
    return <p className="px-5 py-8 text-gray-600">Training Sessionを読み込んでいます...</p>;
  }

  if (loadStatus === "error" || !session) {
    return (
      <main className="min-h-screen px-5 py-8">
        <div className="mx-auto max-w-4xl rounded-lg border border-red-200 bg-red-50 p-6">
          <h1 className="text-xl font-semibold text-red-950">Training Sessionを取得できませんでした</h1>
          <p className="mt-2 text-sm text-red-700">{error || "時間をおいてもう一度お試しください。"}</p>
          <button
            className="mt-5 rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-900 transition hover:bg-red-100"
            type="button"
            onClick={() => router.push("/training-sessions")}
          >
            履歴へ戻る
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen px-5 py-8">
      <div className="mx-auto max-w-4xl">
        <header className="mb-8 flex flex-col gap-4 border-b border-gray-200 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">MyFitness API</p>
            <h1 className="mt-2 text-2xl font-semibold text-gray-950">Training Session編集</h1>
            <p className="mt-1 text-sm text-gray-500">Session ID: {session.id}</p>
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
                  value={session.date}
                  required
                  disabled={isFormDisabled}
                  onChange={(event) => {
                    clearSaveFeedback();
                    setSession((currentSession) =>
                      currentSession ? { ...currentSession, date: event.target.value } : currentSession
                    );
                  }}
                />
              </label>

              <label className="block">
                <span className="text-sm font-medium text-gray-700">memo</span>
                <input
                  className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                  type="text"
                  value={session.memo}
                  maxLength={MAX_MEMO_LENGTH}
                  disabled={isFormDisabled}
                  onChange={(event) => {
                    clearSaveFeedback();
                    setSession((currentSession) =>
                      currentSession ? { ...currentSession, memo: event.target.value } : currentSession
                    );
                  }}
                />
              </label>
            </div>
          </section>

          {session.exercises.length === 0 ? (
            <section className="rounded-lg border border-gray-200 bg-white p-5 text-sm text-gray-600 shadow-sm">
              このSessionには種目がありません。
            </section>
          ) : null}

          {session.exercises.map((exercise) =>
            exercise.markedForDeletion ? (
              <section
                key={exercise.formId}
                className="rounded-lg border border-red-200 bg-red-50 p-5 shadow-sm"
              >
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="font-semibold text-red-900">{exercise.exerciseName || "未選択の種目"}</p>
                    <p className="mt-1 text-sm text-red-700">保存時にこの種目と関連Setを削除します。</p>
                  </div>
                  <button
                    className="rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-800 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:bg-gray-100"
                    type="button"
                    onClick={() => restoreExercise(exercise.formId)}
                    disabled={isFormDisabled}
                  >
                    削除を取り消す
                  </button>
                </div>
              </section>
            ) : (
            <section key={exercise.formId} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-700">exerciseName</p>
                  {exercise.id === null ? (
                    <select
                      className="mt-2 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                      value={exercise.exerciseName}
                      onChange={(event) => updateExerciseName(exercise.formId, event.target.value)}
                      disabled={isFormDisabled}
                      aria-label="追加する種目"
                    >
                      <option value="">種目を選択してください</option>
                      {TRAINING_EXERCISES.filter(
                        (exerciseName) =>
                          exerciseName === exercise.exerciseName || !selectedExerciseNames.has(exerciseName)
                      ).map((exerciseName) => (
                        <option key={exerciseName} value={exerciseName}>
                          {exerciseName}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <p className="mt-2 rounded-md border border-gray-200 bg-gray-50 px-3 py-2 font-medium text-gray-950">
                      {exercise.exerciseName}
                    </p>
                  )}
                  {exercise.memo ? <p className="mt-2 text-sm text-gray-600">{exercise.memo}</p> : null}
                </div>
                <div className="flex flex-wrap gap-2 sm:justify-end">
                  <button
                    className="rounded-md border border-red-200 bg-white px-4 py-2 text-sm font-medium text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400"
                    type="button"
                    onClick={() => markExerciseForDeletion(exercise.formId)}
                    disabled={isFormDisabled}
                  >
                    種目削除
                  </button>
                  <button
                    className="rounded-md bg-gray-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-gray-800 disabled:cursor-not-allowed disabled:bg-gray-400"
                    type="button"
                    onClick={() => addSet(exercise.formId)}
                    disabled={isFormDisabled}
                  >
                    + セット追加
                  </button>
                </div>
              </div>

              {exercise.sets.length === 0 ? (
                <p className="mt-4 rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-600">
                  この種目にはセットがありません。
                </p>
              ) : (
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
                          <tr key={trainingSet.formId}>
                            <td className="px-2 py-2">
                              <span className="block px-2 py-2 font-medium text-gray-950">
                                {trainingSet.setNumber}
                              </span>
                            </td>
                            <td className="px-1 py-2 text-center">
                              <button
                                className="w-9 rounded-md border border-gray-300 bg-white px-0 py-2 font-medium text-gray-800 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400"
                                type="button"
                                onClick={() => copyPreviousSetValue(exercise.formId, setIndex, "weight")}
                                disabled={isFormDisabled || !previousSet || previousSet.weight === ""}
                                aria-label="前のセットの重量をコピー"
                                title="前のセットの重量をコピー"
                              >
                                →
                              </button>
                            </td>
                            <td className="px-2 py-2">
                              <input
                                className="w-full min-w-32 rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                                type="number"
                                min="0"
                                step="0.5"
                                value={trainingSet.weight}
                                disabled={isFormDisabled}
                                onChange={(event) =>
                                  updateSet(exercise.formId, trainingSet.formId, "weight", event.target.value)
                                }
                                aria-label={`${exercise.exerciseName} セット${setIndex + 1}の重量`}
                              />
                            </td>
                            <td className="px-1 py-2 text-center">
                              <button
                                className="w-9 rounded-md border border-gray-300 bg-white px-0 py-2 font-medium text-gray-800 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400"
                                type="button"
                                onClick={() => copyPreviousSetValue(exercise.formId, setIndex, "reps")}
                                disabled={isFormDisabled || !previousSet || previousSet.reps === ""}
                                aria-label="前のセットの回数をコピー"
                                title="前のセットの回数をコピー"
                              >
                                →
                              </button>
                            </td>
                            <td className="px-2 py-2">
                              <input
                                className="w-full min-w-32 rounded-md border border-gray-300 px-3 py-2 text-gray-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                                type="number"
                                min="1"
                                step="1"
                                value={trainingSet.reps}
                                required
                                disabled={isFormDisabled}
                                onChange={(event) =>
                                  updateSet(exercise.formId, trainingSet.formId, "reps", event.target.value)
                                }
                                aria-label={`${exercise.exerciseName} セット${setIndex + 1}の回数`}
                              />
                            </td>
                            <td className="px-2 py-2 text-right">
                              <button
                                className="rounded-md bg-red-600 px-3 py-2 font-medium text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:bg-gray-300"
                                type="button"
                                onClick={() => deleteSet(exercise.formId, trainingSet.formId)}
                                disabled={isFormDisabled}
                                aria-label={`セット${setIndex + 1}を削除`}
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
              )}
            </section>
            )
          )}

          <button
            className="w-full rounded-md bg-white px-4 py-2.5 font-medium text-gray-950 ring-1 ring-gray-200 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 sm:w-auto"
            type="button"
            onClick={addExercise}
            disabled={isFormDisabled || !canAddExercise}
          >
            + 種目追加
          </button>

          {saveError ? (
            <p className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
              {saveError}
            </p>
          ) : null}

          {saveMessage ? (
            <p className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800" aria-live="polite">
              {saveMessage}
            </p>
          ) : null}

          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              className="rounded-md border border-gray-300 bg-white px-4 py-2.5 font-medium text-gray-800 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400"
              type="button"
              onClick={reloadLatestSession}
              disabled={isSubmitting || isReloading}
            >
              {isReloading ? "再取得中..." : "最新データを再取得"}
            </button>
            <button
              className="rounded-md bg-emerald-700 px-4 py-2.5 font-medium text-white transition hover:bg-emerald-800 disabled:cursor-not-allowed disabled:bg-gray-400"
              type="submit"
              disabled={!hasChanges || isFormDisabled}
            >
              {isSubmitting ? "保存中..." : requiresReload ? "再取得が必要です" : "変更を保存"}
            </button>
          </div>

          {!hasChanges && !saveMessage ? <p className="text-sm text-gray-500">変更はありません。</p> : null}
        </form>
      </div>
    </main>
  );
}
