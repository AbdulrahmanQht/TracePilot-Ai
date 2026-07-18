import { useState } from "react";
import { Link } from "react-router";
import { Mail, ArrowRight, ArrowLeft, CheckCircle2, Info } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import AuthShell from "../components/AuthShell";
import { useForgotPassword } from "@/hooks/useAuth";
import { ForgotPasswordRequestSchema, type ForgotPasswordRequest } from "@/schemas/auth-requests";
import { useDocumentTitle } from "@/hooks/useDocumentTitle";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tooltip, TooltipTrigger, TooltipContent, TooltipProvider } from "@/components/ui/tooltip";

export default function ForgotPasswordPage() {
  useDocumentTitle("Forgot Password");
  const [submitted, setSubmitted] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordRequest>({
    resolver: zodResolver(ForgotPasswordRequestSchema),
    defaultValues: { email: "" },
  });

  const { mutate: forgotPassword, isPending } = useForgotPassword({
    onSuccess: () => setSubmitted(true),
    onError: () => setSubmitted(true),
  });

  function onSubmit(values: ForgotPasswordRequest) {
    forgotPassword(values.email);
  }

  return (
    <AuthShell>
      <div className="w-full max-w-[420px]">
        <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">

          <div className="border-b-2 border-black px-6 py-5 bg-primary">
            <h1 style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 22, letterSpacing: "-0.02em" }}>
              Forgot Password
            </h1>
            <p style={{ fontFamily: "var(--font-body)", color: "rgba(244,241,234,0.65)", fontSize: 13, marginTop: 4 }}>
              We&apos;ll email you a reset link
            </p>
          </div>

          <div className="p-6">
            {submitted ? (
              <div className="space-y-4 text-center py-2">
                <div className="mx-auto flex h-12 w-12 items-center justify-center border-2 border-black bg-accent">
                  <CheckCircle2 size={22} className="text-accent-foreground" />
                </div>
                <p style={{ fontFamily: "var(--font-body)", fontSize: 14, color: "var(--foreground)" }}>
                  If that email is registered, a reset link is on its way.
                </p>
                <p style={{ fontFamily: "var(--font-body)", fontSize: 12, color: "var(--muted-foreground)" }}>
                  Check your inbox. The link expires in 1 hour.
                </p>

                {/* Desktop: hover tooltip */}
                <div className="hidden md:flex justify-center">
                  <TooltipProvider>
                    <Tooltip>
                      <TooltipTrigger
                        render={
                          <button
                            type="button"
                            className="flex items-center gap-1.5 border-2 border-black/20 px-3 py-1.5 hover:border-black/40"
                          />
                        }
                      >
                        <Info size={12} className="text-muted-foreground shrink-0" />
                        <span style={{ fontFamily: "var(--font-body)", fontSize: 11, color: "var(--muted-foreground)" }}>
                          Don&apos;t see the email?
                        </span>
                      </TooltipTrigger>
                      <TooltipContent>
                        Check your spam/junk folder. Our emails are sent from noreply@trace-pilot.dev
                      </TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                </div>

                {/* Mobile: always-visible hint (touch has no hover to reveal a tooltip) */}
                <div className="flex md:hidden mx-auto max-w-[320px] items-start gap-2 border-2 border-black/20 bg-muted px-3 py-2 text-left">
                  <Info size={13} className="text-muted-foreground shrink-0 mt-0.5" />
                  <span style={{ fontFamily: "var(--font-body)", fontSize: 11, color: "var(--muted-foreground)" }}>
                    Don&apos;t see it? Check your spam/junk folder. Our emails are sent from{" "}
                    <span style={{ fontFamily: "var(--font-mono)" }}>noreply@trace-pilot.dev</span>
                  </span>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-3" noValidate>
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

                <Button type="submit" disabled={isPending} variant="default"
                  className="w-full flex items-center justify-center gap-2 py-3">
                  {isPending
                    ? <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: "0.1em" }}>Sending…</span>
                    : <><span>Send Reset Link</span><ArrowRight size={13} /></>}
                </Button>
              </form>
            )}
          </div>

          <div className="border-t-2 border-black px-6 py-4 bg-muted">
            <Link to="/login" style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--primary)" }}
              className="hover:underline flex items-center justify-center gap-1.5">
              <ArrowLeft size={12} />
              Back to login
            </Link>
          </div>
        </div>
      </div>
    </AuthShell>
  );
}