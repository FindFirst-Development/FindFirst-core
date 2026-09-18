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
    };


    let timeoutId: ReturnType<typeof setTimeout>;
    const debouncedHandleResize = () => {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(handleResize, 100);
    };


    const handleScroll = () => {
      screensize.setCardSize(384, 258)
      screensize.maxHeight = document.documentElement.scrollHeight;
      screensize.yPos = document.documentElement.scrollTop;
      console.log("yPos", screensize.yPos);
      console.log("screensize lastBuffer", screensize.lastBuffer)
      screensize.lastBuffer = screensize.calculateBufferY(window.innerHeight, window.innerWidth);
    }
    let timeoutScrollId: ReturnType<typeof setTimeout>;
    const debouncedHandleScroll = () => {
      clearTimeout(timeoutScrollId);
      timeoutScrollId = setTimeout(handleScroll, 100);
    };


    window.addEventListener("resize", debouncedHandleResize);
    window.addEventListener("scroll", debouncedHandleScroll);
    handleResize();
    handleScroll();

    console.log("pageHeight", screensize.pageHeight)

    return () => {
      window.removeEventListener("resize", debouncedHandleResize);
      window.removeEventListener("scroll", debouncedHandleScroll);
      clearTimeout(timeoutId);
      clearTimeout(timeoutScrollId);
    };

  }, []);

  return (
    <ScreenSizeContext.Provider value={isPC}>
      {children}
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

