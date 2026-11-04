export class TrainingCreateResultUnknownError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "TrainingCreateResultUnknownError";
  }
}

export class TrainingCreateConflictError extends Error {
  constructor() {
    super("同じ保存IDが異なる内容に使用されたため、保存できません。履歴一覧を確認してください。");
    this.name = "TrainingCreateConflictError";
  }
}

export class TrainingCreateDraftChangedError extends Error {
  constructor() {
    super("保存結果の確認待ち中に入力内容が変更されました。自動送信せず、履歴一覧を確認してください。");
    this.name = "TrainingCreateDraftChangedError";
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

  if (response.status === 409) {
    throw new TrainingCreateConflictError();
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
    memo: string | null;
    sets: Array<{
      setNumber: number;
      weightKg: number | null;
      reps: number;
      memo: string | null;
    }>;
  }>;
};

export type TrainingCreateApi = {
  createComplete: (
    idempotencyKey: string,
    request: TrainingSessionDraft
  ) => Promise<{ id: number }>;
};

export type PendingTrainingSave = {
  idempotencyKey: string;
  serializedDraft: string;
};

export type SaveAttemptGuard = {
  running: boolean;
  locked: boolean;
};

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

export function beginLockedRetry(guard: SaveAttemptGuard) {
  if (guard.running || !guard.locked) {
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

export function createPendingTrainingSave(
  draft: TrainingSessionDraft,
  keyFactory: () => string = () => crypto.randomUUID()
): PendingTrainingSave {
  return {
    idempotencyKey: keyFactory(),
    serializedDraft: JSON.stringify(draft)
  };
}

export function pendingSaveMatchesDraft(
  pendingSave: PendingTrainingSave,
  draft: TrainingSessionDraft
) {
  return pendingSave.serializedDraft === JSON.stringify(draft);
}

export async function saveTrainingSessionDraft(
  draft: TrainingSessionDraft,
  api: TrainingCreateApi,
  pendingSave: PendingTrainingSave
) {
  if (!pendingSaveMatchesDraft(pendingSave, draft)) {
    throw new TrainingCreateDraftChangedError();
  }

  const response = await api.createComplete(pendingSave.idempotencyKey, draft);
  return response.id;
}
