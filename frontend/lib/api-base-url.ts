const LOCAL_API_BASE_URL = "http://localhost:8080";

function isLoopbackHostname(hostname: string) {
  const normalizedHostname = hostname.toLowerCase();
  return (
    normalizedHostname === "localhost" ||
    normalizedHostname.endsWith(".localhost") ||
    normalizedHostname.startsWith("127.") ||
    normalizedHostname === "[::1]" ||
    normalizedHostname === "::1"
  );
}

export function resolveApiBaseUrl(configuredValue: string | undefined, nodeEnv: string | undefined) {
  const isProduction = nodeEnv === "production";
  const candidate = configuredValue?.trim() || (isProduction ? "" : LOCAL_API_BASE_URL);

  if (!candidate) {
    throw new Error("NEXT_PUBLIC_API_BASE_URL must be set for a production build.");
  }

  let url: URL;
  try {
    url = new URL(candidate);
  } catch {
    throw new Error("NEXT_PUBLIC_API_BASE_URL must be a valid absolute URL.");
  }

  if (url.protocol !== "http:" && url.protocol !== "https:") {
    throw new Error("NEXT_PUBLIC_API_BASE_URL must use http or https.");
  }

  if (url.username || url.password || url.search || url.hash) {
    throw new Error("NEXT_PUBLIC_API_BASE_URL must not include credentials, a query, or a fragment.");
  }

  if (isProduction && url.protocol !== "https:") {
    throw new Error("NEXT_PUBLIC_API_BASE_URL must use https for a production build.");
  }

  if (isProduction && isLoopbackHostname(url.hostname)) {
    throw new Error("NEXT_PUBLIC_API_BASE_URL must not target localhost in a production build.");
  }

  const normalizedPath = url.pathname.replace(/\/+$/, "");
  return `${url.origin}${normalizedPath}`;
}

export const API_BASE_URL = resolveApiBaseUrl(
  process.env.NEXT_PUBLIC_API_BASE_URL,
  process.env.NODE_ENV
);
