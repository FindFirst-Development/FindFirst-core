"use client";
import { Navbar, Button, ButtonGroup, Container } from "react-bootstrap";
import { useRouter } from "next/navigation";
import authService, { AuthStatus } from "@services/auth.service";
import useAuth from "@components/UseAuth";
import LightDarkToggle from "./LightDarkToggle";
import ImportModal from "@components/Import/ImportModal";
import Export from "./Export";
import Image from "next/image";
import Searchbar from "./Searchbar";
import navbarView from "styles/navbar.module.scss";
import AccountModal from "./AccountModal";

const GlobalNavbar: React.FC = () => {
  const userAuth = useAuth();
  const user = authService.getUser();
  const router = useRouter();
  function authButton() {
    if (userAuth == AuthStatus.Unauthorized || userAuth === undefined) {
      return (
        <ButtonGroup>
          <Button
            variant="secondary"
            onClick={() => router.push("/account/login")}
          >
            Login
          </Button>
          <Button
            variant="secondary"
            onClick={() => router.push("/account/signup")}
          >
            Signup
          </Button>
        </ButtonGroup>
      );
    } else {
      return (
        <Button
          className="d-inline-block"
          variant="secondary"
          onClick={handleLogoutClick}
        >
          Logout
        </Button>
      );
    }
  }
  const handleLogoutClick = () => {
    authService.logout();
    router.push("/account/login");
  };

  return (
    <Navbar
      expand="md"
      fixed="bottom"
      style={{
        borderTop: "3px solid",
        height: "auto",
        minHeight: "60px",
      }}
      className="bg-body-tertiary"
    >
      <Container className={navbarView.navContainer}>
        <Navbar.Brand
          onClick={() => router.push("/")}
          className={` ${navbarView.navBrand}`}
        >
          <Image
            src="/basic-f-v2-dark-mode-v2-fav.png"
            width="38"
            height="30"
            className="d-inline-block align-top"
            alt="FindFirst Logo"
          />
        </Navbar.Brand>

        {/* Search bar stays visible always */}
        {userAuth === AuthStatus.Authorized ? <Searchbar /> : null}

        {/* Toggle button for collapsed menu */}
        <Navbar.Toggle aria-controls="navbar-collapse" className="ms-auto" />

        {/* Collapsible content */}
        <Navbar.Collapse id="navbar-collapse">
          <div className={`btn-group ms-auto ${navbarView.navBtns}`}>
            {userAuth === AuthStatus.Authorized ? (
              <ImportModal
                file={undefined}
                show={false}
                data-testid="import-modal"
              />
            ) : null}
            {userAuth === AuthStatus.Authorized ? (
              <Export data-testid="export-component" />
            ) : null}
            <LightDarkToggle />
            {userAuth === AuthStatus.Authorized &&
              (user?.profileImage && user?.profileImage.trim() !== "" ? (
                <Image
                  src={`/api/user/avatar?userId=${user.id}`}
                  alt="Profile"
                  width={39}
                  height={39}
                />
              ) : (
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="39"
                  height="39"
                  fill="currentColor"
                  className="bi bi-file-person-fill"
                  viewBox="0 0 16 16"
                >
                  <path d="M12 0H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2m-1 7a3 3 0 1 1-6 0 3 3 0 0 1 6 0m-3 4c2.623 0 4.146.826 5 1.755V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-1.245C3.854 11.825 5.377 11 8 11" />
                </svg>
              ))}
            {authButton()}
            <AccountModal />
          </div>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default GlobalNavbar;
