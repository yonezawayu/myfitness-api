/** Static headers only. CSP is generated per request in proxy.ts, never at build time. */
export function securityHeaders(nodeEnv, publicOrigin) {
  if (nodeEnv !== "production") return [];
  const rules = [{
    source: "/:path*",
    headers: [
      { key: "X-Content-Type-Options", value: "nosniff" },
      { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
      { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
      { key: "X-Frame-Options", value: "DENY" }
    ]
  }];
  if (publicOrigin !== undefined && publicOrigin !== "") {
    let url;
    try { url = new URL(publicOrigin); } catch { throw new Error("Invalid MYFITNESS_FRONTEND_PUBLIC_ORIGIN"); }
    if (url.protocol !== "https:" || url.username || url.password || url.search || url.hash
        || url.pathname !== "/" || url.hostname === "localhost" || url.hostname.endsWith(".localhost")
        || url.hostname.startsWith("127.") || url.hostname === "[::1]" || url.hostname === "0.0.0.0") {
      throw new Error("MYFITNESS_FRONTEND_PUBLIC_ORIGIN must be a public HTTPS origin");
    }
    // Only the explicitly configured production host; never set HSTS on localhost.
    rules.push({
      source: "/:path*",
      has: [{ type: "host", value: url.host.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") }],
      headers: [{ key: "Strict-Transport-Security", value: "max-age=31536000" }]
    });
  }
  return rules;
}
