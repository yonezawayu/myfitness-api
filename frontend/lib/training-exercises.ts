export type TrainingExerciseGroup = {
  bodyPart: string;
  exercises: string[];
};

export const TRAINING_EXERCISE_GROUPS: TrainingExerciseGroup[] = [
  {
    bodyPart: "胸",
    exercises: ["ベンチプレス", "インクラインベンチプレス", "ダンベルフライ", "チェストプレス"]
  },
  {
    bodyPart: "背中",
    exercises: ["デッドリフト", "ラットプルダウン", "シーテッドロー", "ケーブルプルオーバー"]
  },
  {
    bodyPart: "脚",
    exercises: ["スクワット", "ルーマニアンデッドリフト", "レッグプレス", "レッグカール", "カーフレイズ"]
  },
  {
    bodyPart: "肩",
    exercises: ["ショルダープレス", "サイドレイズ", "リアレイズ"]
  },
  {
    bodyPart: "腕",
    exercises: ["バーベルカール", "インクラインカール", "トライセプスエクステンション", "プレスダウン"]
  },
  {
    bodyPart: "腹筋",
    exercises: ["クランチ", "プランク", "レッグレイズ"]
  },
  {
    bodyPart: "その他",
    exercises: ["その他"]
  }
];

export const TRAINING_EXERCISES = TRAINING_EXERCISE_GROUPS.flatMap((group) => group.exercises);
