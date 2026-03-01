"use client";
import { useSession } from "next-auth/react";
import type { OrgMember } from "./types";

export function useCurrentUser() {
  return useSession().data?.user;
}

export function useIsSystemAdmin() {
  const user = useCurrentUser() as
    | { realmRoles?: string[] }
    | undefined;
  return user?.realmRoles?.includes("admin") ?? false;
}

export function useOrgRole(members: OrgMember[]) {
  const user = useCurrentUser();
  if (!user?.email) return null;
  return members.find((m) => m.userId === user.email)?.role ?? null;
}
