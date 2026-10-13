import assert from "node:assert/strict";
import test from "node:test";

import {
  createMutationProgress,
  createSetOperations,
  reloadLockAfterFetch,
  renumberSets,
  runTrackedMutation,
  shouldLockAfterMutationError,
  toSessionForm
} from "./training-session-edit.ts";

function sessionResponse(setNumbers) {
  return {
    id: 1,
    date: "2026-10-13",
    memo: null,
    exercises: [
      {
        id: 10,
        exerciseName: "ベンチプレス",
        memo: null,
        sets: setNumbers.map((setNumber, index) => ({
          id: 100 + index,
          setNumber,
          weightKg: 60,
          reps: 10,
          memo: null
        }))
      }
    ]
  };
}

test("欠番と重複のあるセット番号を配列順に正規化し、既存IDをPUT対象にする", () => {
  const cases = [
    { actual: [1, 3], updateIds: [101], normalized: [1, 2] },
    { actual: [1, 2, 4], updateIds: [102], normalized: [1, 2, 3] },
    { actual: [1, 1, 3], updateIds: [101], normalized: [1, 2, 3] }
  ];

  for (const testCase of cases) {
    const original = toSessionForm(sessionResponse(testCase.actual));
    const current = structuredClone(original);
    const operations = createSetOperations(current, original);

    assert.deepEqual(
      current.exercises[0].sets.map((trainingSet) => trainingSet.setNumber),
      testCase.normalized
    );
    assert.deepEqual(
      operations.setsToUpdate.map(({ setId }) => setId),
      testCase.updateIds
    );
  }
});

test("セット削除後に既存IDを維持して1から再採番する", () => {
  const original = toSessionForm(sessionResponse([1, 2, 3]));
  const current = structuredClone(original);
  current.exercises[0].sets = renumberSets(current.exercises[0].sets.filter(({ id }) => id !== 100));

  const operations = createSetOperations(current, original);

  assert.deepEqual(operations.setsToDelete, [{ exerciseId: 10, setId: 100 }]);
  assert.deepEqual(
    operations.setsToUpdate.map(({ setId, trainingSet }) => [setId, trainingSet.setNumber]),
    [
      [101, 1],
      [102, 2]
    ]
  );
});

test("セット追加後も1からの連番でPOST対象にする", () => {
  const original = toSessionForm(sessionResponse([1, 2]));
  const current = structuredClone(original);
  current.exercises[0].sets.push({
    id: null,
    formId: "new-test-set",
    setNumber: 3,
    originalSetNumber: null,
    weight: "60",
    reps: "10",
    memo: null
  });

  const operations = createSetOperations(current, original);

  assert.equal(operations.setsToUpdate.length, 0);
  assert.equal(operations.setsToCreate.length, 1);
  assert.equal(operations.setsToCreate[0].trainingSet.setNumber, 3);
});

test("変更がなければ更新計画が空になりAPI呼び出し条件を満たさない", () => {
  const original = toSessionForm(sessionResponse([1, 2, 3]));
  const current = structuredClone(original);
  const operations = createSetOperations(current, original);
  const plannedUpdateCount =
    operations.setsToCreate.length + operations.setsToUpdate.length + operations.setsToDelete.length;
  let apiCallCount = 0;

  if (plannedUpdateCount > 0) {
    apiCallCount += 1;
  }

  assert.equal(plannedUpdateCount, 0);
  assert.equal(apiCallCount, 0);
});

test("最初のAPIが応答前に失敗しても保存ロックする", async () => {
  const progress = createMutationProgress();
  let serverApplied = false;

  await assert.rejects(
    runTrackedMutation(progress, async () => {
      serverApplied = true;
      throw new Error("connection lost");
    })
  );

  assert.equal(serverApplied, true);
  assert.equal(progress.completedCount, 0);
  assert.equal(shouldLockAfterMutationError(progress), true);
});

test("2件目以降のAPI失敗でも成功件数を保持して保存ロックする", async () => {
  const progress = createMutationProgress();

  await runTrackedMutation(progress, async () => undefined);
  await assert.rejects(
    runTrackedMutation(progress, async () => {
      throw new Error("connection lost");
    })
  );

  assert.equal(progress.completedCount, 1);
  assert.equal(shouldLockAfterMutationError(progress), true);
});

test("再取得失敗ではロックを維持し、成功後は解除する", () => {
  assert.equal(reloadLockAfterFetch(false), true);
  assert.equal(reloadLockAfterFetch(true), false);
});
