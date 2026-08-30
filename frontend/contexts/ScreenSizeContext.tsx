import { createContext, useContext, useEffect, useState } from "react";
import screensize from "@services/screenSize.service";

const ScreenSizeContext = createContext<boolean | null>(null);

export const ScreenSizeProvider = ({
  useMobile = false,
  children,
}: {
  useMobile?: boolean;
  children: React.ReactNode;
}) => {
  const [isPC, setIsPC] = useState<boolean>(() => {
    if (useMobile) {
      return window.innerWidth < 768;
    }
    if (typeof window !== "undefined") {
      return window.innerWidth > 768;
    }
    return true;
  });

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    const handleResize = () => {
      setIsPC(window.innerWidth > 768);
      screensize.calculateBuffer(window.innerHeight, window.innerWidth)
    };

    let timeoutId: ReturnType<typeof setTimeout>;
    const debouncedHandleResize = () => {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(handleResize, 100);
    };

    window.addEventListener("resize", debouncedHandleResize);
    handleResize();

    return () => {
      window.removeEventListener("resize", debouncedHandleResize);
      clearTimeout(timeoutId);
    };
  }, []);

  return (
    <ScreenSizeContext.Provider value={isPC}>
      {isPC && children}
    </ScreenSizeContext.Provider>
  );
};

export const useScreenSize = () => {
  const context = useContext(ScreenSizeContext);
  if (context === null) {
    throw new Error("useScreenSize must be used within a ScreenSizeProvider");
  }
  return context;
};

