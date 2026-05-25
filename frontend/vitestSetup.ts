import { vi } from "vitest";
import "@testing-library/jest-dom";
import { loadEnvConfig } from "@next/env";
import "./app/globals.scss";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.min.css";
loadEnvConfig(process.cwd());

vi.mock("next/font/google", () => ({
  Libre_Baskerville: vi.fn(() => ({ className: "mock-font" })),
  Inter: vi.fn(() => ({ className: "mock-font" })),
}));

vi.mock("next/navigation", () => {
  const actual = vi.importActual("next/navigation");
  return {
    ...actual,
    useRouter: vi.fn(() => ({
      push: vi.fn(),
    })),
    useSearchParams: vi.fn(() => ({
      get: vi.fn(),
    })),
    usePathname: vi.fn(),
  };
});
