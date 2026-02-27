"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Plus, Trash2, Lock, Save } from "lucide-react";
import Link from "next/link";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import type { OrgEnvConfig } from "@/lib/types";

const PREDEFINED = [
  { category: "build", key: "JDK_VERSION", label: "JDK Version", options: ["8", "11", "17", "21"], type: "select" as const },
  { category: "build", key: "NODE_VERSION", label: "Node.js Version", options: ["16", "18", "20", "22"], type: "select" as const },
  { category: "build", key: "MAVEN_OPTS", label: "Maven Options", options: [], type: "text" as const },
  { category: "build", key: "GRADLE_OPTS", label: "Gradle Options", options: [], type: "text" as const },
];

interface PredefinedFieldProps {
  orgId: string;
  field: (typeof PREDEFINED)[0];
  existing: OrgEnvConfig | undefined;
}

function PredefinedField({ orgId, field, existing }: PredefinedFieldProps) {
  const qc = useQueryClient();
  const [value, setValue] = useState(existing?.configValue ?? "");
  const [saved, setSaved] = useState(false);

  const mutation = useMutation({
    mutationFn: () =>
      api.envConfigs.upsert(orgId, field.category, field.key, {
        configKey: field.key,
        configValue: value,
        isSensitive: false,
        description: field.label,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["orgs", orgId, "env-configs"] });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    },
  });

  return (
    <div className="flex items-end gap-2">
      {field.type === "select" ? (
        <div className="flex-1">
          <label className="text-xs font-medium text-muted-foreground block mb-1">{field.label}</label>
          <select
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground"
            value={value}
            onChange={(e) => setValue(e.target.value)}
          >
            <option value="">Select...</option>
            {field.options.map((opt) => (
              <option key={opt} value={opt}>{opt}</option>
            ))}
          </select>
        </div>
      ) : (
        <div className="flex-1">
          <Input label={field.label} value={value} onChange={(e) => setValue(e.target.value)} placeholder="e.g. -Xmx2g" />
        </div>
      )}
      <Button size="sm" onClick={() => mutation.mutate()} loading={mutation.isPending} variant={saved ? "secondary" : "primary"}>
        {saved ? "Saved!" : <Save size={13} />}
      </Button>
    </div>
  );
}

export default function BuildEnvPage() {
  const { id } = useParams<{ id: string }>();
  const qc = useQueryClient();

  const { data: allConfigs = [], isLoading } = useQuery({
    queryKey: ["orgs", id, "env-configs"],
    queryFn: () => api.envConfigs.list(id),
  });

  const customConfigs = allConfigs.filter(
    (c) => !PREDEFINED.some((p) => p.category === c.category && p.key === c.configKey)
  );

  const [newKey, setNewKey] = useState("");
  const [newValue, setNewValue] = useState("");
  const [newCategory, setNewCategory] = useState("custom");
  const [isSensitive, setIsSensitive] = useState(false);

  const addMutation = useMutation({
    mutationFn: () =>
      api.envConfigs.upsert(id, newCategory, newKey, {
        configKey: newKey,
        configValue: newValue,
        isSensitive,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["orgs", id, "env-configs"] });
      setNewKey(""); setNewValue("");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ({ category, key }: { category: string; key: string }) =>
      api.envConfigs.delete(id, category, key),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["orgs", id, "env-configs"] }),
  });

  return (
    <div>
      <div className="mb-6 flex items-center gap-3">
        <Link href={`/orgs/${id}`}>
          <Button variant="ghost" size="sm">
            <ArrowLeft size={14} />
          </Button>
        </Link>
        <div>
          <h1 className="text-xl font-bold text-foreground">Build Environment</h1>
          <p className="text-sm text-muted-foreground">Configure build and runtime environment variables</p>
        </div>
      </div>

      <div className="space-y-6 max-w-2xl">
        <Card title="Predefined Settings">
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <div className="h-5 w-5 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            </div>
          ) : (
            <div className="space-y-4">
              {PREDEFINED.map((field) => (
                <PredefinedField
                  key={field.key}
                  orgId={id}
                  field={field}
                  existing={allConfigs.find((c) => c.category === field.category && c.configKey === field.key)}
                />
              ))}
            </div>
          )}
        </Card>

        <Card title="Custom Variables">
          <div className="space-y-3">
            <div className="flex gap-2">
              <Input placeholder="Category" value={newCategory} onChange={(e) => setNewCategory(e.target.value)} className="w-24" />
              <Input placeholder="KEY" value={newKey} onChange={(e) => setNewKey(e.target.value)} className="flex-1" />
              <Input
                placeholder="Value"
                type={isSensitive ? "password" : "text"}
                value={newValue}
                onChange={(e) => setNewValue(e.target.value)}
                className="flex-1"
              />
              <label className="flex items-center gap-1 text-xs text-muted-foreground whitespace-nowrap cursor-pointer">
                <input
                  type="checkbox"
                  checked={isSensitive}
                  onChange={(e) => setIsSensitive(e.target.checked)}
                  className="rounded"
                />
                Secret
              </label>
              <Button size="sm" disabled={!newKey.trim()} loading={addMutation.isPending} onClick={() => addMutation.mutate()}>
                <Plus size={13} />
              </Button>
            </div>

            {customConfigs.length > 0 && (
              <div className="overflow-x-auto rounded-lg border border-border">
                <table className="w-full text-xs">
                  <thead className="border-b border-border bg-muted/50">
                    <tr>
                      <th className="px-3 py-2 text-left text-muted-foreground">Category</th>
                      <th className="px-3 py-2 text-left text-muted-foreground">Key</th>
                      <th className="px-3 py-2 text-left text-muted-foreground">Value</th>
                      <th className="px-3 py-2 text-left text-muted-foreground">Type</th>
                      <th className="px-3 py-2" />
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    {customConfigs.map((cfg) => (
                      <tr key={cfg.id} className="hover:bg-accent/30 transition-colors">
                        <td className="px-3 py-2 font-mono text-muted-foreground">{cfg.category}</td>
                        <td className="px-3 py-2 font-mono text-foreground">{cfg.configKey}</td>
                        <td className="px-3 py-2 text-foreground max-w-xs truncate font-mono">
                          {cfg.configValue ?? <span className="text-muted-foreground">—</span>}
                        </td>
                        <td className="px-3 py-2">
                          {cfg.isSensitive ? (
                            <Badge color="yellow">
                              <Lock size={10} className="inline mr-1" />
                              secret
                            </Badge>
                          ) : (
                            <Badge color="gray">plain</Badge>
                          )}
                        </td>
                        <td className="px-3 py-2 text-right">
                          <Button
                            variant="ghost" size="sm"
                            className="text-destructive hover:text-destructive hover:bg-destructive/10"
                            onClick={() => deleteMutation.mutate({ category: cfg.category, key: cfg.configKey })}
                          >
                            <Trash2 size={12} />
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {customConfigs.length === 0 && (
              <p className="text-center py-4 text-xs text-muted-foreground">No custom variables yet</p>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}
