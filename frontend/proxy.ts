import { NextRequest, NextResponse } from "next/server";
import { randomBytes } from "node:crypto";
import { API_BASE_URL } from "./lib/api-base-url";
import { productionContentSecurityPolicy } from "./lib/content-security-policy";

export function proxy(request: NextRequest) {
  const legacy = ["/dashboard", "/records", "/training", "/meals", "/foods"]
    .some(path => request.nextUrl.pathname === path || request.nextUrl.pathname.startsWith(`${path}/`));
  const requestHeaders = new Headers(request.headers);
  // Never accept a caller's nonce or CSP as trusted input to server rendering.
  requestHeaders.delete("x-nonce");
  requestHeaders.delete("Content-Security-Policy");
  let csp: string | undefined;
  if (process.env.NODE_ENV === "production") {
    const nonce = randomBytes(16).toString("base64");
    csp = productionContentSecurityPolicy(API_BASE_URL, nonce);
    requestHeaders.set("x-nonce", nonce);
    requestHeaders.set("Content-Security-Policy", csp);
  }
  const response = legacy
    ? NextResponse.redirect(new URL("/training-sessions", request.url))
    : NextResponse.next({ request: { headers: requestHeaders } });
  if (csp) {
    response.headers.set("Content-Security-Policy", csp);
    // Nonce-bearing HTML/RSC must not be reused across requests by a CDN or proxy.
    response.headers.set("Cache-Control", "private, no-store");
  }
  return response;
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico).*)"
  ]
};
