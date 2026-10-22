import { NextRequest, NextResponse } from "next/server";

export function proxy(request: NextRequest) {
  return NextResponse.redirect(new URL("/training-sessions", request.url));
}

export const config = {
  matcher: [
    "/dashboard/:path*",
    "/records/:path*",
    "/training/:path*",
    "/meals/:path*",
    "/foods/:path*"
  ]
};
