/// <reference types="vitest" />
import { vi, beforeEach, afterEach, describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import GlobalNavbar from "@components/Navbar/Navbar";
import { useRouter } from "next/navigation";
import useAuth from "@components/UseAuth";
import authService, { AuthStatus } from "@services/auth.service";

const user = userEvent.setup();

// Mock the necessary modules
vi.mock("next/navigation", () => ({
  useRouter: vi.fn(),
}));

vi.mock("@components/UseAuth", () => ({
  default: vi.fn(),
}));

vi.mock("next/image", () => ({
  default: (props: any) => {
    // eslint-disable-next-line jsx-a11y/alt-text, @next/next/no-img-element
    return <img {...props} />;
  },
}));

vi.mock(
  "@services/auth.service",
  async (
    importOriginal: () => Promise<typeof import("@services/auth.service")>,
  ) => {
    const actual = await importOriginal();
    return {
      __esModule: true,
      ...actual,
      default: {
        ...actual.default,
        logout: vi.fn(),
        getUser: vi.fn().mockImplementation(() => {
          return {
            id: 1,
            username: "test",
            refreshToken: "test",
            profileImage: "",
          };
        }),
        AuthStatus: {
          Unauthorized: "Unauthorized",
          Authorized: "Authorized",
        },
      },
    };
  },
);

type MockedFunction<T extends (...args: any[]) => any> = ReturnType<
  typeof vi.fn
> & { mockReturnValue: (value: ReturnType<T>) => void };

describe("GlobalNavbar", () => {
  const mockPush = vi.fn();
  const mockLogout = vi.fn();

  beforeEach(() => {
    (useRouter as MockedFunction<typeof useRouter>).mockReturnValue({
      push: mockPush,
    });
    (
      authService.logout as MockedFunction<typeof authService.logout>
    ).mockImplementation(mockLogout);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders the navbar with brand logo", () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Unauthorized,
    );
    render(<GlobalNavbar />);
    const brandLogo = screen.getByAltText("FindFirst Logo");
    expect(brandLogo).toBeInTheDocument();
  });

  it("renders login and signup buttons when user is unauthorized", () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Unauthorized,
    );
    render(<GlobalNavbar />);
    const loginButton = screen.getByText("Login");
    const signupButton = screen.getByText("Signup");
    expect(loginButton).toBeInTheDocument();
    expect(signupButton).toBeInTheDocument();
  });

  it("renders logout button when user is authorized", () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Authorized,
    );
    render(<GlobalNavbar />);
    const logoutButton = screen.getByText("Logout");
    expect(logoutButton).toBeInTheDocument();
  });

  it("calls router.push on login button click", async () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Unauthorized,
    );
    render(<GlobalNavbar />);
    const loginButton = screen.getByText("Login");
    await user.click(loginButton);
    expect(mockPush).toHaveBeenCalledWith("/account/login");
  });

  it("calls router.push on signup button click", async () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Unauthorized,
    );
    render(<GlobalNavbar />);
    const signupButton = screen.getByText("Signup");
    await user.click(signupButton);
    expect(mockPush).toHaveBeenCalledWith("/account/signup");
  });

  it("calls authService.logout and router.push on logout button click", async () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Authorized,
    );
    render(<GlobalNavbar />);
    const logoutButton = screen.getByText("Logout");
    await user.click(logoutButton);
    expect(authService.logout).toHaveBeenCalled();
    expect(mockPush).toHaveBeenCalledWith("/account/login");
  });

  it("renders ImportModal and Export components when user is authorized", () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Authorized,
    );
    render(<GlobalNavbar />);
    const importModal = screen.getByTestId("import-modal"); // Test ID for ImportModal
    const exportComponent = screen.getByTestId("export-component"); // Test ID for Export
    expect(importModal).toBeInTheDocument();
    expect(exportComponent).toBeInTheDocument();
  });

  it("does not render ImportModal and Export components when user is unauthorized", () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Unauthorized,
    );
    render(<GlobalNavbar />);
    const importModal = screen.queryByTestId("import-modal"); // Test ID for ImportModal
    const exportComponent = screen.queryByTestId("export-component"); // Test ID for Export
    expect(importModal).toBeNull();
    expect(exportComponent).toBeNull();
  });

  it("calls router.push on brand logo click", async () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Unauthorized,
    );
    render(<GlobalNavbar />);
    const brandLogo = screen.getByAltText("FindFirst Logo");
    await user.click(brandLogo);
    expect(mockPush).toHaveBeenCalledWith("/");
  });

  it("renders default avatar when authorized and no profileImage", () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Authorized,
    );

    render(<GlobalNavbar />);
    const avatar = document.querySelector(".bi-file-person-fill");
    expect(avatar).toBeInTheDocument();
  });

  it("renders user avatar when authorized and profileImage exists", () => {
    (useAuth as MockedFunction<typeof useAuth>).mockReturnValue(
      AuthStatus.Authorized,
    );

    (authService.getUser as ReturnType<typeof vi.fn>).mockReturnValue({
      id: 1,
      username: "test",
      refreshToken: "test",
      profileImage: "avatars/1.png",
    });

    // In your test setup file or at the top of the test file

    render(<GlobalNavbar />);
    const avatar = screen.getByAltText("Profile") as HTMLImageElement;
    expect(avatar).toBeInTheDocument();
    expect(avatar.src).toContain("/api/user/avatar?userId=1");
  });
});
