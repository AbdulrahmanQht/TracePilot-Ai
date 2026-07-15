import { AppRouter } from "./routes/AppRouter";
import { useAuthContext } from "./context/AuthContext";

function App() {
  const { isInitializing } = useAuthContext();

  // Block the whole app tree until the mount-time refresh() (cookie -> session)
  // resolves. Without this, ProtectedRoute's own isInitializing check still
  // works, but you'd flash public pages/layout chrome before redirecting.
  if (isInitializing) {
    return <FullPageSpinner />;
  }

  return <AppRouter />;
}

function FullPageSpinner() {
  return (
    <div style={{ display: "flex", height: "100vh", alignItems: "center", justifyContent: "center" }}>
      Loading…
    </div>
  );
}

export default App;