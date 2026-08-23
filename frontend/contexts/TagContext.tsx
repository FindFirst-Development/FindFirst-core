import { Dispatch, createContext, useContext, useReducer } from "react";
import TagAction from "@/types/Bookmarks/TagAction";
import { TagWithCnt } from "@/types/Bookmarks/Tag";
import Bookmark from "@type/Bookmarks/Bookmark";

const initialTagCnts: Map<number, TagWithCnt> = new Map();

export const TagsCntContext = createContext<Map<number, TagWithCnt>>(
  new Map<number, TagWithCnt>(),
);
export const TagsCntDispatchContext = createContext<Dispatch<TagAction>>(
  () => { },
);

/**
 * This code looks complex and it kinda is, its layers of React gobbley-gook.
 * The short version of it, is that this provider uses 
 * TagsCntDispatchContext context and declares that anyone that uses
 * TagsCntDispatchContext will get the provider value of dispatch. 
 * Meaning that the tagCntReducer will be available to increment those new bookmarks for you.
 *
 * You might say to yourself well the TagsCntDispatchContext looks like its just a context.
 * Yes, you're right but we then do the magic in the JSX below to wire it in.
 */
export function TagCntProvider({
  children,
}: {
  readonly children: React.ReactNode;
}) {
  const [tags, dispatch] = useReducer(tagCntReducer, initialTagCnts);

  return (
    <TagsCntContext.Provider value={tags}>
      <TagsCntDispatchContext.Provider value={dispatch}>
        {children}
      </TagsCntDispatchContext.Provider>
    </TagsCntContext.Provider>
  );
}

/**
  * tagMap: is the initial state, in this case an empty map.
  * action: add, delete.
  */
function tagCntReducer(tagMap: Map<number, TagWithCnt>, action: TagAction) {
  const tagId = action.id;
  const tagCnt: TagWithCnt | undefined = tagMap.get(action.id);
  const untaggeds: TagWithCnt | undefined = tagMap.get(-1);
  // create a deep copy of the existing.
  const newTagMap = new Map(tagMap);

  const bkmk = action.bookmark;

  switch (action.type) {
    case "add": {
      if (tagCnt !== undefined) {
        addBkmkToTag(tagCnt, action.bookmark);
        newTagMap.set(tagId, {
          id: tagId,
          title: tagCnt.title,
          count: tagCnt.count + 1,
          associatedBkmks: tagCnt.associatedBkmks,
        });
      } else {
        newTagMap.set(tagId, {
          id: tagId,
          title: action.title,
          count: 1,
          associatedBkmks: [bkmk],
        });
      }
      return newTagMap;
    }
    case "delete": {
      if (tagCnt && tagCnt.count > 1) {
        newTagMap.set(tagId, {
          id: tagId,
          title: tagCnt.title,
          count: tagCnt.count - 1,
          associatedBkmks: remBkmkFrmTag(tagCnt, action.bookmark),
        });
      } else {
        // delete the tag from the map if its the last one
        newTagMap.delete(action.id);
      }
      return newTagMap;
    }
    default: {
      throw Error("Unknown action: " + action.type);
    }
  }
}

function addBkmkToTag(tagCnt: TagWithCnt, bkmk: Bookmark) {
  let fnd = false;
  if (tagCnt) {
    const associatedBkmks = tagCnt.associatedBkmks;
    for (let associatedBkmk of associatedBkmks) {
      if (associatedBkmk.title == bkmk.title) {
        fnd = true;
        break;
      }
    }
  }
  if (!fnd) {
    tagCnt?.associatedBkmks.push({ ...bkmk });
  }
}

function remBkmkFrmTag(tagCnt: TagWithCnt, bkmk: Bookmark) {
  return tagCnt.associatedBkmks.filter((b) => b.title !== bkmk.title);
}

export function useTags() {
  return useContext(TagsCntContext);
}

export function useTagsDispatch() {
  return useContext(TagsCntDispatchContext);
}

