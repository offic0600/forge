"use client";

import React, { useState, useEffect, useCallback } from "react";
import {
  BarChart3,
  Clock,
  CheckCircle,
  XCircle,
  Timer,
  RefreshCw,
} from "lucide-react";

interface ProfileStat {
  name: string;
  count: number;
  avgDurationMs: number;
  avgTurns: number;
}

interface ToolCallStat {
  name: string;
  count: number;
}

interface HitlStats {
  total: number;
  approved: number;
  rejected: number;
  timeout: number;
  modified: number;
  pending: number;
}

interface DashboardMetrics {
  profileStats: ProfileStat[];
  toolCallStats: ToolCallStat[];
  hitlStats: HitlStats;
  totalSessions: number;
  avgDurationMs: number;
}

interface ExecutionRecord {
  id: string;
  sessionId: string;
  profile: string;
  skillsLoaded: number;
  totalDurationMs: number;
  totalTurns: number;
  hitlResult: string | null;
  baselineResults: string | null;
  createdAt: string;
}

interface TrendPoint {
  date: string;
  sessions: number;
  avgDurationMs: number;
}

export function QualityPanel() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [executions, setExecutions] = useState<ExecutionRecord[]>([]);
  const [trends, setTrends] = useState<TrendPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [metricsRes, execRes, trendsRes] = await Promise.all([
        fetch("/api/dashboard/metrics"),
        fetch("/api/dashboard/executions?limit=20"),
        fetch("/api/dashboard/trends?days=7"),
      ]);

      if (metricsRes.ok) setMetrics(await metricsRes.json());
      if (execRes.ok) setExecutions(await execRes.json());
      if (trendsRes.ok) setTrends(await trendsRes.json());
    } catch (err) {
      setError("Failed to load dashboard data");
      console.error("Dashboard fetch error:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full text-muted-foreground">
        <RefreshCw className="h-4 w-4 animate-spin mr-2" />
        <span className="text-sm">加载中...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-muted-foreground">
        <p className="text-sm">{error}</p>
        <button onClick={fetchData} className="mt-2 text-xs text-primary hover:underline">
          重试
        </button>
      </div>
    );
  }

  const maxToolCount = Math.max(...(metrics?.toolCallStats?.map((t) => t.count) ?? [1]));
  const maxTrendSessions = Math.max(...trends.map((t) => t.sessions), 1);

  return (
    <div className="space-y-4 p-4 overflow-auto h-full">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">质量面板</h3>
        <button onClick={fetchData} className="rounded p-1 text-muted-foreground hover:bg-accent" title="刷新">
          <RefreshCw className="h-3.5 w-3.5" />
        </button>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-3 gap-2">
        <StatCard
          icon={<BarChart3 className="h-4 w-4" />}
          label="总会话"
          value={metrics?.totalSessions ?? 0}
        />
        <StatCard
          icon={<Timer className="h-4 w-4" />}
          label="平均耗时"
          value={`${((metrics?.avgDurationMs ?? 0) / 1000).toFixed(1)}s`}
        />
        <StatCard
          icon={<CheckCircle className="h-4 w-4" />}
          label="HITL 审批"
          value={metrics?.hitlStats?.total ?? 0}
        />
      </div>

      {/* HITL Breakdown */}
      {metrics?.hitlStats && metrics.hitlStats.total > 0 && (
        <div className="space-y-1">
          <span className="text-xs font-medium text-muted-foreground">HITL 审批分布</span>
          <div className="flex gap-1">
            <HitlBadge label="批准" count={metrics.hitlStats.approved} color="bg-green-500/20 text-green-400" />
            <HitlBadge label="拒绝" count={metrics.hitlStats.rejected} color="bg-red-500/20 text-red-400" />
            <HitlBadge label="修改" count={metrics.hitlStats.modified} color="bg-blue-500/20 text-blue-400" />
            <HitlBadge label="超时" count={metrics.hitlStats.timeout} color="bg-yellow-500/20 text-yellow-400" />
          </div>
        </div>
      )}

      {/* Profile Usage */}
      {metrics?.profileStats && metrics.profileStats.length > 0 && (
        <div className="space-y-1">
          <span className="text-xs font-medium text-muted-foreground">Profile 使用</span>
          <div className="space-y-1">
            {metrics.profileStats.map((p) => (
              <div key={p.name} className="flex items-center justify-between text-xs">
                <span className="font-mono text-foreground">{p.name.replace("-profile", "")}</span>
                <span className="text-muted-foreground">
                  {p.count}次 / {(p.avgDurationMs / 1000).toFixed(1)}s
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tool Call Ranking */}
      {metrics?.toolCallStats && metrics.toolCallStats.length > 0 && (
        <div className="space-y-1">
          <span className="text-xs font-medium text-muted-foreground">工具调用 Top 10</span>
          <div className="space-y-1">
            {metrics.toolCallStats.map((t) => (
              <div key={t.name} className="space-y-0.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-mono truncate max-w-[150px]">{t.name}</span>
                  <span className="text-muted-foreground">{t.count}</span>
                </div>
                <div className="h-1 rounded-full bg-muted overflow-hidden">
                  <div
                    className="h-full rounded-full bg-primary"
                    style={{ width: `${(t.count / maxToolCount) * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 7-Day Trend */}
      {trends.length > 0 && (
        <div className="space-y-1">
          <span className="text-xs font-medium text-muted-foreground">7 日趋势</span>
          <div className="flex items-end gap-1 h-16">
            {trends.map((t) => (
              <div key={t.date} className="flex-1 flex flex-col items-center gap-0.5">
                <div
                  className="w-full rounded-sm bg-primary/60 min-h-[2px]"
                  style={{ height: `${Math.max((t.sessions / maxTrendSessions) * 100, 5)}%` }}
                  title={`${t.date}: ${t.sessions} 会话, ${(t.avgDurationMs / 1000).toFixed(1)}s`}
                />
                <span className="text-[8px] text-muted-foreground/60">
                  {t.date.slice(5)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Execution Records Table */}
      {executions.length > 0 && (
        <div className="space-y-1">
          <span className="text-xs font-medium text-muted-foreground">最近执行记录</span>
          <div className="border border-border rounded-md overflow-hidden">
            <table className="w-full text-xs">
              <thead className="bg-muted/50">
                <tr>
                  <th className="px-2 py-1 text-left font-medium">时间</th>
                  <th className="px-2 py-1 text-left font-medium">Profile</th>
                  <th className="px-2 py-1 text-right font-medium">耗时</th>
                </tr>
              </thead>
              <tbody>
                {executions.slice(0, 10).map((r) => (
                  <tr key={r.id} className="border-t border-border">
                    <td className="px-2 py-1 text-muted-foreground">
                      {new Date(r.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                    </td>
                    <td className="px-2 py-1 font-mono">
                      {r.profile.replace("-profile", "")}
                    </td>
                    <td className="px-2 py-1 text-right text-muted-foreground">
                      {(r.totalDurationMs / 1000).toFixed(1)}s
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Empty state */}
      {!metrics?.totalSessions && executions.length === 0 && (
        <div className="text-center text-muted-foreground text-sm py-8">
          暂无执行数据。开始一次对话后数据将出现在这里。
        </div>
      )}
    </div>
  );
}

function StatCard({ icon, label, value }: { icon: React.ReactNode; label: string; value: number | string }) {
  return (
    <div className="rounded-md border border-border bg-card p-2 space-y-1">
      <div className="flex items-center gap-1 text-muted-foreground">
        {icon}
        <span className="text-[10px]">{label}</span>
      </div>
      <div className="text-lg font-semibold">{value}</div>
    </div>
  );
}

function HitlBadge({ label, count, color }: { label: string; count: number; color: string }) {
  if (count === 0) return null;
  return (
    <span className={`rounded px-1.5 py-0.5 text-xs ${color}`}>
      {label}: {count}
    </span>
  );
}
