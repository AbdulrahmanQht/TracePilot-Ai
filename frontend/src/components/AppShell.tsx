import { Outlet, NavLink, useNavigate } from "react-router";
import { Terminal, Send, History, TrendingUp, User, Shield, LogOut, ChevronRight } from "lucide-react";
import { useAuthContext } from "@/context/AuthContext";
import { useSystemHealth } from "@/hooks/useSystemHealth";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";

const navItems = [
  { to: "/app/submit",      label: "Submit Trace",    icon: <Send size={15} />       },
  { to: "/app/history",     label: "History",         icon: <History size={15} />    },
  { to: "/app/reliability", label: "Reliability",     icon: <TrendingUp size={15} /> },
  { to: "/app/profile",     label: "Profile",         icon: <User size={15} />       },
];

export default function AppShell() {
  const { user, logout } = useAuthContext();
  const navigate = useNavigate();
  const isAdmin = user?.role === "ADMIN";
  const systemStatus = useSystemHealth();

  const dotColor =
    systemStatus === "healthy"
      ? "var(--color-sage)"
      : systemStatus === "unhealthy"
        ? "var(--color-crimson)"
        : "var(--color-ink-muted)";

  const dotLabel =
    systemStatus === "healthy"
      ? "SYSTEMS ONLINE"
      : systemStatus === "unhealthy"
        ? "SYSTEMS OFFLINE"
        : "CHECKING STATUS";
  const initials = (user?.displayName?.trim() || user?.email || "?")
    .slice(0, 2)
    .toUpperCase();

  async function handleLogout() {
    await logout();
    navigate("/");
  }

  return (
    <div className="flex min-h-screen bg-background" style={{ fontFamily: "var(--font-body)" }}>

      <aside className="w-[224px] shrink-0 flex flex-col border-r-2 border-black sticky top-0 h-screen bg-primary">

        {/* Logo */}
        <div className="border-b-2 border-black px-5 py-4 flex items-center gap-2.5">
          <div className="w-8 h-8 border-2 border-black flex items-center justify-center shrink-0 bg-secondary">
            <Terminal size={13} className="text-primary-foreground" />
          </div>
          <span style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 15, letterSpacing: "-0.02em" }}>
            TracePilot
          </span>
        </div>

        {/* Live indicator */}
        <div
          className="border-b-2 border-black px-5 py-2.5 flex items-center gap-2"
          style={{ background: "#162D23" }}
        >
          <span
          style={{
            width: 6,
            height: 6,
            borderRadius: "50%",
            background: dotColor,
            display: "inline-block",
            animation:
              systemStatus === "healthy"
                ? "rawPulse 1.2s ease-in-out infinite"
                : undefined,
          }}
        />
          <span
            style={{
              fontFamily: "var(--font-mono)",
              fontSize: 9,
              color: "rgba(244,241,234,0.5)",
              letterSpacing: "0.1em",
            }}
          >
            {dotLabel}
          </span>
        </div>

        {/* Nav label */}
        <div className="px-5 pt-5 pb-2">
          <span style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "rgba(244,241,234,0.4)", letterSpacing: "0.12em" }}>
            WORKSPACE
          </span>
        </div>

        {/* Nav items */}
        <nav className="flex-1 px-3 space-y-1">
          {navItems.map(({ to, label, icon }) => (
            <NavLink key={to} to={to}
              className={({ isActive }) =>
                `flex items-center gap-2.5 px-3 py-2.5 border-2 transition-all ${
                  isActive
                    ? "border-black shadow-[2px_2px_0px_#0D0D0D] -translate-x-px -translate-y-px bg-background"
                    : "border-transparent hover:border-black/30"
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <span style={{ color: isActive ? "var(--primary)" : "rgba(244,241,234,0.7)" }}>{icon}</span>
                  <span style={{ fontFamily: "var(--font-display)", fontSize: 12, color: isActive ? "var(--primary)" : "var(--primary-foreground)" }}>{label}</span>
                  {isActive && <ChevronRight size={11} className="ml-auto text-primary" />}
                </>
              )}
            </NavLink>
          ))}

          {isAdmin && (
            <>
              <div className="mx-3 my-3 border-t border-black/20" />
              <NavLink to="/admin"
                className={({ isActive }) =>
                  `flex items-center gap-2.5 px-3 py-2.5 border-2 transition-all ${
                    isActive
                      ? "border-black shadow-[2px_2px_0px_#0D0D0D] -translate-x-px -translate-y-px bg-background"
                      : "border-transparent hover:border-black/30"
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    <span style={{ color: isActive ? "var(--destructive)" : "rgba(244,241,234,0.7)" }}><Shield size={15} /></span>
                    <span style={{ fontFamily: "var(--font-display)", fontSize: 12, color: isActive ? "var(--primary)" : "var(--primary-foreground)" }}>Admin Panel</span>
                    {isActive && <ChevronRight size={11} className="ml-auto text-primary" />}
                  </>
                )}
              </NavLink>
            </>
          )}
        </nav>

        {/* User chip */}
        <div className="border-t-2 border-black p-3 space-y-2" style={{ background: "#162D23" }}>
          <div className="flex items-center gap-2.5 px-2 py-2">
            <Avatar size="sm" className="!size-8 shrink-0 rounded-none">
              <AvatarFallback color="secondary" size="sm">
                {initials}
              </AvatarFallback>
            </Avatar>
            <div className="flex-1 min-w-0">
              <div style={{ fontFamily: "var(--font-display)", fontSize: 11, color: "var(--primary-foreground)" }}>
                {user?.displayName?.trim() || user?.email}
              </div>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "rgba(244,241,234,0.45)" }} className="truncate">
                {user?.email}
              </div>
            </div>
            {isAdmin && (
              <Badge variant="role-admin" className="text-[8px] px-1.5">ADMIN</Badge>
            )}
          </div>
          <Button
            variant="ghost"
            onClick={handleLogout}
            className="w-full justify-start gap-2 h-auto px-3 py-2 text-xs border-2 border-black/30 hover:border-black hover:bg-black/20 hover:text-[rgba(244,241,234,0.8)]"
            style={{ fontFamily: "var(--font-body)", color: "rgba(244,241,234,0.6)" }}
          >
            <LogOut size={12} />
            Logout
          </Button>
        </div>
      </aside>

      <main className="flex-1 min-w-0">
        <Outlet />
      </main>
    </div>
  );
}