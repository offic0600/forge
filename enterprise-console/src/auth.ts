import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";

export const { handlers, signIn, signOut, auth } = NextAuth({
  providers: [
    Keycloak({
      clientId: process.env.KEYCLOAK_CLIENT_ID!,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,
      issuer: process.env.KEYCLOAK_ISSUER!,
    }),
  ],
  callbacks: {
    jwt({ token, account, profile }) {
      if (account) {
        token.accessToken = account.access_token;
        token.realmRoles =
          (profile as Record<string, unknown> | undefined)
            ?.realm_access &&
          typeof (profile as Record<string, unknown>).realm_access === "object"
            ? (
                (profile as Record<string, unknown>).realm_access as Record<
                  string,
                  unknown
                >
              )["roles"] ?? []
            : [];
      }
      return token;
    },
    session({ session, token }) {
      session.accessToken = token.accessToken as string;
      (session.user as { realmRoles?: string[] }).realmRoles =
        token.realmRoles as string[];
      return session;
    },
  },
});
