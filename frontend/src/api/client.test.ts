import { describe, it, expect, beforeEach, vi, afterEach } from "vitest";
import MockAdapter from "axios-mock-adapter";
import axios from "axios";
import { apiClient, setAccessToken, getAccessToken, setUnauthorizedHandler } from "./client";

describe("apiClient interceptors", () => {
  let mock: MockAdapter;
  let globalMock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    globalMock = new MockAdapter(axios);
    setAccessToken(null);
    setUnauthorizedHandler(() => {});
  });

  afterEach(() => {
    mock.restore();
    globalMock.restore();
  });

  it("attaches Authorization header on protected routes when a token is set", async () => {
    setAccessToken("token-123");
    mock.onGet("/audits").reply((config) => {
      expect(config.headers?.Authorization).toBe("Bearer token-123");
      return [200, { ok: true }];
    });

    await apiClient.get("/audits");
  });

  it("does NOT attach Authorization header on public routes even if a token is set", async () => {
    setAccessToken("token-123");
    mock.onPost("/auth/login").reply((config) => {
      expect(config.headers?.Authorization).toBeUndefined();
      return [200, { ok: true }];
    });

    await apiClient.post("/auth/login", {});
  });

  it("on a single 401, refreshes the token once and retries the original request", async () => {
    setAccessToken("expired-token");

    let auditsCallCount = 0;
    mock.onGet("/audits").reply(() => {
      auditsCallCount += 1;
      if (auditsCallCount === 1) {
        return [401, { message: "expired" }];
      }
      return [200, { ok: true }];
    });

    globalMock.onPost(/\/auth\/refresh$/).reply(200, {
      accessToken: "new-token",
      tokenType: "Bearer",
      expiresAt: new Date().toISOString(),
      user: { id: "11111111-1111-4111-8111-111111111111", email: "a@b.com", displayName: null, role: "USER" },
    });

    const res = await apiClient.get("/audits");

    expect(res.status).toBe(200);
    expect(auditsCallCount).toBe(2);
    expect(getAccessToken()).toBe("new-token");
  });

  it("does not retry more than once (avoids infinite retry loop on repeated 401)", async () => {
    setAccessToken("expired-token");
    mock.onGet("/audits").reply(401, { message: "still unauthorized" });
    globalMock.onPost(/\/auth\/refresh$/).reply(200, {
      accessToken: "new-token-that-is-also-rejected",
      tokenType: "Bearer",
      expiresAt: new Date().toISOString(),
      user: { id: "11111111-1111-4111-8111-111111111111", email: "a@b.com", displayName: null, role: "USER" },
    });

    await expect(apiClient.get("/audits")).rejects.toBeDefined();
    // exactly 2 calls to /audits: original + the single retry, never a 3rd
    expect(mock.history.get?.length).toBe(2);
  });

  it("calls the unauthorized handler and clears the token when refresh itself returns 401", async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    setAccessToken("expired-token");

    mock.onGet("/audits").reply(401, { message: "expired" });
    globalMock.onPost(/\/auth\/refresh$/).reply(401, { message: "refresh token invalid" });

    await expect(apiClient.get("/audits")).rejects.toBeDefined();

    expect(onUnauthorized).toHaveBeenCalledTimes(1);
    expect(getAccessToken()).toBeNull();
  });

  it("does not attempt a refresh loop for 401s on public routes (e.g. bad login credentials)", async () => {
    mock.onPost("/auth/login").reply(401, { message: "bad credentials" });

    await expect(apiClient.post("/auth/login", {})).rejects.toBeDefined();
    // refresh endpoint should never have been called
    expect(globalMock.history.post?.length ?? 0).toBe(0);
  });
});