import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { FindingsList } from "./SharedReportPage";


describe("FindingsList (untrusted content rendering)", () => {
  const maliciousPayload = `<img src=x onerror="window.__xss=true">`;

  it("renders a malicious title/body/evidence/footer as literal text, not markup", () => {
    render(
      <FindingsList
        items={[
          {
            severity: "HIGH",
            title: maliciousPayload,
            body: maliciousPayload,
            evidence: [maliciousPayload],
            footer: maliciousPayload,
          },
        ]}
      />
    );

   
    const occurrences = screen.getAllByText((_, node) => node?.textContent === maliciousPayload);
    expect(occurrences.length).toBeGreaterThan(0);

    expect(document.querySelector("img")).toBeNull();
    expect((window as unknown as { __xss?: boolean }).__xss).toBeUndefined();
  });

  it("renders nothing for an empty findings list", () => {
    const { container } = render(<FindingsList items={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("omits the footer block when footer is not provided", () => {
    render(
      <FindingsList
        items={[
          { severity: "LOW", title: "t", body: "b", evidence: [] },
        ]}
      />
    );
    expect(screen.queryByText(/Better rule/)).not.toBeInTheDocument();
  });
});