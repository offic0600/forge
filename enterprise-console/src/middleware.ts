import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { auth } from "@/auth";

// In Next.js 15 standalone mode, NextResponse.rewrite() with an absolute URL
// triggers an internal HTTP proxy request. When next-intl's createIntlMiddleware
// does this rewrite to inject locale context, it proxies to itself → ECONNRESET loop.
// Fix: handle locale routing with NextResponse.redirect() (browser-side) instead.
// next-intl's getRequestConfig reads locale from requestLocale (URL params), so
// the intl middleware rewrite is not needed for i18n to work in App Router.

const locales = ["zh", "en"];
const defaultLocale = "zh";

export default auth((req: NextRequest & { auth: unknown }) => {
  const { pathname } = req.nextUrl;

  // API routes: skip auth check (handled server-side)
  if (pathname.startsWith("/api/")) {
    return NextResponse.next();
  }

  // Redirect unauthenticated users to sign-in
  if (!req.auth) {
    return NextResponse.redirect(new URL("/api/auth/signin", req.url));
  }

  // Add locale prefix via redirect (NOT rewrite — avoids Next.js internal proxy loop)
  const hasLocale = locales.some(
    (l) => pathname === `/${l}` || pathname.startsWith(`/${l}/`)
  );
  if (!hasLocale) {
    const url = req.nextUrl.clone();
    url.pathname = `/${defaultLocale}${pathname}`;
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
});

export const config = {
  matcher: ["/((?!_next|.*\\...*).*)"],
};
