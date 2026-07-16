import { useState } from "react";
import { Shield, Users, FileSearch, ChevronRight, ChevronLeft } from "lucide-react";
import { useNavigate } from "react-router";
import { useAdminUsers, useAdminFlaggedAudits } from "@/hooks/useAdmin";
import { useDocumentTitle } from "@/hooks/useDocumentTitle";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";

type Tab = "users" | "audits";
const PAGE_SIZE = 20;

function statusVariant(s: string): "status-complete" | "status-failed" | "status-processing" | "status-pending" {
  if (s === "COMPLETE") return "status-complete";
  if (s === "FAILED") return "status-failed";
  if (s === "PROCESSING") return "status-processing";
  return "status-pending";
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export default function AdminPage() {
  useDocumentTitle("Admin Page");
  const [tab, setTab] = useState<Tab>("users");
  const [usersPage, setUsersPage] = useState(0);
  const [auditsPage, setAuditsPage] = useState(0);
  const navigate = useNavigate();

  const { data: usersData, isLoading: usersLoading } = useAdminUsers({ page: usersPage, size: PAGE_SIZE });
  const { data: auditsData, isLoading: auditsLoading } = useAdminFlaggedAudits({ page: auditsPage, size: PAGE_SIZE });

  const users = usersData?.content ?? [];
  const audits = auditsData?.content ?? [];


  const adminsOnPage = users.filter((u) => u.role === "ADMIN").length;
  const failedOnPage = audits.filter((a) => a.status === "FAILED").length;

  return (
    <div className="bg-background min-h-screen" style={{ fontFamily: "var(--font-body)" }}>
      <div className="border-b-2 border-black px-8 py-5 bg-card">
        <div className="flex items-center gap-3 mb-1">
          <div className="border-2 border-black p-2 bg-secondary">
            <Shield size={14} className="text-secondary-foreground" />
          </div>
          <h1 style={{ fontFamily: "var(--font-display)", fontSize: 22, letterSpacing: "-0.02em" }}>Admin Panel</h1>
          <Badge variant="role-admin">ADMIN ONLY</Badge>
        </div>
        <p style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)" }}>
          User management and flagged audit review.
        </p>
      </div>

      <div className="px-8 py-6 space-y-5">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: "TOTAL USERS", value: usersData?.totalElements ?? "—" },
            { label: "ADMINS (PAGE)", value: adminsOnPage },
            { label: "FLAGGED AUDITS", value: auditsData?.totalElements ?? "—" },
            { label: "FAILED (PAGE)", value: failedOnPage },
          ].map(({ label, value }) => (
            <div key={label} className="border-2 border-black px-5 py-4 bg-card">
              <div style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)", letterSpacing: "0.1em" }}>
                {label}
              </div>
              <div style={{ fontFamily: "var(--font-display)", fontSize: 28, letterSpacing: "-0.03em", marginTop: 4 }}>
                {value}
              </div>
            </div>
          ))}
        </div>

        <Tabs value={tab} onValueChange={(v) => setTab(v as Tab)}>
          <TabsList>
            <TabsTrigger value="users" className="flex items-center gap-2">
              <Users size={13} /> Users
            </TabsTrigger>
            <TabsTrigger value="audits" className="flex items-center gap-2">
              <FileSearch size={13} /> Flagged Audits
            </TabsTrigger>
          </TabsList>

          <TabsContent value="users" className="mt-4">
            <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">
              <div className="hidden md:grid border-b-2 border-black px-5 py-2.5 bg-muted"
                style={{ gridTemplateColumns: "2fr 2fr 80px 90px" }}>
                {["USER", "EMAIL", "ROLE", "AUDITS TODAY"].map((h) => (
                  <span key={h} style={{ fontFamily: "var(--font-display)", fontSize: 10, letterSpacing: "0.08em", color: "var(--muted-foreground)" }}>
                    {h}
                  </span>
                ))}
              </div>
              <div className="divide-y-2 divide-black/10">
                {usersLoading && (
                  <div className="px-5 py-6 text-center" style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                    Loading users…
                  </div>
                )}
                {!usersLoading && users.length === 0 && (
                  <div className="px-5 py-6 text-center" style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                    No users found.
                  </div>
                )}
                {users.map((user) => (
                  <div key={user.id} className="px-5 py-4">
                    <div className="md:hidden space-y-1">
                      <div className="flex items-center gap-2">
                        <Avatar size="sm">
                          <AvatarFallback color={user.role === "ADMIN" ? "secondary" : "primary"} size="sm">
                            {(user.displayName ?? user.email).slice(0, 1).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                        <span style={{ fontFamily: "var(--font-display)", fontSize: 13 }}>
                          {user.displayName ?? user.email}
                        </span>
                        <Badge variant={user.role === "ADMIN" ? "role-admin" : "role-user"} className="ml-auto">
                          {user.role}
                        </Badge>
                      </div>
                      <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
                        {user.email} · {user.auditCountToday} audits today
                      </div>
                    </div>
                    <div className="hidden md:grid items-center gap-2" style={{ gridTemplateColumns: "2fr 2fr 80px 90px" }}>
                      <div className="flex items-center gap-3">
                        <Avatar size="default">
                          <AvatarFallback color={user.role === "ADMIN" ? "secondary" : "primary"}>
                            {(user.displayName ?? user.email).slice(0, 2).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <div style={{ fontFamily: "var(--font-display)", fontSize: 13 }}>
                            {user.displayName ?? "—"}
                          </div>
                          <div style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)" }}>
                            ID: {user.id.slice(0, 8)}
                          </div>
                        </div>
                      </div>
                      <span style={{ fontFamily: "var(--font-mono)", fontSize: 11 }}>{user.email}</span>
                      <Badge variant={user.role === "ADMIN" ? "role-admin" : "role-user"}>{user.role}</Badge>
                      <span style={{ fontFamily: "var(--font-mono)", fontSize: 12 }}>{user.auditCountToday}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            {usersData && usersData.totalPages > 1 && (
              <div className="flex items-center justify-between mt-3">
                <Button variant="outline" size="sm" disabled={usersPage === 0}
                  onClick={() => setUsersPage((p) => Math.max(0, p - 1))} className="flex items-center gap-1">
                  <ChevronLeft size={13} /> Prev
                </Button>
                <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                  Page {usersPage + 1} of {usersData.totalPages}
                </span>
                <Button variant="outline" size="sm" disabled={usersPage + 1 >= usersData.totalPages}
                  onClick={() => setUsersPage((p) => p + 1)} className="flex items-center gap-1">
                  Next <ChevronRight size={13} />
                </Button>
              </div>
            )}
          </TabsContent>

          <TabsContent value="audits" className="mt-4">
            <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">
              <div className="hidden md:grid border-b-2 border-black px-5 py-2.5 bg-muted"
                style={{ gridTemplateColumns: "2fr 2fr 90px 90px 120px 30px" }}>
                {["AUDIT ID", "USER", "STATUS", "FLAGGED", "CREATED", ""].map((h) => (
                  <span key={h} style={{ fontFamily: "var(--font-display)", fontSize: 10, letterSpacing: "0.08em", color: "var(--muted-foreground)" }}>
                    {h}
                  </span>
                ))}
              </div>
              <div className="divide-y-2 divide-black/10">
                {auditsLoading && (
                  <div className="px-5 py-6 text-center" style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                    Loading audits…
                  </div>
                )}
                {!auditsLoading && audits.length === 0 && (
                  <div className="px-5 py-6 text-center" style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                    No flagged audits.
                  </div>
                )}
                {audits.map((a) => (
                  <button key={a.id} onClick={() => navigate(`/app/audits/${a.id}`)}
                    className="w-full text-left px-5 py-4 hover:bg-[#F0EDE4] transition-colors">
                    <div className="md:hidden space-y-1">
                      <div className="flex items-start justify-between">
                        <span style={{ fontFamily: "var(--font-mono)", fontSize: 11 }} className="line-clamp-1">
                          {a.id.slice(0, 8)}
                        </span>
                        <ChevronRight size={13} style={{ color: "var(--muted-foreground)" }} />
                      </div>
                      <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
                        {a.user.displayName ?? a.user.email}
                      </div>
                      <div className="flex gap-2 flex-wrap">
                        <Badge variant={statusVariant(a.status)}>{a.status}</Badge>
                        {a.suspicious && <Badge variant="destructive">FLAGGED</Badge>}
                      </div>
                    </div>
                    <div className="hidden md:grid items-center gap-2" style={{ gridTemplateColumns: "2fr 2fr 90px 90px 120px 30px" }}>
                      <span style={{ fontFamily: "var(--font-mono)", fontSize: 11 }} className="truncate">
                        {a.id.slice(0, 8)}…
                      </span>
                      <div className="min-w-0">
                        <div style={{ fontFamily: "var(--font-display)", fontSize: 13 }} className="truncate">
                          {a.user.displayName ?? "—"}
                        </div>
                        <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
                          {a.user.email}
                        </div>
                      </div>
                      <Badge variant={statusVariant(a.status)}>{a.status}</Badge>
                      <div>
                        {a.suspicious
                          ? <Badge variant="destructive">FLAGGED</Badge>
                          : <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>—</span>
                        }
                      </div>
                      <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                        {formatDate(a.createdAt)}
                      </span>
                      <ChevronRight size={13} style={{ color: "var(--muted-foreground)" }} />
                    </div>
                  </button>
                ))}
              </div>
            </div>
            {auditsData && auditsData.totalPages > 1 && (
              <div className="flex items-center justify-between mt-3">
                <Button variant="outline" size="sm" disabled={auditsPage === 0}
                  onClick={() => setAuditsPage((p) => Math.max(0, p - 1))} className="flex items-center gap-1">
                  <ChevronLeft size={13} /> Prev
                </Button>
                <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                  Page {auditsPage + 1} of {auditsData.totalPages}
                </span>
                <Button variant="outline" size="sm" disabled={auditsPage + 1 >= auditsData.totalPages}
                  onClick={() => setAuditsPage((p) => p + 1)} className="flex items-center gap-1">
                  Next <ChevronRight size={13} />
                </Button>
              </div>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}