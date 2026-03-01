import { redirect } from "next/navigation";
import { auth } from "@/auth";
import { Button } from "@/components/ui/Button";

interface InviteInfo {
  token: string;
  orgId: string;
  orgName: string;
  role: string;
  expiresAt: string;
}

interface PageProps {
  params: Promise<{ locale: string; token: string }>;
}

async function getInviteInfo(token: string): Promise<InviteInfo | null> {
  const backendUrl = process.env.BACKEND_URL ?? "http://localhost:19000";
  try {
    const res = await fetch(
      `${backendUrl}/api/admin/invitations/${token}`,
      { cache: "no-store" }
    );
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

export default async function InvitePage({ params }: PageProps) {
  const { locale, token } = await params;
  const info = await getInviteInfo(token);

  if (!info) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen">
        <div className="rounded-lg border border-border bg-card p-8 text-center max-w-md w-full">
          <h1 className="text-lg font-semibold text-foreground mb-2">
            Invalid or Expired Invitation
          </h1>
          <p className="text-sm text-muted-foreground">
            This invitation link is no longer valid.
          </p>
        </div>
      </div>
    );
  }

  const orgId = info.orgId;

  async function accept() {
    "use server";
    const session = await auth();
    const backendUrl = process.env.BACKEND_URL ?? "http://localhost:19000";
    const headers: Record<string, string> = {};
    if (session?.accessToken) {
      headers["Authorization"] = `Bearer ${session.accessToken}`;
    }
    const res = await fetch(
      `${backendUrl}/api/admin/invitations/${token}/accept`,
      { method: "POST", headers }
    );
    if (res.ok) {
      redirect(`/${locale}/orgs/${orgId}`);
    }
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <div className="rounded-lg border border-border bg-card p-8 max-w-md w-full space-y-4">
        <h1 className="text-lg font-semibold text-foreground">
          Join Organization
        </h1>
        <div className="space-y-2 text-sm">
          <div>
            <span className="text-muted-foreground">Organization: </span>
            <span className="font-medium text-foreground">{info.orgName}</span>
          </div>
          <div>
            <span className="text-muted-foreground">Role: </span>
            <span className="font-medium text-foreground">
              {info.role.charAt(0) + info.role.slice(1).toLowerCase()}
            </span>
          </div>
          <div>
            <span className="text-muted-foreground">Expires: </span>
            <span className="text-foreground">
              {new Date(info.expiresAt).toLocaleDateString()}
            </span>
          </div>
        </div>
        <form action={accept}>
          <Button type="submit" className="w-full">
            Accept &amp; Join
          </Button>
        </form>
      </div>
    </div>
  );
}
