import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { Lock, ArrowRight, Eye, EyeOff, CheckCircle2 } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import AuthShell from "../components/AuthShell";
import { useResetPassword } from "@/hooks/useAuth";
import { useDocumentTitle } from "@/hooks/useDocumentTitle";
import type { ApiError } from "@/schemas/error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";

const ResetPasswordFormSchema = z
  .object({
    newPassword: z.string().min(8, "Must be at least 8 characters"),
    confirmPassword: z.string().min(1, "Please confirm your password"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords don't match",
    path: ["confirmPassword"],
  });

type ResetPasswordForm = z.infer<typeof ResetPasswordFormSchema>;

export default function ResetPasswordPage() {
  useDocumentTitle("Reset Password");
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [showPw, setShowPw] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordForm>({
    resolver: zodResolver(ResetPasswordFormSchema),
    defaultValues: { newPassword: "", confirmPassword: "" },
  });

  const { mutate: resetPassword, isPending } = useResetPassword({
    onSuccess: () => setDone(true),
    onError: (err) => {
      const apiErr = err as ApiError;
      setError(apiErr?.message ?? "That link is invalid or has expired.");
    },
  });

  function onSubmit(values: ResetPasswordForm) {
    if (!token) return;
    setError("");
    resetPassword({ token, newPassword: values.newPassword });
  }

  return (
    <AuthShell>
      <div className="w-full max-w-[420px]">
        <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">

          <div className="border-b-2 border-black px-6 py-5 bg-primary">
            <h1 style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 22, letterSpacing: "-0.02em" }}>
              Reset Password
            </h1>
            <p style={{ fontFamily: "var(--font-body)", color: "rgba(244,241,234,0.65)", fontSize: 13, marginTop: 4 }}>
              Choose a new password
            </p>
          </div>

          <div className="p-6">
            {!token ? (
              <div className="space-y-3">
                <Alert variant="destructive">
                  <AlertDescription style={{ fontFamily: "var(--font-body)", fontSize: 13 }}>
                    This reset link is missing or invalid.
                  </AlertDescription>
                </Alert>
                <Link to="/forgot-password">
                  <Button type="button" variant="default" className="w-full py-3">
                    Request a new link
                  </Button>
                </Link>
              </div>
            ) : done ? (
              <div className="space-y-4 text-center py-2">
                <div className="mx-auto flex h-12 w-12 items-center justify-center border-2 border-black bg-accent">
                  <CheckCircle2 size={22} className="text-accent-foreground" />
                </div>
                <p style={{ fontFamily: "var(--font-body)", fontSize: 14, color: "var(--foreground)" }}>
                  Your password has been updated.
                </p>
                <Button type="button" variant="default" className="w-full py-3" onClick={() => navigate("/login")}>
                  Continue to login
                </Button>
              </div>
            ) : (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-3" noValidate>
                {error && (
                  <Alert variant="destructive">
                    <AlertDescription style={{ fontFamily: "var(--font-body)", fontSize: 13 }}>{error}</AlertDescription>
                  </Alert>
                )}

                <div className="space-y-1">
                  <Label variant="default">NEW PASSWORD</Label>
                  <div className="relative">
                    <Lock size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                    <Input type={showPw ? "text" : "password"} placeholder="••••••••" className="pl-9 pr-10"
                      {...register("newPassword")} />
                    <Button type="button" variant="ghost" size="icon-sm"
                      onClick={() => setShowPw(!showPw)}
                      className="absolute right-1 top-1/2 -translate-y-1/2 h-8 w-8 border-transparent hover:border-transparent">
                      {showPw ? <EyeOff size={13} className="text-muted-foreground" /> : <Eye size={13} className="text-muted-foreground" />}
                    </Button>
                  </div>
                  {errors.newPassword && (
                    <span style={{ fontFamily: "var(--font-body)", fontSize: 11, color: "var(--destructive)" }}>
                      {errors.newPassword.message}
                    </span>
                  )}
                </div>

                <div className="space-y-1">
                  <Label variant="default">CONFIRM PASSWORD</Label>
                  <div className="relative">
                    <Lock size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                    <Input type={showPw ? "text" : "password"} placeholder="••••••••" className="pl-9"
                      {...register("confirmPassword")} />
                  </div>
                  {errors.confirmPassword && (
                    <span style={{ fontFamily: "var(--font-body)", fontSize: 11, color: "var(--destructive)" }}>
                      {errors.confirmPassword.message}
                    </span>
                  )}
                </div>

                <Button type="submit" disabled={isPending} variant="default"
                  className="w-full flex items-center justify-center gap-2 py-3">
                  {isPending
                    ? <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: "0.1em" }}>Updating…</span>
                    : <><span>Reset Password</span><ArrowRight size={13} /></>}
                </Button>
              </form>
            )}
          </div>

          <div className="border-t-2 border-black px-6 py-4 bg-muted">
            <p style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)", textAlign: "center" }}>
              Remembered it?{" "}
              <Link to="/login" style={{ color: "var(--primary)", fontWeight: 700 }} className="hover:underline">
                Back to login
              </Link>
            </p>
          </div>
        </div>
      </div>
    </AuthShell>
  );
}