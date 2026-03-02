"use client";

import React, { useState, useEffect } from "react";
import { getAuthHeaders } from "@/lib/auth";

interface StageMemory {
  workspaceId: string;
  profile: string;
  completedWork: string;
  keyDecisions: string;
  unresolvedIssues: string;
  nextSteps: string;
  sessionCount: number;
  updatedAt: string;
}

interface StageMemoryViewProps {
  workspaceId: string;
}

function parseJsonArray(json: string): string[] {
  try {
    return JSON.parse(json) as string[];
  } catch {
    return [];
  }
}

export function StageMemoryView({ workspaceId }: StageMemoryViewProps) {
  const [stages, setStages] = useState<StageMemory[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedProfile, setExpandedProfile] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const res = await fetch(`/api/memory/stage/${workspaceId}`, { headers: getAuthHeaders() });
        if (res.ok) {
          const data = (await res.json()) as StageMemory[];
          setStages(data);
        }
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    })();
  }, [workspaceId]);

  if (loading) {
    return <p className="text-xs text-muted-foreground">加载中...</p>;
  }

  if (stages.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-8 text-center">
        <p className="text-sm text-muted-foreground">暂无阶段记忆</p>
        <p className="mt-1 text-xs text-muted-foreground">
          阶段记忆会在会话结束后自动聚合
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {stages.map((stage) => {
        const completed = parseJsonArray(stage.completedWork);
        const decisions = parseJsonArray(stage.keyDecisions);
        const unresolved = parseJsonArray(stage.unresolvedIssues);
        const nextSteps = parseJsonArray(stage.nextSteps);
        const isExpanded = expandedProfile === stage.profile;

        return (
          <div
            key={stage.profile}
            className="rounded-md border border-border bg-muted/20"
          >
            <button
              onClick={() =>
                setExpandedProfile(isExpanded ? null : stage.profile)
              }
              className="flex w-full items-center justify-between px-3 py-2 text-left"
            >
              <div className="flex items-center gap-2">
                <span className="text-xs font-medium text-foreground">
                  {stage.profile.replace("-profile", "")}
                </span>
                <span className="text-xs text-muted-foreground">
                  {stage.sessionCount} sessions
                </span>
              </div>
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <span title="已完成">{completed.length} done</span>
                <span title="决策">{decisions.length} decisions</span>
                {unresolved.length > 0 && (
                  <span className="text-yellow-500" title="未解决">
                    {unresolved.length} open
                  </span>
                )}
              </div>
            </button>

            {isExpanded && (
              <div className="space-y-2 border-t border-border px-3 py-2">
                {completed.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-foreground">
                      已完成工作
                    </p>
                    <ul className="mt-1 space-y-0.5">
                      {completed.map((item, i) => (
                        <li
                          key={i}
                          className="text-xs text-muted-foreground"
                        >
                          - {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {decisions.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-foreground">
                      关键决策
                    </p>
                    <ul className="mt-1 space-y-0.5">
                      {decisions.map((item, i) => (
                        <li
                          key={i}
                          className="text-xs text-muted-foreground"
                        >
                          - {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {unresolved.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-yellow-500">
                      未解决问题
                    </p>
                    <ul className="mt-1 space-y-0.5">
                      {unresolved.map((item, i) => (
                        <li
                          key={i}
                          className="text-xs text-yellow-400/80"
                        >
                          - {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {nextSteps.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-foreground">
                      下一步
                    </p>
                    <ul className="mt-1 space-y-0.5">
                      {nextSteps.map((item, i) => (
                        <li
                          key={i}
                          className="text-xs text-muted-foreground"
                        >
                          - {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                <p className="text-xs text-muted-foreground/60">
                  更新于 {new Date(stage.updatedAt).toLocaleString()}
                </p>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
