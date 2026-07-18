import { useSearchParams, Link } from "react-router";
import { CheckCircle2, XCircle, ArrowLeft } from "lucide-react";
import AuthShell from "../components/AuthShell";
import { useDocumentTitle } from "@/hooks/useDocumentTitle";

export default function EmailVerifiedPage() {
  useDocumentTitle("Email Verified");
  const [searchParams] = useSearchParams();
  const success = searchParams.get("status") === "success";

  return (
    <AuthShell>
      <div className="w-full max-w-[420px]">
        <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">

          <div className="border-b-2 border-black px-6 py-5 bg-primary">
            <h1 style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 22, letterSpacing: "-0.02em" }}>
              Email Verification
            </h1>
            <p style={{ fontFamily: "var(--font-body)", color: "rgba(244,241,234,0.65)", fontSize: 13, marginTop: 4 }}>
              {success ? "You're all set" : "There was a problem"}
            </p>
          </div>

          <div className="p-6">
            <div className="space-y-4 text-center py-2">
              <div className={`mx-auto flex h-12 w-12 items-center justify-center border-2 border-black ${success ? "bg-accent" : "bg-destructive"}`}>
                {success
                  ? <CheckCircle2 size={22} className="text-accent-foreground" />
                  : <XCircle size={22} className="text-destructive-foreground" />}
              </div>
              <p style={{ fontFamily: "var(--font-body)", fontSize: 14, color: "var(--foreground)" }}>
                {success
                  ? "Your email has been verified."
                  : "This verification link is invalid or expired."}
              </p>
              <p style={{ fontFamily: "var(--font-body)", fontSize: 12, color: "var(--muted-foreground)" }}>
                {success
                  ? "You can now log in to your account."
                  : "Request a new verification email from the login page."}
              </p>
            </div>
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