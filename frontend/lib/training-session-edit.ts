import type { TrainingSessionResponse } from "./api";

export type SetForm = {
  id: number | null;
  formId: string;
  setNumber: number;
  originalSetNumber: number | null;
  weight: string;
  reps: string;
  memo: string | null;
};

export type ExerciseForm = {
  id: number | null;
  formId: string;
  exerciseName: string;
  memo: string | null;
  sets: SetForm[];
  markedForDeletion: boolean;
};

export type SessionForm = {
  id: number;
  date: string;
  memo: string;
  exercises: ExerciseForm[];
};

export type SetOperations = {
  setsToCreate: Array<{ exerciseId: number; trainingSet: SetForm }>;
  setsToUpdate: Array<{ exerciseId: number; setId: number; trainingSet: SetForm }>;
  setsToDelete: Array<{ exerciseId: number; setId: number }>;
};

export type MutationProgress = {
  hasStarted: boolean;
  completedCount: number;
};

export function toSessionForm(session: TrainingSessionResponse): SessionForm {
  return {
    id: session.id,
    date: session.date,
    memo: session.memo ?? "",
    exercises: session.exercises.map((exercise) => ({
      id: exercise.id,
      formId: `existing-exercise-${exercise.id}`,
      exerciseName: exercise.exerciseName,
      memo: exercise.memo,
      markedForDeletion: false,
      sets: exercise.sets.map((trainingSet, setIndex) => ({
        id: trainingSet.id,
        formId: `existing-${trainingSet.id}`,
        setNumber: setIndex + 1,
        originalSetNumber: trainingSet.setNumber,
        weight: trainingSet.weightKg === null ? "" : String(trainingSet.weightKg),
        reps: String(trainingSet.reps),
        memo: trainingSet.memo
      }))
    }))
  };
}

export function weightValue(value: string) {
  return value.trim() === "" ? null : Number(value);
}

export function renumberSets(sets: SetForm[]) {
  return sets.map((trainingSet, setIndex) => ({ ...trainingSet, setNumber: setIndex + 1 }));
}

function setValueChanged(currentSet: SetForm, originalSet: SetForm) {
  return (
    currentSet.setNumber !== originalSet.originalSetNumber ||
    weightValue(currentSet.weight) !== weightValue(originalSet.weight) ||
    Number(currentSet.reps) !== Number(originalSet.reps)
  );
}

export function createSetOperations(currentSession: SessionForm, originalSession: SessionForm): SetOperations {
  const originalSets = new Map<number, { exerciseId: number; trainingSet: SetForm }>();
  originalSession.exercises.forEach((exercise) => {
    const currentExercise = currentSession.exercises.find(
      (candidate) => candidate.id === exercise.id && !candidate.markedForDeletion
    );
    if (!currentExercise || exercise.id === null) {
      return;
    }

    const exerciseId = exercise.id;
    exercise.sets.forEach((trainingSet) => {
      if (trainingSet.id !== null) {
        originalSets.set(trainingSet.id, { exerciseId, trainingSet });
      }
    });
  });

  const currentExistingSetIds = new Set<number>();
  const setsToCreate: SetOperations["setsToCreate"] = [];
  const setsToUpdate: SetOperations["setsToUpdate"] = [];

  currentSession.exercises.forEach((exercise) => {
    if (exercise.id === null || exercise.markedForDeletion) {
      return;
    }

    const exerciseId = exercise.id;
    exercise.sets.forEach((trainingSet) => {
      if (trainingSet.id === null) {
        setsToCreate.push({ exerciseId, trainingSet });
        return;
      }

      currentExistingSetIds.add(trainingSet.id);
      const originalSet = originalSets.get(trainingSet.id)?.trainingSet;
      if (originalSet && setValueChanged(trainingSet, originalSet)) {
        setsToUpdate.push({ exerciseId, setId: trainingSet.id, trainingSet });
      }
    });
  });

  const setsToDelete = Array.from(originalSets.entries())
    .filter(([setId]) => !currentExistingSetIds.has(setId))
    .map(([setId, originalSet]) => ({ exerciseId: originalSet.exerciseId, setId }));

  return { setsToCreate, setsToUpdate, setsToDelete };
}

export function createMutationProgress(): MutationProgress {
  return { hasStarted: false, completedCount: 0 };
}

export async function runTrackedMutation<T>(progress: MutationProgress, mutation: () => Promise<T>): Promise<T> {
  progress.hasStarted = true;
  const result = await mutation();
  progress.completedCount += 1;
  return result;
}

export function shouldLockAfterMutationError(progress: MutationProgress) {
  return progress.hasStarted;
}

export function reloadLockAfterFetch(succeeded: boolean) {
  return !succeeded;
}
