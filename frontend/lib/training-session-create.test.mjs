import assert from "node:assert/strict";
import test from "node:test";

import {
  TrainingCreateResultUnknownError,
  beginSaveAttempt,
  createSaveAttemptGuard,
  createTrainingCreateProgress,
  executeTrainingCreateRequest,
  finishSaveAttempt,
  lockSaveAttempt,
  recoveryAfterCreateError,
  saveTrainingSessionDraft
} from "./training-session-create.ts";

function draft() {
  return {
    date: "2026-11-03",
    memo: "test",
    exercises: [
      {
        exerciseName: "ベンチプレス",
        sets: [
          { setNumber: 1, weightKg: 60, reps: 10 },
          { setNumber: 2, weightKg: 65, reps: 8 }
        ]
      }
    ]
  };
}

test("全API成功時はSession、Exercise、Setを順番に1回ずつ作成する", async () => {
  const calls = [];
  const progress = createTrainingCreateProgress();
  const input = draft();
  const originalInput = structuredClone(input);
  const sessionId = await saveTrainingSessionDraft(
    input,
    {
      createSession: async () => {
        calls.push("session");
        return { id: 10 };
      },
      createExercise: async (createdSessionId) => {
        calls.push(`exercise:${createdSessionId}`);
        return { id: 20 };
      },
      createSet: async (createdSessionId, exerciseId, request) => {
        calls.push(`set:${createdSessionId}:${exerciseId}:${request.setNumber}`);
      }
    },
    progress
  );

  assert.equal(sessionId, 10);
  assert.deepEqual(calls, ["session", "exercise:10", "set:10:20:1", "set:10:20:2"]);
  assert.equal(progress.completedCount, 4);
  assert.deepEqual(input, originalInput);
});

test("保存開始直後の連打を拒否し、明確な失敗後だけ再試行できる", () => {
  const guard = createSaveAttemptGuard();

  assert.equal(beginSaveAttempt(guard), true);
  assert.equal(beginSaveAttempt(guard), false);
  finishSaveAttempt(guard);
  assert.equal(beginSaveAttempt(guard), true);
});

test("保存結果不明でロックした後は自動再POSTできない", () => {
  const guard = createSaveAttemptGuard();
  assert.equal(beginSaveAttempt(guard), true);
  lockSaveAttempt(guard);

  assert.equal(beginSaveAttempt(guard), false);
});

test("通信断と成功レスポンス解析失敗を保存結果不明として分類する", async () => {
  await assert.rejects(
    executeTrainingCreateRequest(async () => {
      throw new Error("connection lost");
    }, "rejected"),
    TrainingCreateResultUnknownError
  );

  await assert.rejects(
    executeTrainingCreateRequest(
      async () => new Response("not-json", { status: 201 }),
      "rejected"
    ),
    TrainingCreateResultUnknownError
  );
});

test("4xxレスポンスは明確な失敗として扱う", async () => {
  await assert.rejects(
    executeTrainingCreateRequest(
      async () => new Response("", { status: 400 }),
      "入力内容を確認してください。"
    ),
    (error) =>
      error instanceof Error &&
      !(error instanceof TrainingCreateResultUnknownError) &&
      error.message === "入力内容を確認してください。"
  );
});

test("5xxレスポンスは保存結果不明として扱う", async () => {
  await assert.rejects(
    executeTrainingCreateRequest(
      async () => new Response("", { status: 500 }),
      "保存に失敗しました。"
    ),
    TrainingCreateResultUnknownError
  );
});

test("Session作成前の明確なHTTP失敗では復旧ロックしない", () => {
  const progress = createTrainingCreateProgress();
  progress.mutationStarted = true;
  progress.currentStage = { kind: "session" };

  assert.equal(recoveryAfterCreateError(progress, new Error("400 Bad Request")), null);
});

test("Session作成後のExercise失敗ではSession ID付き復旧状態にする", async () => {
  const progress = createTrainingCreateProgress();

  await assert.rejects(
    saveTrainingSessionDraft(
      draft(),
      {
        createSession: async () => ({ id: 10 }),
        createExercise: async () => {
          throw new Error("Exercise rejected");
        },
        createSet: async () => undefined
      },
      progress
    )
  );

  assert.deepEqual(recoveryAfterCreateError(progress, new Error("Exercise rejected")), {
    kind: "partial",
    sessionId: 10,
    stageLabel: "種目1（ベンチプレス）の作成",
    completedCount: 1,
    resultUnknown: false
  });
});

test("Exercise作成後のSet失敗では完了応答数と失敗箇所を保持する", async () => {
  const progress = createTrainingCreateProgress();

  await assert.rejects(
    saveTrainingSessionDraft(
      draft(),
      {
        createSession: async () => ({ id: 10 }),
        createExercise: async () => ({ id: 20 }),
        createSet: async () => {
          throw new Error("Set rejected");
        }
      },
      progress
    )
  );

  const recovery = recoveryAfterCreateError(progress, new Error("Set rejected"));
  assert.equal(recovery?.sessionId, 10);
  assert.equal(recovery?.completedCount, 2);
  assert.match(recovery?.stageLabel ?? "", /セット1/);
});

test("Session POSTのレスポンス喪失時はID不明として履歴確認を要求する", async () => {
  const progress = createTrainingCreateProgress();
  const connectionLost = new TrainingCreateResultUnknownError("connection lost");

  await assert.rejects(
    saveTrainingSessionDraft(
      draft(),
      {
        createSession: async () => {
          throw connectionLost;
        },
        createExercise: async () => ({ id: 20 }),
        createSet: async () => undefined
      },
      progress
    ),
    TrainingCreateResultUnknownError
  );

  assert.deepEqual(recoveryAfterCreateError(progress, connectionLost), {
    kind: "unknown-session",
    sessionId: null,
    stageLabel: "Sessionの作成",
    completedCount: 0,
    resultUnknown: true
  });
});

test("Set POSTのレスポンス喪失時は作成済みSessionの編集導線を使用する", async () => {
  const progress = createTrainingCreateProgress();
  const connectionLost = new TrainingCreateResultUnknownError("connection lost");

  await assert.rejects(
    saveTrainingSessionDraft(
      draft(),
      {
        createSession: async () => ({ id: 10 }),
        createExercise: async () => ({ id: 20 }),
        createSet: async () => {
          throw connectionLost;
        }
      },
      progress
    ),
    TrainingCreateResultUnknownError
  );

  const recovery = recoveryAfterCreateError(progress, connectionLost);
  assert.equal(recovery?.kind, "partial");
  assert.equal(recovery?.sessionId, 10);
  assert.equal(recovery?.resultUnknown, true);
});
