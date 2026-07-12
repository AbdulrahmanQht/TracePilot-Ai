import { replace, useNavigate } from "react-router-dom";

export default function LandingPage() {
  const navigate = useNavigate();
  return <button onClick={() => navigate("/login", { replace: true })}>Login page</button>;
}
