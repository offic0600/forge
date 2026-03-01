"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Save } from "lucide-react";
import { Link } from "@/navigation";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card } from "@/components/ui/Card";
import { useIsSystemAdmin } from "@/lib/session";

export default function OrgQuotaPage() {
  const { id } = useParams<{ id: string }>();
  const qc = useQueryClient();
  const isAdmin = useIsSystemAdmin();

  const { data: org, isLoading } = useQuery({
    queryKey: ["orgs", id],
    queryFn: () => api.orgs.get(id),
  });

  const [msgQuota, setMsgQuota] = useState<string>("");
  const [execQuota, setExecQuota] = useState<string>("");
  const [initialized, setInitialized] = useState(false);

  // Initialize form values once org is loaded
  if (org && !initialized) {
    setMsgQuota(org.monthlyMessageQuota?.toString() ?? "");
    setExecQuota(org.monthlyExecQuota?.toString() ?? "");
    setInitialized(true);
  }

  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const mutation = useMutation({
    mutationFn: () =>
      api.quota.update(id, {
        monthlyMessageQuota: msgQuota.trim() !== "" ? parseInt(msgQuota, 10) : null,
        monthlyExecQuota: execQuota.trim() !== "" ? parseInt(execQuota, 10) : null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["orgs", id] });
      qc.invalidateQueries({ queryKey: ["orgs", id, "usage"] });
      setSaveSuccess(true);
      setSaveError(null);
      setTimeout(() => setSaveSuccess(false), 3000);
    },
    onError: (err: Error) => {
      setSaveError(err.message);
      setSaveSuccess(false);
    },
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="text-center py-20">
        <p className="text-muted-foreground">Only System Admins can manage quotas.</p>
        <Link href={`/orgs/${id}`} className="mt-4 inline-block">
          <Button variant="secondary">Back</Button>
        </Link>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-6 flex items-center gap-3">
        <Link href={`/orgs/${id}`}>
          <Button variant="ghost" size="sm">
            <ArrowLeft size={14} />
          </Button>
        </Link>
        <h1 className="text-xl font-bold text-foreground">Quota Management</h1>
        {org && (
          <span className="text-sm text-muted-foreground">{org.name}</span>
        )}
      </div>

      <div className="max-w-md">
        <Card title="Monthly Limits">
          <div className="space-y-4">
            <div>
              <Input
                label="Monthly Message Quota"
                type="number"
                min="0"
                placeholder="Leave empty for unlimited"
                value={msgQuota}
                onChange={(e) => setMsgQuota(e.target.value)}
              />
              <p className="text-xs text-muted-foreground mt-1">
                Maximum chat messages per month. Empty = no limit.
              </p>
            </div>
            <div>
              <Input
                label="Monthly Execution Quota"
                type="number"
                min="0"
                placeholder="Leave empty for unlimited"
                value={execQuota}
                onChange={(e) => setExecQuota(e.target.value)}
              />
              <p className="text-xs text-muted-foreground mt-1">
                Maximum agent executions per month. Empty = no limit.
              </p>
            </div>

            {saveError && (
              <div className="rounded-md border border-destructive/40 bg-destructive/20 px-3 py-2 text-sm text-destructive-foreground">
                {saveError}
              </div>
            )}
            {saveSuccess && (
              <div className="rounded-md border border-green-500/40 bg-green-500/20 px-3 py-2 text-sm text-green-700 dark:text-green-300">
                Quota updated successfully.
              </div>
            )}

            <div className="flex gap-2 pt-2">
              <Button
                onClick={() => mutation.mutate()}
                loading={mutation.isPending}
              >
                <Save size={14} />
                Save Quota
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  setMsgQuota(org?.monthlyMessageQuota?.toString() ?? "");
                  setExecQuota(org?.monthlyExecQuota?.toString() ?? "");
                }}
              >
                Reset
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
