export class TrainingCreateResultUnknownError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "TrainingCreateResultUnknownError";
  }
}

export async function executeTrainingCreateRequest<T>(
  request: () => Promise<Response>,
  failureMessage: string
): Promise<T> {
  let response: Response;

  try {
    response = await request();
  } catch {
    throw new TrainingCreateResultUnknownError(
      "通信が途切れたため、サーバーに保存されたか確認できません。"
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (response.status >= 500) {
    throw new TrainingCreateResultUnknownError(
      "サーバーエラーのため、保存結果を確認できません。"
    );
  }

  if (!response.ok) {
    throw new Error(failureMessage);
  }

  try {
    return await response.json();
  } catch {
    throw new TrainingCreateResultUnknownError(
      "サーバーから保存結果を受信できませんでした。"
    );
  }
}

export type TrainingSessionDraft = {
  date: string;
  memo: string | null;
  exercises: Array<{
    exerciseName: string;
    sets: Array<{
      setNumber: number;
      weightKg: number;
      reps: number;
    }>;
  }>;
};

export type TrainingCreateApi = {
  createSession: (request: { date: string; memo: string | null }) => Promise<{ id: number }>;
  createExercise: (
    sessionId: number,
    request: { exerciseName: string; memo: null }
  ) => Promise<{ id: number }>;
  createSet: (
    sessionId: number,
    exerciseId: number,
    request: { setNumber: number; weightKg: number; reps: number; memo: null }
  ) => Promise<unknown>;
};

export type TrainingCreateStage =
  | { kind: "session" }
  | { kind: "exercise"; exerciseNumber: number; exerciseName: string }
  | { kind: "set"; exerciseNumber: number; exerciseName: string; setNumber: number };

export type TrainingCreateProgress = {
  mutationStarted: boolean;
  completedCount: number;
  sessionId: number | null;
  currentStage: TrainingCreateStage | null;
};

export type SaveAttemptGuard = {
  running: boolean;
  locked: boolean;
};

export type TrainingCreateRecovery = {
  kind: "partial" | "unknown-session";
  sessionId: number | null;
  stageLabel: string;
  completedCount: number;
  resultUnknown: boolean;
};

export function createTrainingCreateProgress(): TrainingCreateProgress {
  return {
    mutationStarted: false,
    completedCount: 0,
    sessionId: null,
    currentStage: null
  };
}

export function createSaveAttemptGuard(): SaveAttemptGuard {
  return { running: false, locked: false };
}

export function beginSaveAttempt(guard: SaveAttemptGuard) {
  if (guard.running || guard.locked) {
    return false;
  }

  guard.running = true;
  return true;
}

export function finishSaveAttempt(guard: SaveAttemptGuard) {
  guard.running = false;
}

export function lockSaveAttempt(guard: SaveAttemptGuard) {
  guard.running = false;
  guard.locked = true;
}

async function runCreateMutation<T>(
  progress: TrainingCreateProgress,
  stage: TrainingCreateStage,
  mutation: () => Promise<T>
) {
  progress.mutationStarted = true;
  progress.currentStage = stage;
  const result = await mutation();
  progress.completedCount += 1;
  return result;
}

export async function saveTrainingSessionDraft(
  draft: TrainingSessionDraft,
  api: TrainingCreateApi,
  progress: TrainingCreateProgress
) {
  const createdSession = await runCreateMutation(progress, { kind: "session" }, () =>
    api.createSession({ date: draft.date, memo: draft.memo })
  );
  progress.sessionId = createdSession.id;

  for (let exerciseIndex = 0; exerciseIndex < draft.exercises.length; exerciseIndex += 1) {
    const exercise = draft.exercises[exerciseIndex];
    const exerciseNumber = exerciseIndex + 1;
    const createdExercise = await runCreateMutation(
      progress,
      { kind: "exercise", exerciseNumber, exerciseName: exercise.exerciseName },
      () =>
        api.createExercise(createdSession.id, {
          exerciseName: exercise.exerciseName,
          memo: null
        })
    );

    for (const trainingSet of exercise.sets) {
      await runCreateMutation(
        progress,
        {
          kind: "set",
          exerciseNumber,
          exerciseName: exercise.exerciseName,
          setNumber: trainingSet.setNumber
        },
        () =>
          api.createSet(createdSession.id, createdExercise.id, {
            ...trainingSet,
            memo: null
          })
      );
    }
  }

  return createdSession.id;
}

export function trainingCreateStageLabel(stage: TrainingCreateStage | null) {
  if (!stage || stage.kind === "session") {
    return "Sessionの作成";
  }

  if (stage.kind === "exercise") {
    return `種目${stage.exerciseNumber}（${stage.exerciseName}）の作成`;
  }

  return `種目${stage.exerciseNumber}（${stage.exerciseName}）のセット${stage.setNumber}の作成`;
}

export function recoveryAfterCreateError(
  progress: TrainingCreateProgress,
  error: unknown
): TrainingCreateRecovery | null {
  const resultUnknown = error instanceof TrainingCreateResultUnknownError;

  if (progress.sessionId !== null) {
    return {
      kind: "partial",
      sessionId: progress.sessionId,
      stageLabel: trainingCreateStageLabel(progress.currentStage),
      completedCount: progress.completedCount,
      resultUnknown
    };
  }

  if (progress.mutationStarted && resultUnknown) {
    return {
      kind: "unknown-session",
      sessionId: null,
      stageLabel: trainingCreateStageLabel(progress.currentStage),
      completedCount: progress.completedCount,
      resultUnknown: true
    };
  }

  return null;
}
