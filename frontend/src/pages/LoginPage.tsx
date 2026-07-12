import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { replace, useNavigate } from "react-router-dom";
import { LoginRequestSchema, type LoginRequest } from "../schemas/auth-requests";
import { useAuthContext } from "../context/AuthContext";


export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuthContext();
  
  const { register, handleSubmit, formState: { errors, isSubmitting }, } = useForm<LoginRequest>({ resolver: zodResolver(LoginRequestSchema) });

  const onSubmit = async (data: LoginRequest) => {
    try {
      await login(data);
      navigate("/app/submit", { replace: true });
    } catch (err) {
      setFormError((err as { message: string }).message);
    }
  };

  const [formError, setFormError] = useState<string | null>(null);

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register("email")} placeholder="Email" />
      {errors.email && <p>{errors.email.message}</p>}

      <input {...register("password")} type="password" placeholder="Password" />
      {errors.password && <p>{errors.password.message}</p>}

      {formError && <p role="alert">{formError}</p>}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Logging in…" : "Log in"}
      </button>
    </form>
  );
}
