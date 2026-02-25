"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "next/navigation";
import { KnowledgeSearch } from "@/components/knowledge/KnowledgeSearch";
import { DocViewer } from "@/components/knowledge/DocViewer";
import { ArchDiagramViewer } from "@/components/knowledge/ArchDiagramViewer";
import { ServiceGraphViewer } from "@/components/knowledge/ServiceGraphViewer";
import { ApiExplorer } from "@/components/knowledge/ApiExplorer";
import { KnowledgeTagList } from "@/components/knowledge/KnowledgeTagList";
import { KnowledgeTagDetail } from "@/components/knowledge/KnowledgeTagDetail";
import { KnowledgeTagView, knowledgeTagApi } from "@/lib/knowledge-tag-api";
import { BookOpen, BookMarked, GitBranch, Network, Globe } from "lucide-react";

type KnowledgeTab = "docs" | "standards" | "architecture" | "services" | "apis";

const tabs: { id: KnowledgeTab; label: string; icon: React.ElementType }[] = [
  { id: "docs", label: "Documentation", icon: BookOpen },
  { id: "standards", label: "Standards", icon: BookMarked },
  { id: "architecture", label: "Architecture", icon: GitBranch },
  { id: "services", label: "Service Graph", icon: Network },
  { id: "apis", label: "API Explorer", icon: Globe },
];

export default function KnowledgePage() {
  const [activeTab, setActiveTab] = useState<KnowledgeTab>("docs");
  const [selectedDocId, setSelectedDocId] = useState<string | null>(null);
  const searchParams = useSearchParams();
  const workspaceId = searchParams.get("workspaceId") ?? undefined;

  // Standards tab state
  const [tags, setTags] = useState<KnowledgeTagView[]>([]);
  const [tagsLoading, setTagsLoading] = useState(false);
  const [selectedTagId, setSelectedTagId] = useState<string | null>(null);
  const [tagSearchQuery, setTagSearchQuery] = useState("");

  const loadTags = useCallback(async () => {
    setTagsLoading(true);
    try {
      const data = await knowledgeTagApi.listTags();
      setTags(data);
    } catch (err) {
      console.error("Failed to load tags:", err);
    } finally {
      setTagsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === "standards") {
      loadTags();
    }
  }, [activeTab, loadTags]);

  const selectedTag = tags.find((t) => t.id === selectedTagId) ?? null;

  const handleTagUpdated = useCallback((updated: KnowledgeTagView) => {
    setTags((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
  }, []);

  return (
    <div className="flex h-full flex-col">
      {/* Tab Bar */}
      <div className="flex items-center gap-1 border-b border-border bg-card px-4">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => {
              setActiveTab(tab.id);
              setSelectedDocId(null);
            }}
            className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-medium transition-colors ${
              activeTab === tab.id
                ? "border-primary text-foreground"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            <tab.icon className="h-4 w-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex flex-1 overflow-hidden">
        {activeTab === "docs" && (
          <>
            <div className="w-96 flex-shrink-0 border-r border-border overflow-auto">
              <KnowledgeSearch
                onSelectDocument={(docId) => setSelectedDocId(docId)}
                selectedDocId={selectedDocId}
                workspaceId={workspaceId}
              />
            </div>
            <div className="flex-1 overflow-auto">
              {selectedDocId ? (
                <DocViewer documentId={selectedDocId} />
              ) : (
                <div className="flex h-full items-center justify-center text-muted-foreground">
                  <div className="text-center">
                    <BookOpen className="mx-auto h-12 w-12 opacity-50" />
                    <p className="mt-4 text-lg font-medium">
                      Select a document
                    </p>
                    <p className="mt-1 text-sm">
                      Search and browse knowledge base documents
                    </p>
                  </div>
                </div>
              )}
            </div>
          </>
        )}

        {activeTab === "standards" && (
          <>
            <div className="w-80 flex-shrink-0 border-r border-border overflow-auto">
              <KnowledgeTagList
                tags={tags}
                selectedTagId={selectedTagId}
                onSelect={setSelectedTagId}
                loading={tagsLoading}
                searchQuery={tagSearchQuery}
                onSearchChange={setTagSearchQuery}
              />
            </div>
            <div className="flex-1 overflow-auto">
              {selectedTag ? (
                <KnowledgeTagDetail
                  tag={selectedTag}
                  onUpdated={handleTagUpdated}
                />
              ) : (
                <div className="flex h-full items-center justify-center text-muted-foreground">
                  <div className="text-center">
                    <BookMarked className="mx-auto h-12 w-12 opacity-50" />
                    <p className="mt-4 text-lg font-medium">
                      Select a standard
                    </p>
                    <p className="mt-1 text-sm">
                      Browse and edit design baseline standards
                    </p>
                  </div>
                </div>
              )}
            </div>
          </>
        )}

        {activeTab === "architecture" && (
          <div className="flex-1 overflow-auto p-4">
            <ArchDiagramViewer />
          </div>
        )}

        {activeTab === "services" && (
          <div className="flex-1 overflow-auto">
            <ServiceGraphViewer />
          </div>
        )}

        {activeTab === "apis" && (
          <div className="flex-1 overflow-auto">
            <ApiExplorer />
          </div>
        )}
      </div>
    </div>
  );
}
