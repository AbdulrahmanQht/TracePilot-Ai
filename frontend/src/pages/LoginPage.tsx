import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { Github, Mail, Lock, ArrowRight, Eye, EyeOff } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import AuthShell from "../components/AuthShell";
import { useAuthContext } from "../context/AuthContext";
import { LoginRequestSchema, type LoginRequest } from "../schemas/auth-requests";
import type { ApiError } from "../schemas/error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";

export default function LoginPage() {
  const { login } = useAuthContext();
  const navigate = useNavigate();
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginRequest>({
    resolver: zodResolver(LoginRequestSchema),
    defaultValues: { email: "", password: "" },
  });

  async function onSubmit(values: LoginRequest) {
    setError("");
    setLoading(true);
    try {
      await login(values);
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
      <div className="w-full max-w-[420px]">
        <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">

          <div className="border-b-2 border-black px-6 py-5 bg-primary">
            <h1 style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 22, letterSpacing: "-0.02em" }}>
              Sign in
            </h1>
            <p style={{ fontFamily: "var(--font-body)", color: "rgba(244,241,234,0.65)", fontSize: 13, marginTop: 4 }}>
              to TracePilot.AI — your coding-agent auditor
            </p>
          </div>

          <div className="p-6 space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <Button variant="default" onClick={() => handleOAuth("github")} disabled={loading}
                className="flex items-center justify-center gap-2 py-3 bg-foreground text-background hover:bg-foreground/90">
                <Github size={15} /> GitHub
              </Button>
              <Button variant="outline" onClick={() => handleOAuth("google")} disabled={loading}
                className="flex items-center justify-center gap-2 py-3">
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

              <div className="space-y-1">
                <Label variant="default">EMAIL</Label>
                <div className="relative">
                  <Mail size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                  <Input type="email" placeholder="you@example.com" className="pl-9"
                    {...register("email")} />
                </div>
                {errors.email && (
                  <span style={{ fontFamily: "var(--font-body)", fontSize: 11, color: "var(--destructive)" }}>
                    {errors.email.message}
                  </span>
                )}
              </div>

              <div className="space-y-1">
                <Label variant="default">PASSWORD</Label>
                <div className="relative">
                  <Lock size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                  <Input type={showPw ? "text" : "password"} placeholder="••••••••" className="pl-9 pr-10"
                    {...register("password")} />
                  <Button type="button" variant="ghost" size="icon-sm"
                    onClick={() => setShowPw(!showPw)}
                    className="absolute right-1 top-1/2 -translate-y-1/2 h-8 w-8 border-transparent hover:border-transparent">
                    {showPw ? <EyeOff size={13} className="text-muted-foreground" /> : <Eye size={13} className="text-muted-foreground" />}
                  </Button>
                </div>
                {errors.password && (
                  <span style={{ fontFamily: "var(--font-body)", fontSize: 11, color: "var(--destructive)" }}>
                    {errors.password.message}
                  </span>
                )}
              </div>

              <div className="flex justify-end">
                <Link to="/forgot-password" style={{ fontFamily: "var(--font-body)", fontSize: 12, color: "var(--primary)" }} className="hover:underline">
                  Forgot password?
                </Link>
              </div>

              <Button type="submit" disabled={loading} variant="default"
                className="w-full flex items-center justify-center gap-2 py-3">
                {loading
                  ? <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: "0.1em" }}>SIGNING IN…</span>
                  : <><span>Sign In</span><ArrowRight size={13} /></>}
              </Button>
            </form>
          </div>

          <div className="border-t-2 border-black px-6 py-4 bg-muted">
            <p style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)", textAlign: "center" }}>
              No account yet?{" "}
              <Link to="/register" style={{ color: "var(--primary)", fontWeight: 700 }} className="hover:underline">
                Create one free
              </Link>
            </p>
          </div>
        </div>
      </div>
    </AuthShell>
  );
}