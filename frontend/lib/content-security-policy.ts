/** Production CSP. The nonce must be fresh, cryptographically random, and server-generated. */
export function productionContentSecurityPolicy(apiBaseUrl: string, nonce: string): string {
  if (!/^[A-Za-z0-9+/]{22,}={0,2}$/.test(nonce)) {
    throw new Error("Invalid CSP nonce");
  }
  const api = new URL(apiBaseUrl);
  if (api.protocol !== "https:" || api.username || api.password || api.search || api.hash
      || api.hostname === "localhost" || api.hostname.endsWith(".localhost")
      || api.hostname.startsWith("127.") || api.hostname === "[::1]") {
    throw new Error("Production CSP requires a public HTTPS API URL");
  }
  return [
    "default-src 'none'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'`,
    "script-src-attr 'none'",
    `style-src 'self' 'nonce-${nonce}'`,
    "style-src-attr 'none'",
    `connect-src 'self' ${api.origin}`,
    "img-src 'self'",
    "font-src 'self'",
    "manifest-src 'self'",
    "frame-src 'none'",
    "frame-ancestors 'none'",
    "object-src 'none'",
    "base-uri 'none'",
    "form-action 'self'",
    "worker-src 'none'",
    "media-src 'none'"
  ].join("; ");
}
