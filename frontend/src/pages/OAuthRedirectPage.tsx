import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { useAuthContext } from "../context/AuthContext";

export default function OAuthRedirectPage() {
  const [params] = useSearchParams();
  const { loginWithToken } = useAuthContext();
  const navigate = useNavigate();
  const [error, setError] = useState(false);
  const ranOnce = useRef(false);

  useEffect(() => {
    if (ranOnce.current) return;
    ranOnce.current = true;

    const token = params.get("token");

    if (!token) {
      navigate("/login?error=oauth_failed", { replace: true });
      return;
    }

    loginWithToken(token)
      .then(() => navigate("/app/submit", { replace: true }))
      .catch(() => {
        setError(true);
        navigate("/login?error=oauth_failed", { replace: true });
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="border-2 border-black px-10 py-8 text-center shadow-[4px_4px_0px_#0D0D0D] bg-primary">
        <div className="flex justify-center mb-4 gap-1.5">
          {[0, 1, 2].map(i => (
            <div key={i} className="w-3 h-3 border-2 border-black"
              style={{ background: "#A8D5A2", animation: `rawPulse 1s ease-in-out ${i * 0.2}s infinite` }} />
          ))}
        </div>
        <p style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 16 }}>
          {error ? "Sign-in failed…" : "Completing sign-in…"}
        </p>
        <p style={{ fontFamily: "var(--font-mono)", color: "rgba(244,241,234,0.5)", fontSize: 11, marginTop: 6, letterSpacing: "0.06em" }}>
          {error ? "REDIRECTING TO LOGIN" : "VALIDATING OAUTH TOKEN"}
        </p>
      </div>
      <style>{`@keyframes rawPulse { 0%,100%{opacity:1} 50%{opacity:0.2} }`}</style>
    </div>
  );
}