import createMiddleware from "next-intl/middleware";
import { routing } from "../i18n/routing";

export default createMiddleware(routing);

export const config = {
  // Exclude _next internals, static files, api routes, and auth/callback
  // from locale prefixing — the PKCE callback page lives at /auth/callback
  // (not under [locale]) and must be served without a locale prefix.
  matcher: ["/((?!_next|.*\\...*|api|auth).*)"],
};
