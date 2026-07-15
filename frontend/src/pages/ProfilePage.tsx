import { useState } from "react";
import { useNavigate } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { User, Mail, Lock, Shield, Eye, EyeOff, Check, AlertTriangle } from "lucide-react";
import { useAuthContext } from "@/context/AuthContext";
import { useUpdateUser, useDeleteUser } from "@/hooks/useUser";
import { useChangePassword } from "@/hooks/useAuth";
import { useAuditList } from "@/hooks/useAudit";
import { UpdateUserRequestSchema } from "@/schemas/user";
import { ChangePasswordRequestSchema } from "@/schemas/auth-requests";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Progress, ProgressTrack, ProgressIndicator } from "@/components/ui/progress";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Alert, AlertDescription } from "@/components/ui/alert";

const DAILY_AUDIT_LIMIT = 10;

const ChangePasswordFormSchema = ChangePasswordRequestSchema.extend({
  confirmPassword: z.string().min(1),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords do not match.",
  path: ["confirmPassword"],
});
type ChangePasswordFormValues = z.infer<typeof ChangePasswordFormSchema>;

export default function ProfilePage() {
  const { user, logout } = useAuthContext();
  const navigate = useNavigate();

  const [showPw, setShowPw] = useState(false);
  const [nameSaved, setNameSaved] = useState(false);
  const [pwSaved, setPwSaved] = useState(false);

  const { data: auditPage } = useAuditList({ page: 0, size: 1 });

  const updateUserMutation = useUpdateUser();
  const changePasswordMutation = useChangePassword();
  const deleteUserMutation = useDeleteUser();

  const {
    register: registerName,
    handleSubmit: handleNameSubmit,
    formState: { errors: nameErrors },
  } = useForm<{ displayName: string }>({
    resolver: zodResolver(UpdateUserRequestSchema),
    defaultValues: { displayName: user?.displayName ?? "" },
  });

  const {
    register: registerPw,
    handleSubmit: handlePwSubmit,
    reset: resetPwForm,
    formState: { errors: pwErrors },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(ChangePasswordFormSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  function onSaveProfile(values: { displayName: string }) {
    updateUserMutation.mutate(
      { displayName: values.displayName },
      {
        onSuccess: () => {
          setNameSaved(true);
          setTimeout(() => setNameSaved(false), 2500);
        },
      }
    );
  }

  function onSavePassword(values: ChangePasswordFormValues) {
    changePasswordMutation.mutate(
      { currentPassword: values.currentPassword, newPassword: values.newPassword },
      {
        onSuccess: () => {
          setPwSaved(true);
          resetPwForm();
          setTimeout(() => setPwSaved(false), 3000);
        },
      }
    );
  }

  async function onDeleteAccount() {
    const confirmed = window.confirm(
      "This will permanently delete your account and all audit data. This cannot be undone. Continue?"
    );
    if (!confirmed) return;

    await deleteUserMutation.mutateAsync();
    await logout();
    navigate("/");
  }

  const displayName = user?.displayName ?? "";
  const initials =
    displayName
      .split(" ")
      .map((w) => w[0])
      .join("")
      .slice(0, 2)
      .toUpperCase() || "U";

  const auditCountToday = user?.auditCountToday ?? 0;
  const quotaReached = auditCountToday >= DAILY_AUDIT_LIMIT;
  const quotaPct = Math.min(100, (auditCountToday / DAILY_AUDIT_LIMIT) * 100);
  const totalAudits = auditPage?.totalElements;

  return (
    <div className="bg-background min-h-screen" style={{ fontFamily: "var(--font-body)" }}>
      <div className="border-b-2 border-black px-8 py-5 bg-card">
        <div className="flex items-center gap-3 mb-1">
          <div className="border-2 border-black p-2 bg-primary">
            <User size={14} className="text-primary-foreground" />
          </div>
          <h1 style={{ fontFamily: "var(--font-display)", fontSize: 22, letterSpacing: "-0.02em" }}>Profile</h1>
        </div>
        <p style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)" }}>
          Manage your account settings and preferences.
        </p>
      </div>

      <div className="mx-auto max-w-[900px] px-8 py-7 space-y-5">
        <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] flex flex-wrap items-center gap-6 px-7 py-6 bg-card">
          <Avatar size="lg" className="!size-16 !border-4 shrink-0">
            <AvatarFallback color="secondary" size="lg">{initials}</AvatarFallback>
          </Avatar>
          <div className="flex-1">
            <div style={{ fontFamily: "var(--font-display)", fontSize: 20, letterSpacing: "-0.02em" }}>
              {displayName || "—"}
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--muted-foreground)", marginTop: 2 }}>
              {user?.email}
            </div>
            {user?.role === "ADMIN" && (
              <Badge variant="role-admin" className="mt-2 inline-flex items-center gap-1">
                <Shield size={9} /> ADMIN
              </Badge>
            )}
          </div>
          <div className="grid grid-cols-2 gap-4">
            {[
              { label: "TOTAL AUDITS", value: totalAudits ?? "—" },
              { label: "TODAY", value: `${auditCountToday}/${DAILY_AUDIT_LIMIT}` },
            ].map(({ label, value }) => (
              <div key={label} className="border-2 border-black px-4 py-3 text-center bg-muted">
                <div style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)", letterSpacing: "0.1em" }}>
                  {label}
                </div>
                <div style={{ fontFamily: "var(--font-display)", fontSize: 22, letterSpacing: "-0.03em", marginTop: 2 }}>
                  {value}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="border-2 border-black px-6 py-5 bg-card">
          <div className="flex items-center justify-between mb-3">
            <span style={{ fontFamily: "var(--font-display)", fontSize: 12, letterSpacing: "0.04em" }}>
              DAILY AUDIT QUOTA
            </span>
            <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
              {auditCountToday} of {DAILY_AUDIT_LIMIT} used today
            </span>
          </div>
          <Progress value={quotaPct}>
            <ProgressTrack size="default">
              <ProgressIndicator color={quotaReached ? "destructive" : "primary"} />
            </ProgressTrack>
          </Progress>
          {quotaReached && (
            <p style={{ fontFamily: "var(--font-body)", fontSize: 12, color: "var(--destructive)", marginTop: 6 }}>
              Daily quota reached. Resets at midnight UTC.
            </p>
          )}
        </div>

        <form onSubmit={handleNameSubmit(onSaveProfile)} className="border-2 border-black bg-card">
          <div className="border-b-2 border-black px-5 py-3 bg-muted">
            <span style={{ fontFamily: "var(--font-display)", fontSize: 11, letterSpacing: "0.06em" }}>
              ACCOUNT DETAILS
            </span>
          </div>
          <div className="p-5 space-y-4">
            <div className="space-y-1">
              <Label htmlFor="displayName" variant="default">DISPLAY NAME</Label>
              <div className="relative">
                <User size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                <Input id="displayName" className="pl-9" {...registerName("displayName")} />
              </div>
              {nameErrors.displayName && (
                <p style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--destructive)" }}>
                  {nameErrors.displayName.message}
                </p>
              )}
            </div>
            <div className="space-y-1">
              <Label variant="default">EMAIL</Label>
              <div className="flex items-center gap-2 border-2 border-black px-3 py-2.5 opacity-60 bg-muted">
                <Mail size={13} style={{ color: "var(--muted-foreground)" }} />
                <span style={{ fontFamily: "var(--font-body)", fontSize: 14, color: "var(--foreground)" }}>
                  {user?.email}
                </span>
              </div>
              <p style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
                Email cannot be changed.
              </p>
            </div>
            {updateUserMutation.isError && (
              <Alert variant="destructive">
                <AlertTriangle size={13} />
                <AlertDescription style={{ fontFamily: "var(--font-body)", fontSize: 12 }}>
                  {updateUserMutation.error?.message}
                </AlertDescription>
              </Alert>
            )}
            <Button type="submit" variant={nameSaved ? "muted" : "default"} className="flex items-center gap-2"
              disabled={updateUserMutation.isPending}>
              {nameSaved ? (<><Check size={13} /> Saved!</>) : updateUserMutation.isPending ? "Saving…" : "Save Changes"}
            </Button>
          </div>
        </form>

        <form onSubmit={handlePwSubmit(onSavePassword)} className="border-2 border-black bg-card">
          <div className="border-b-2 border-black px-5 py-3 bg-muted">
            <span style={{ fontFamily: "var(--font-display)", fontSize: 11, letterSpacing: "0.06em" }}>
              CHANGE PASSWORD
            </span>
          </div>
          <div className="p-5 space-y-4">
            {changePasswordMutation.isError && (
              <Alert variant="destructive">
                <AlertDescription style={{ fontFamily: "var(--font-body)", fontSize: 12 }}>
                  {changePasswordMutation.error?.message}
                </AlertDescription>
              </Alert>
            )}

            <div className="space-y-1">
              <Label htmlFor="currentPassword" variant="default">CURRENT PASSWORD</Label>
              <div className="relative">
                <Lock size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                <Input
                  id="currentPassword"
                  type={showPw ? "text" : "password"}
                  placeholder="••••••••"
                  className="pl-9 pr-10"
                  autoComplete="current-password"
                  onCopy={(e) => e.preventDefault()}
                  onPaste={(e) => e.preventDefault()}
                  {...registerPw("currentPassword")}
                />
                <Button type="button" variant="ghost" size="icon-sm"
                  onClick={() => setShowPw((s) => !s)}
                  className="absolute right-1 top-1/2 -translate-y-1/2 h-8 w-8 border-transparent hover:border-transparent">
                  {showPw ? <EyeOff size={13} className="text-muted-foreground" /> : <Eye size={13} className="text-muted-foreground" />}
                </Button>
              </div>
              {pwErrors.currentPassword && (
                <p style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--destructive)" }}>
                  {pwErrors.currentPassword.message}
                </p>
              )}
            </div>

            <div className="space-y-1">
              <Label htmlFor="newPassword" variant="default">NEW PASSWORD</Label>
              <div className="relative">
                <Lock size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                <Input
                  id="newPassword"
                  type={showPw ? "text" : "password"}
                  placeholder="••••••••"
                  className="pl-9"
                  autoComplete="new-password"
                  onCopy={(e) => e.preventDefault()}
                  onPaste={(e) => e.preventDefault()}
                  {...registerPw("newPassword")}
                />
              </div>
              {pwErrors.newPassword && (
                <p style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--destructive)" }}>
                  {pwErrors.newPassword.message}
                </p>
              )}
            </div>

            <div className="space-y-1">
              <Label htmlFor="confirmPassword" variant="default">CONFIRM NEW PASSWORD</Label>
              <div className="relative">
                <Lock size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                <Input
                  id="confirmPassword"
                  type={showPw ? "text" : "password"}
                  placeholder="••••••••"
                  className="pl-9"
                  autoComplete="new-password"
                  onCopy={(e) => e.preventDefault()}
                  onPaste={(e) => e.preventDefault()}
                  {...registerPw("confirmPassword")}
                />
              </div>
              {pwErrors.confirmPassword && (
                <p style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--destructive)" }}>
                  {pwErrors.confirmPassword.message}
                </p>
              )}
            </div>

            <Button type="submit" variant={pwSaved ? "muted" : "secondary"} className="flex items-center gap-2"
              disabled={changePasswordMutation.isPending}>
              {pwSaved ? (<><Check size={13} /> Password Updated!</>) : (<><Lock size={13} /> {changePasswordMutation.isPending ? "Updating…" : "Update Password"}</>)}
            </Button>
          </div>
        </form>

        <div className="border-2 border-black bg-card">
          <div className="border-b-2 border-black px-5 py-3 bg-destructive/10">
            <span style={{ fontFamily: "var(--font-display)", fontSize: 11, letterSpacing: "0.06em", color: "var(--destructive)" }}>
              DANGER ZONE
            </span>
          </div>
          <div className="p-5 flex items-center justify-between gap-4">
            <div>
              <div style={{ fontFamily: "var(--font-display)", fontSize: 13 }}>Delete Account</div>
              <div style={{ fontFamily: "var(--font-body)", fontSize: 12, color: "var(--muted-foreground)" }}>
                Permanently delete your account and all audit data. This cannot be undone.
              </div>
            </div>
            <Button variant="destructive" className="shrink-0" onClick={onDeleteAccount}
              disabled={deleteUserMutation.isPending}>
              {deleteUserMutation.isPending ? "Deleting…" : "Delete Account"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}