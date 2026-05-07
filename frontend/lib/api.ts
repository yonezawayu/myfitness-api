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
