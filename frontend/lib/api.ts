export const API_BASE_URL = "http://localhost:8080";
export const TOKEN_STORAGE_KEY = "myfitness_access_token";

export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
};

export type DashboardResponse = {
  totalCalories: number;
  totalProtein: number;
  totalFat?: number;
  totalCarbs?: number;
  todayWeight: number | null;
  totalTrainingCalories: number;
};

export type CreateRecordRequest = {
  weight: number;
  calories: number;
  date: string;
};

export type RecordResponse = {
  id: number;
  weight: number;
  date: string;
};

export type CreateTrainingLogRequest = {
  trainingName: string;
  caloriesBurned: number;
  date: string;
};

export type TrainingLogResponse = {
  id: number;
  trainingName: string;
  caloriesBurned: number;
  date: string;
};

export type TrainingSetResponse = {
  id: number;
  setNumber: number;
  weightKg: number | null;
  reps: number;
  memo: string | null;
};

export type TrainingExerciseResponse = {
  id: number;
  exerciseName: string;
  memo: string | null;
  sets: TrainingSetResponse[];
};

export type TrainingSessionResponse = {
  id: number;
  date: string;
  memo: string | null;
  exercises: TrainingExerciseResponse[];
};

export type CreateTrainingSessionRequest = {
  date: string;
  memo: string | null;
};

export type CreateTrainingExerciseRequest = {
  exerciseName: string;
  memo: string | null;
};

export type CreateTrainingSetRequest = {
  setNumber: number;
  weightKg: number;
  reps: number;
  memo: string | null;
};

export type CreateFoodRequest = {
  name: string;
  caloriesPer100g: number;
  proteinPer100g: number;
  fatPer100g?: number;
  carbPer100g?: number;
};

export type Food = {
  id: number;
  name: string;
  caloriesPer100g: number;
  proteinPer100g: number;
  fatPer100g: number;
  carbPer100g: number;
};

export type CreateMealLogRequest = {
  mealName: string;
  date: string;
};

export type MealLogResponse = {
  id: number;
  mealName: string;
  date: string;
  memo?: string | null;
  totalCalories?: number;
  totalProtein?: number;
  totalFat?: number;
  totalCarbs?: number;
};

export type MealItemResponse = {
  id: number;
  mealLogId: number;
  foodId: number;
  quantityG: number;
  amountG?: number;
  quantityGrams?: number;
  calories: number;
  protein: number;
  fat?: number;
  carbs?: number;
};

export type CreateMealItemRequest = {
  mealLogId: number;
  foodId: number;
  amountG: number;
};

export async function login(email: string, password: string): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ email, password })
  });

  if (!response.ok) {
    throw new Error("ログインに失敗しました。メールアドレスとパスワードを確認してください。");
  }

  return response.json();
}

export async function fetchTodayDashboard(token: string): Promise<DashboardResponse> {
  const response = await fetch(`${API_BASE_URL}/dashboard/today`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    cache: "no-store"
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("ダッシュボードの取得に失敗しました。");
  }

  return response.json();
}

export async function createRecord(token: string, request: CreateRecordRequest): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/records`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("Recordの保存に失敗しました。入力内容を確認してください。");
  }
}

export async function createTrainingLog(token: string, request: CreateTrainingLogRequest): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/training-logs`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("トレーニング記録の保存に失敗しました。入力内容を確認してください。");
  }
}

export async function createFood(token: string, request: CreateFoodRequest): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/foods`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      ...request,
      fatPer100g: request.fatPer100g ?? 0,
      carbPer100g: request.carbPer100g ?? 0
    })
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("食品の保存に失敗しました。入力内容を確認してください。");
  }
}

export async function fetchFoods(token: string): Promise<Food[]> {
  const response = await fetch(`${API_BASE_URL}/foods`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    cache: "no-store"
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("Food一覧の取得に失敗しました。");
  }

  return response.json();
}

export async function createMealLog(token: string, request: CreateMealLogRequest): Promise<MealLogResponse> {
  const response = await fetch(`${API_BASE_URL}/meal-logs`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      mealName: request.mealName,
      calories: 0,
      date: request.date,
      memo: "",
      protein: 0
    })
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("MealLogの保存に失敗しました。入力内容を確認してください。");
  }

  return response.json();
}

export async function createMealItem(token: string, request: CreateMealItemRequest): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/meal-items`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      mealLogId: request.mealLogId,
      foodId: request.foodId,
      quantityG: request.amountG
    })
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("MealItemの保存に失敗しました。入力内容を確認してください。");
  }
}

export async function fetchMealLogs(token: string): Promise<MealLogResponse[]> {
  const response = await fetch(`${API_BASE_URL}/meal-logs`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    cache: "no-store"
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("Meal履歴の取得に失敗しました。");
  }

  return response.json();
}

export async function fetchMealItems(token: string, mealLogId: number): Promise<MealItemResponse[]> {
  const response = await fetch(`${API_BASE_URL}/meal-items/meal-log/${mealLogId}`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    cache: "no-store"
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("MealItemの取得に失敗しました。");
  }

  return response.json();
}

export async function deleteMealLog(token: string, mealLogId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/meal-logs/${mealLogId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("Mealの削除に失敗しました。");
  }
}

export async function fetchTrainingLogs(token: string): Promise<TrainingLogResponse[]> {
  const response = await fetch(`${API_BASE_URL}/training-logs`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    cache: "no-store"
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("トレーニング履歴の取得に失敗しました。");
  }

  return response.json();
}

export async function deleteTrainingLog(token: string, trainingLogId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/training-logs/${trainingLogId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("トレーニング記録の削除に失敗しました。");
  }
}

export async function fetchTrainingSessions(token: string): Promise<TrainingSessionResponse[]> {
  const response = await fetch(`${API_BASE_URL}/training-sessions`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    cache: "no-store"
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("本格トレーニング履歴の取得に失敗しました。");
  }

  return response.json();
}

export async function createTrainingSession(
  token: string,
  request: CreateTrainingSessionRequest
): Promise<TrainingSessionResponse> {
  const response = await fetch(`${API_BASE_URL}/training-sessions`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("Training Sessionの保存に失敗しました。入力内容を確認してください。");
  }

  return response.json();
}

export async function createTrainingExercise(
  token: string,
  sessionId: number,
  request: CreateTrainingExerciseRequest
): Promise<TrainingExerciseResponse> {
  const response = await fetch(`${API_BASE_URL}/training-sessions/${sessionId}/exercises`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("Training Exerciseの保存に失敗しました。入力内容を確認してください。");
  }

  return response.json();
}

export async function createTrainingSet(
  token: string,
  sessionId: number,
  exerciseId: number,
  request: CreateTrainingSetRequest
): Promise<TrainingSetResponse> {
  const response = await fetch(`${API_BASE_URL}/training-sessions/${sessionId}/exercises/${exerciseId}/sets`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("Training Setの保存に失敗しました。入力内容を確認してください。");
  }

  return response.json();
}

export async function fetchRecords(token: string): Promise<RecordResponse[]> {
  const response = await fetch(`${API_BASE_URL}/records`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    cache: "no-store"
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("体重履歴の取得に失敗しました。");
  }

  return response.json();
}

export async function deleteRecord(token: string, recordId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/records/${recordId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });

  if (response.status === 401 || response.status === 403) {
    throw new Error("認証情報が無効です。もう一度ログインしてください。");
  }

  if (!response.ok) {
    throw new Error("体重記録の削除に失敗しました。");
  }
}
