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
  todayWeight: number | null;
  totalTrainingCalories: number;
};

export type CreateRecordRequest = {
  weight: number;
  calories: number;
  date: string;
};

export type CreateTrainingLogRequest = {
  trainingName: string;
  caloriesBurned: number;
  date: string;
};

export type CreateFoodRequest = {
  name: string;
  caloriesPer100g: number;
  proteinPer100g: number;
};

export type Food = {
  id: number;
  name: string;
  caloriesPer100g: number;
  proteinPer100g: number;
};

export type CreateMealLogRequest = {
  mealName: string;
  date: string;
};

export type MealLogResponse = {
  id: number;
  mealName: string;
  date: string;
};

export type MealItemResponse = {
  id: number;
  mealLogId: number;
  foodId: number;
  quantityG: number;
  calories: number;
  protein: number;
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
      fatPer100g: 0,
      carbPer100g: 0
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
