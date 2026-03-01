"use client";

import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, MessageSquare, Play, HardDrive, Gauge } from "lucide-react";
import { Link } from "@/navigation";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

export default function OrgUsagePage() {
  const { id } = useParams<{ id: string }>();

  const { data: usage, isLoading } = useQuery({
    queryKey: ["orgs", id, "usage"],
    queryFn: () => api.usage.get(id),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!usage) return null;

  const msgUsagePct =
    usage.monthlyMessageQuota != null && usage.monthlyMessageQuota > 0
      ? Math.min(100, Math.round((usage.totalMessages / usage.monthlyMessageQuota) * 100))
      : null;
  const execUsagePct =
    usage.monthlyExecQuota != null && usage.monthlyExecQuota > 0
      ? Math.min(100, Math.round((usage.totalExecutions / usage.monthlyExecQuota) * 100))
      : null;

  // Build sorted 7-day date list
  const days: string[] = [];
  for (let i = usage.days - 1; i >= 0; i--) {
    const d = new Date();
    d.setUTCDate(d.getUTCDate() - i);
    days.push(d.toISOString().slice(0, 10));
  }

  const statCards = [
    {
      label: `Messages (last ${usage.days}d)`,
      value: usage.totalMessages.toLocaleString(),
      icon: MessageSquare,
      quota: usage.monthlyMessageQuota != null ? `/ ${usage.monthlyMessageQuota.toLocaleString()} limit` : "Unlimited",
    },
    {
      label: `Executions (last ${usage.days}d)`,
      value: usage.totalExecutions.toLocaleString(),
      icon: Play,
      quota: usage.monthlyExecQuota != null ? `/ ${usage.monthlyExecQuota.toLocaleString()} limit` : "Unlimited",
    },
    {
      label: "Active Workspaces",
      value: usage.activeWorkspaces.toLocaleString(),
      icon: HardDrive,
      quota: null,
    },
    {
      label: "Quota Usage",
      value: msgUsagePct != null ? `${msgUsagePct}%` : execUsagePct != null ? `${execUsagePct}%` : "—",
      icon: Gauge,
      quota: "messages quota",
    },
  ];

  return (
    <div>
      <div className="mb-6 flex items-center gap-3">
        <Link href={`/orgs/${id}`}>
          <Button variant="ghost" size="sm">
            <ArrowLeft size={14} />
          </Button>
        </Link>
        <h1 className="text-xl font-bold text-foreground">Usage Statistics</h1>
        <span className="text-sm text-muted-foreground">Last {usage.days} days</span>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 gap-4 mb-8 lg:grid-cols-4">
        {statCards.map(({ label, value, icon: Icon, quota }) => (
          <div
            key={label}
            className="rounded-lg border border-border bg-card p-4"
          >
            <div className="flex items-center gap-2 mb-2">
              <Icon size={16} className="text-muted-foreground" />
              <span className="text-xs text-muted-foreground">{label}</span>
            </div>
            <div className="text-2xl font-bold text-foreground">{value}</div>
            {quota && (
              <div className="text-xs text-muted-foreground mt-1">{quota}</div>
            )}
          </div>
        ))}
      </div>

      {/* 7-day trend table */}
      <Card title="Daily Trend">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-border">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-muted-foreground">Date</th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase text-muted-foreground">Messages</th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase text-muted-foreground">Executions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/50">
              {days.map((day) => (
                <tr key={day} className="hover:bg-accent/30 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs text-foreground">{day}</td>
                  <td className="px-4 py-3 text-right text-foreground">
                    {(usage.messagesByDay[day] ?? 0).toLocaleString()}
                  </td>
                  <td className="px-4 py-3 text-right text-foreground">
                    {(usage.executionsByDay[day] ?? 0).toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
