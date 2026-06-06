import { useSelectedTags } from "@/contexts/SelectedContext";
import BookmarkCard from "@components/Bookmark/BookmarkCard";
import { useBookmarks } from "@/contexts/BookmarkContext";
import Bookmark from "@type/Bookmarks/Bookmark";
import { useTags } from "@/contexts/TagContext";
import { TagWithCnt } from "@type/Bookmarks/Tag";
import cardView from "styles/cardView.module.scss";


// Bookmark group composed of Bookmarks.
export default function BookmarkCardsView() {
  const bookmarks = useBookmarks();
  const { selected } = useSelectedTags();
  const tags = useTags();
  const filterMap = new Map<number, Bookmark>();

  function addIfNotInList(ids: number[]) {
    ids.forEach((bkmkId) => {
      const fnd = bookmarks.fetchedBookmarks.find((v) => v.id == bkmkId);
      if (fnd) filterMap.set(bkmkId, fnd);
    });
  }

  function filterBookmarks(bookmarks: Bookmark[]): Bookmark[] {
    if (selected.length == 0) {
      return bookmarks;
    } else {
      selected.forEach((selectedTag) => {
        // get tagId of each selected
        const selectedBkmks = tags.get(selectedTag)?.associatedBkmks.map((v) => v.id);
        if (selectedBkmks) {
          addIfNotInList(selectedBkmks);
        }
      });
      return [...filterMap.values()];
    }
  }

  return (
    <div>
      {!bookmarks.loading ? (
        <div
          className={`${cardView.content} row`}
          style={{ marginBottom: "60px" }}
        >
          {filterBookmarks(bookmarks.fetchedBookmarks).map((b) => {
            return (
              <div
                key={b.id}
                className="col-12 col-sm-6 col-md-6 col-lg-4 col-xl-3 col-xxl-2"
              >
                <BookmarkCard bookmark={b} />
              </div>
            );
          })}
        </div>
      ) : (
        <p>loading</p>
      )}
    </div>
  );
}
