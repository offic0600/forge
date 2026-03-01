import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { auth } from "@/auth";
import createIntlMiddleware from "next-intl/middleware";
import { routing } from "../i18n/routing";

const intlMiddleware = createIntlMiddleware(routing);

export default auth((req: NextRequest & { auth: unknown }) => {
  const isAuthRoute = req.nextUrl.pathname.startsWith("/api/auth");
  const isApiRoute = req.nextUrl.pathname.startsWith("/api/");

  // Skip auth check for API routes (handled server-side) and auth routes
  if (isApiRoute) {
    return NextResponse.next();
  }

  // Redirect unauthenticated users to sign-in
  if (!req.auth && !isAuthRoute) {
    return NextResponse.redirect(new URL("/api/auth/signin", req.url));
  }

  return intlMiddleware(req);
});

export const config = {
  matcher: ["/((?!_next|.*\\...*).*)"],
};
