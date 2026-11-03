export type RegistrationFormValues = {
  email: string;
  password: string;
  passwordConfirmation: string;
};

export type RegistrationFieldErrors = Partial<Record<keyof RegistrationFormValues, string>>;

export type RegistrationValidationResult = {
  email: string;
  password: string;
  errors: RegistrationFieldErrors;
};

export type UserRegistrationErrorCode =
  | "duplicate-email"
  | "validation"
  | "network"
  | "server"
  | "unexpected";

export class UserRegistrationError extends Error {
  readonly code: UserRegistrationErrorCode;

  constructor(code: UserRegistrationErrorCode, message: string) {
    super(message);
    this.code = code;
    this.name = "UserRegistrationError";
  }
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+$/;

export function validateRegistration(values: RegistrationFormValues): RegistrationValidationResult {
  const email = values.email.trim();
  const errors: RegistrationFieldErrors = {};

  if (!email) {
    errors.email = "メールアドレスを入力してください。";
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = "有効なメールアドレスを入力してください。";
  } else if (email.length > 255) {
    errors.email = "メールアドレスは255文字以内で入力してください。";
  }

  if (!values.password) {
    errors.password = "パスワードを入力してください。";
  } else if (values.password.length < 8 || values.password.length > 72) {
    errors.password = "パスワードは8文字以上72文字以内で入力してください。";
  }

  if (!values.passwordConfirmation) {
    errors.passwordConfirmation = "確認用パスワードを入力してください。";
  } else if (values.password !== values.passwordConfirmation) {
    errors.passwordConfirmation = "パスワードが一致しません。";
  }

  return { email, password: values.password, errors };
}

export function registrationErrorForStatus(status: number): UserRegistrationError | null {
  if (status >= 200 && status < 300) {
    return null;
  }

  if (status === 400) {
    return new UserRegistrationError("validation", "入力内容を確認してください。");
  }

  if (status === 409) {
    return new UserRegistrationError("duplicate-email", "このメールアドレスは既に登録されています。");
  }

  if (status >= 500) {
    return new UserRegistrationError(
      "server",
      "サーバーで問題が発生しました。時間をおいて、もう一度お試しください。"
    );
  }

  return new UserRegistrationError("unexpected", "ユーザー登録に失敗しました。もう一度お試しください。");
}

export function networkRegistrationError() {
  return new UserRegistrationError(
    "network",
    "サーバーに接続できませんでした。通信環境を確認して、もう一度お試しください。"
  );
}
