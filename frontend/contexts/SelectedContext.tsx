import {
  Dispatch,
  SetStateAction,
  createContext,
  useContext,
  useMemo,
  useState,
} from "react";

export interface TagProvider {
  selected: number[];
  setSelected: Dispatch<SetStateAction<number[]>>;
}

export const SelectedTagContext = createContext<TagProvider>({
  selected: [],
  setSelected: () => { },
});

export function SelectedTagProvider({
  children,
}: {
  readonly children: React.ReactNode;
}) {
  const [selected, setSelected] = useState<number[]>([]);

  const selectedMemo = useMemo(() => ({ selected, setSelected }), [selected]);

  return (
    <SelectedTagContext.Provider value={selectedMemo}>
      {children}
    </SelectedTagContext.Provider>
  );
}

export function useSelectedTags() {
  return useContext(SelectedTagContext);
}
