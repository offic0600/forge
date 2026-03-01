-- 按 session_id 查询消息（Usage统计热路径）
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
-- ChatSessions 按 workspace_id 过滤
CREATE INDEX IF NOT EXISTS idx_chat_sessions_workspace_id ON chat_sessions(workspace_id);
-- ExecutionRecords 按 session_id 关联
CREATE INDEX IF NOT EXISTS idx_execution_records_session_id ON execution_records(session_id);
-- Workspaces 按 org_id 过滤（Usage统计 JOIN 热路径）
CREATE INDEX IF NOT EXISTS idx_workspaces_org_id ON workspaces(org_id);
-- OrgMembers 按 user_id 查找（RbacHelper.isOrgAdmin 热路径）
CREATE INDEX IF NOT EXISTS idx_org_members_user_id ON org_members(user_id);
-- AuditLogs 按 actor_id 查询
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_id ON audit_logs(actor_id);
