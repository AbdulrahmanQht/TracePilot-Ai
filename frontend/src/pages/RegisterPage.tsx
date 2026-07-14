import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router";
import { Github, Mail, Lock, User, ArrowRight, Eye, EyeOff } from "lucide-react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import AuthShell from "../components/AuthShell";
import { useAuthContext } from "../context/AuthContext";
import { RegisterRequestSchema, type RegisterRequest } from "../schemas/auth-requests";
import type { ApiError } from "../schemas/error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";

const RegisterFormSchema = RegisterRequestSchema.extend({
  confirmPassword: z.string().min(1, "Please confirm your password"),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});
type RegisterFormValues = z.infer<typeof RegisterFormSchema>;

const DRAFT_KEY = "tracepilot:register-draft";

function blockClipboard(e: React.ClipboardEvent<HTMLInputElement>) {
  e.preventDefault();
}

function Field({
  label, type = "text", placeholder, icon, suffix, registration, errorMsg, onPasswordGuard,
}: {
  label: string;
  type?: string;
  placeholder: string;
  icon: React.ReactNode;
  suffix?: React.ReactNode;
  registration: UseFormRegisterReturn;
  errorMsg?: string | undefined;
  onPasswordGuard?: boolean;
}) {
  return (
    <div className="space-y-1">
      <Label variant="default">{label}</Label>
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none">{icon}</span>
        <Input
          type={type}
          placeholder={placeholder}
          className="pl-9 pr-10"
          onCopy={onPasswordGuard ? blockClipboard : undefined}
          onPaste={onPasswordGuard ? blockClipboard : undefined}
          onCut={onPasswordGuard ? blockClipboard : undefined}
          {...registration}
        />
        {suffix && <div className="absolute right-1 top-1/2 -translate-y-1/2">{suffix}</div>}
      </div>
      {errorMsg && (
        <span style={{ fontFamily: "var(--font-body)", fontSize: 11, color: "var(--destructive)" }}>
          {errorMsg}
        </span>
      )}
    </div>
  );
}

function ToggleVisibilityButton({ visible, onToggle }: { visible: boolean; onToggle: () => void }) {
  return (
    <Button
      type="button"
      variant="ghost"
      size="icon-sm"
      onClick={onToggle}
      className="h-8 w-8 border-transparent hover:border-transparent"
    >
      {visible ? <EyeOff size={13} className="text-muted-foreground" /> : <Eye size={13} className="text-muted-foreground" />}
    </Button>
  );
}

function getPasswordStrength(pw: string): { label: string; filled: number; tone: "danger" | "success" | "neutral" } {
  if (!pw) return { label: "", filled: 0, tone: "neutral" };
  if (pw.length < 8) return { label: "TOO SHORT", filled: 1, tone: "danger" };
 
  let varietyScore = 0;
  if (/[a-z]/.test(pw)) varietyScore++;
  if (/[A-Z]/.test(pw)) varietyScore++;
  if (/[0-9]/.test(pw)) varietyScore++;
  if (/[^A-Za-z0-9]/.test(pw)) varietyScore++;
 
  let lengthScore = 0;
  if (pw.length >= 12) lengthScore++;
  if (pw.length >= 16) lengthScore++;
 
  const total = varietyScore + lengthScore; // max 6
 
  if (total <= 2) return { label: "WEAK", filled: 1, tone: "success" };
  if (total <= 4) return { label: "GOOD", filled: 2, tone: "success" };
  return { label: "STRONG", filled: 3, tone: "success" };
}

export default function RegisterPage() {
  const { register: registerUser } = useAuthContext();
  const navigate = useNavigate();
  const [showPw, setShowPw] = useState(false);
  const [showConfirmPw, setShowConfirmPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(RegisterFormSchema),
    defaultValues: { email: "", password: "", confirmPassword: "", displayName: "" },
  });

  const passwordValue = watch("password") ?? "";
  const strength = getPasswordStrength(passwordValue);

  // Restore a draft of the non-sensitive fields on mount (survives page
  // switches / reloads). Deliberately excludes password/confirmPassword —
  // persisting password values in browser storage is a security anti-pattern
  // even when scoped to sessionStorage.
  useEffect(() => {
    const saved = sessionStorage.getItem(DRAFT_KEY);
    if (!saved) return;
    try {
      const parsed = JSON.parse(saved) as { displayName?: string; email?: string };
      if (parsed.displayName) setValue("displayName", parsed.displayName);
      if (parsed.email) setValue("email", parsed.email);
    } catch {
      // ignore malformed/corrupt draft
    }
  }, [setValue]);

  useEffect(() => {
    const subscription = watch((values) => {
      sessionStorage.setItem(
        DRAFT_KEY,
        JSON.stringify({ displayName: values.displayName ?? "", email: values.email ?? "" })
      );
    });
    return () => subscription.unsubscribe();
  }, [watch]);

  async function onSubmit(values: RegisterFormValues) {
    setError("");
    setLoading(true);
    try {
      const payload: RegisterRequest = {
        email: values.email,
        password: values.password,
        displayName: values.displayName,
      };
      await registerUser(payload);
      sessionStorage.removeItem(DRAFT_KEY);
      navigate("/app/submit");
    } catch (err) {
      const apiErr = err as ApiError;
      setError(apiErr?.message ?? "Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  function handleOAuth(provider: "github" | "google") {
    const serverOrigin = new URL(import.meta.env.VITE_API_BASE_URL).origin;
    window.location.href = `${serverOrigin}/oauth2/authorization/${provider}`;
  }

  return (
    <AuthShell>
      <div className="w-full max-w-[440px]">
        <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">

          <div className="border-b-2 border-black px-6 py-5 bg-secondary">
            <h1 style={{ fontFamily: "var(--font-display)", color: "var(--secondary-foreground)", fontSize: 22, letterSpacing: "-0.02em" }}>
              Create account
            </h1>
            <p style={{ fontFamily: "var(--font-body)", color: "rgba(244,241,234,0.65)", fontSize: 13, marginTop: 4 }}>
              Start auditing your AI coding agents for free
            </p>
          </div>

          <div className="p-6 space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <Button variant="default" onClick={() => handleOAuth("github")} disabled={loading}
                className="flex items-center justify-center gap-2 bg-foreground text-background hover:bg-foreground/90">
                <Github size={15} /> GitHub
              </Button>
              <Button variant="outline" onClick={() => handleOAuth("google")} disabled={loading}
                className="flex items-center justify-center gap-2">
                <svg width="14" height="14" viewBox="0 0 24 24"><path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/><path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/><path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/><path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/></svg>
                Google
              </Button>
            </div>

            <div className="flex items-center gap-3">
              <div className="flex-1 border-t-2 border-black/10" />
              <span style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)", letterSpacing: "0.1em" }}>OR</span>
              <div className="flex-1 border-t-2 border-black/10" />
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-3" noValidate>
              {error && (
                <Alert variant="destructive">
                  <AlertDescription style={{ fontFamily: "var(--font-body)", fontSize: 13 }}>{error}</AlertDescription>
                </Alert>
              )}

              <Field label="DISPLAY NAME" placeholder="Your name" icon={<User size={13} />}
                registration={register("displayName")} errorMsg={errors.displayName?.message} />

              <Field label="EMAIL" type="email" placeholder="you@example.com" icon={<Mail size={13} />}
                registration={register("email")} errorMsg={errors.email?.message} />

              <Field
                label="PASSWORD"
                type={showPw ? "text" : "password"}
                placeholder="Min. 8 characters"
                icon={<Lock size={13} />}
                registration={register("password")}
                errorMsg={errors.password?.message}
                onPasswordGuard
                suffix={<ToggleVisibilityButton visible={showPw} onToggle={() => setShowPw(v => !v)} />}
              />

              <Field
                label="CONFIRM PASSWORD"
                type={showConfirmPw ? "text" : "password"}
                placeholder="Repeat password"
                icon={<Lock size={13} />}
                registration={register("confirmPassword")}
                errorMsg={errors.confirmPassword?.message}
                onPasswordGuard
                suffix={<ToggleVisibilityButton visible={showConfirmPw} onToggle={() => setShowConfirmPw(v => !v)} />}
              />

              {passwordValue && (
                <div className="space-y-1">
                  <div className="flex gap-1">
                    {[1, 2, 3].map(n => {
                      const isFilled = strength.filled >= n;
                      const color = !isFilled
                        ? "var(--muted)"
                        : strength.tone === "danger"
                          ? "var(--destructive)"
                          : "var(--primary)";
                      return (
                        <div key={n} className="flex-1 h-1.5 border border-black"
                          style={{ background: color }} />
                      );
                    })}
                  </div>
                  <span style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)" }}>
                    {strength.label}
                  </span>
                </div>
              )}

              <Button type="submit" disabled={loading} variant="default"
                className="w-full flex items-center justify-center gap-2 py-3 mt-1">
                {loading
                  ? <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: "0.1em" }}>CREATING ACCOUNT…</span>
                  : <><span>Create Account</span><ArrowRight size={13} /></>}
              </Button>
            </form>

            <p style={{ fontFamily: "var(--font-body)", fontSize: 11, color: "var(--muted-foreground)", textAlign: "center", lineHeight: 1.5 }}>
              By creating an account you agree to our{" "}
              <a href="#" className="hover:underline" style={{ color: "var(--primary)" }}>Terms of Service</a> and{" "}
              <a href="#" className="hover:underline" style={{ color: "var(--primary)" }}>Privacy Policy</a>.
            </p>
          </div>

          <div className="border-t-2 border-black px-6 py-4 bg-muted">
            <p style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)", textAlign: "center" }}>
              Already have an account?{" "}
              <Link to="/login" style={{ color: "var(--primary)", fontWeight: 700 }} className="hover:underline">Sign in</Link>
            </p>
          </div>
        </div>
      </div>
    </AuthShell>
  );
}