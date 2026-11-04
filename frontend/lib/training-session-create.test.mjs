import assert from "node:assert/strict";
import test from "node:test";

import {
  TrainingCreateConflictError,
  TrainingCreateDraftChangedError,
  TrainingCreateResultUnknownError,
  beginLockedRetry,
  beginSaveAttempt,
  createPendingTrainingSave,
  createSaveAttemptGuard,
  executeTrainingCreateRequest,
  finishSaveAttempt,
  lockSaveAttempt,
  pendingSaveMatchesDraft,
  saveTrainingSessionDraft
} from "./training-session-create.ts";

function draft() {
  return {
    date: "2026-11-03",
    memo: "test",
    exercises: [{
      exerciseName: "ベンチプレス",
      memo: null,
      sets: [
        { setNumber: 1, weightKg: 60, reps: 10, memo: null },
        { setNumber: 2, weightKg: 65, reps: 8, memo: null }
      ]
    }]
  };
}

test("一括APIを1回だけ呼び出して作成結果を返す", async () => {
  const input = draft();
  const pending = createPendingTrainingSave(input, () => "key-1");
  const calls = [];
  const id = await saveTrainingSessionDraft(input, {
    createComplete: async (key, request) => {
      calls.push({ key, request });
      return { id: 10 };
    }
  }, pending);

  assert.equal(id, 10);
  assert.equal(calls.length, 1);
  assert.equal(calls[0].key, "key-1");
  assert.deepEqual(calls[0].request, input);
});

test("保存開始直後の連打を拒否する", () => {
  const guard = createSaveAttemptGuard();
  assert.equal(beginSaveAttempt(guard), true);
  assert.equal(beginSaveAttempt(guard), false);
  finishSaveAttempt(guard);
  assert.equal(beginSaveAttempt(guard), true);
});

test("結果不明ロック後は通常保存を拒否し明示的再試行だけ許可する", () => {
  const guard = createSaveAttemptGuard();
  assert.equal(beginSaveAttempt(guard), true);
  lockSaveAttempt(guard);
  assert.equal(beginSaveAttempt(guard), false);
  assert.equal(beginLockedRetry(guard), true);
  assert.equal(beginLockedRetry(guard), false);
});

test("同じ保存操作の再試行は同じ冪等キーを使用する", async () => {
  const input = draft();
  const pending = createPendingTrainingSave(input, () => "same-key");
  const keys = [];
  const api = {
    createComplete: async (key) => {
      keys.push(key);
      if (keys.length === 1) {
        throw new TrainingCreateResultUnknownError("lost");
      }
      return { id: 11 };
    }
  };

  await assert.rejects(saveTrainingSessionDraft(input, api, pending), TrainingCreateResultUnknownError);
  assert.equal(await saveTrainingSessionDraft(input, api, pending), 11);
  assert.deepEqual(keys, ["same-key", "same-key"]);
});

test("新しい保存操作には新しい冪等キーを生成する", () => {
  let index = 0;
  const keyFactory = () => `key-${++index}`;
  assert.notEqual(
    createPendingTrainingSave(draft(), keyFactory).idempotencyKey,
    createPendingTrainingSave(draft(), keyFactory).idempotencyKey
  );
});

test("結果不明後に入力が変わった場合はPOSTしない", async () => {
  const original = draft();
  const changed = structuredClone(original);
  changed.memo = "changed";
  const pending = createPendingTrainingSave(original, () => "key");
  let callCount = 0;

  assert.equal(pendingSaveMatchesDraft(pending, changed), false);
  await assert.rejects(
    saveTrainingSessionDraft(changed, {
      createComplete: async () => {
        callCount += 1;
        return { id: 1 };
      }
    }, pending),
    TrainingCreateDraftChangedError
  );
  assert.equal(callCount, 0);
});

test("通信断、5xx、成功レスポンス解析失敗は保存結果不明", async () => {
  await assert.rejects(
    executeTrainingCreateRequest(async () => { throw new Error("lost"); }, "failed"),
    TrainingCreateResultUnknownError
  );
  await assert.rejects(
    executeTrainingCreateRequest(async () => new Response("", { status: 500 }), "failed"),
    TrainingCreateResultUnknownError
  );
  await assert.rejects(
    executeTrainingCreateRequest(async () => new Response("not-json", { status: 201 }), "failed"),
    TrainingCreateResultUnknownError
  );
});

test("409は冪等キー競合として分類する", async () => {
  await assert.rejects(
    executeTrainingCreateRequest(async () => new Response("", { status: 409 }), "failed"),
    TrainingCreateConflictError
  );
});

test("明確な4xxでは通常エラーを返す", async () => {
  await assert.rejects(
    executeTrainingCreateRequest(async () => new Response("", { status: 400 }), "入力エラー"),
    (error) => error instanceof Error && error.message === "入力エラー"
  );
});
