import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import Response

from app.messaging import audit_consumer

from telemetry import setup_tracing

from utils.logger import Logger
from opentelemetry import trace

tracer = trace.get_tracer(__name__)
logger = Logger()


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("App started.")
    await audit_consumer.start()
    yield
    logger.info("App stoped.")
    await audit_consumer.stop()


app = FastAPI(lifespan=lifespan)
setup_tracing(app)


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logger.error(f"unhandled_exception path={request.url.path} error={str(exc)}")
    return JSONResponse(status_code=500, content={"detail": "Internal server error"})


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response: Response = await call_next(request)
        # Standard security headers for all routes
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        response.headers["Permissions-Policy"] = (
            "camera=(), microphone=(), geolocation=()"
        )

        path = request.url.path

        # Check if we are accessing the documentation or the OpenAPI schema
        if path in ["/docs", "/redoc", "/openapi.json"]:
            # CSP specifically for the docs UI
            csp = (
                "default-src 'self'; "
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; "
                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                "font-src 'self' https://fonts.gstatic.com; "
                "img-src 'self' data: blob: https://fastapi.tiangolo.com; "
                "connect-src 'self' http://127.0.0.1:8000;"
            )
        else:
            # CSP for all API endpoints and general traffic
            csp = (
                "default-src 'self'; "
                # Add 'unsafe-inline' right here:
                "script-src 'self' 'unsafe-eval'; "
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                "font-src 'self' https://fonts.gstatic.com; "
                "img-src 'self' data: blob:; "
                "connect-src 'self';"
            )

        response.headers["Content-Security-Policy"] = csp
        return response


class RequestLogMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        started = time.perf_counter()
        client_host = (
            request.headers.get("cf-connecting-ip")
            or request.headers.get("x-forwarded-for", "").split(",")[0]
            or (request.client.host if request.client else "-")
        )
        try:
            response = await call_next(request)
            duration_ms = (time.perf_counter() - started) * 1000
            logger.info(
                f'{client_host} "{request.method} {request.url.path}" '
                f"status={response.status_code} duration_ms={duration_ms:.2f}"
            )
            return response
        except Exception:
            duration_ms = (time.perf_counter() - started) * 1000
            logger.error(
                f'{client_host} "{request.method} {request.url.path}" '
                f"unhandled_exception duration_ms={duration_ms:.2f}"
            )
            raise


app.add_middleware(RequestLogMiddleware)

app.add_middleware(SecurityHeadersMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://127.0.0.1:8080"],
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)

app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=["localhost", "127.0.0.1", "0.0.0.0"],
)


@app.get("/ai-worker/v1/health")
def health_check(request: Request):
    from datetime import datetime, timezone
    
    with tracer.start_as_current_span("health_check_manual"):
        return {
            "status": "healthy",
            "service": "tracepilot-worker",
            "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        }