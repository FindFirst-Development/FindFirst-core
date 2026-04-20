import { ReactNode, RefObject, useEffect, useRef, useState } from "react";
import { Card } from "react-bootstrap";
import { useTagsDispatch } from "@/contexts/TagContext";
import Bookmark from "@/types/Bookmarks/Bookmark";
import TagAction from "@/types/Bookmarks/TagAction";
import DeleteModal from "./DeleteModal";
import style from "./bookmarkCard.module.scss";
import { useBookmarkDispatch } from "@/contexts/BookmarkContext";
import BookmarkAction from "@/types/Bookmarks/BookmarkAction";
import Tag from "@/types/Bookmarks/Tag";
import api from "@/api/Api";
import CardBody from "./CardBody";
import { SERVER_URL } from "@type/global";
import TagInput from "./TagInput";

const imgApi = SERVER_URL + "/api/screenshots/";

interface BookmarkProp {
  bookmark: Readonly<Bookmark>;
}

/**
 *  function to add a Tag to a Bookmark
 * @param bookmark bookmark
 * @param tag string title of tag
 * @returns Promise<TagAction> populates bookmark with the tags.
 */
async function addTagToBookmark(
  bookmark: Bookmark,
  tag: string,
): Promise<TagAction> {
  let action: TagAction = {
    type: "add",
    id: -1,
    title: "",
    bookmark: bookmark,
  };

  await api.addBookmarkTag(bookmark?.id, tag).then((response) => {
    // It will always be the last index since it was the last added.
    // let index = response.data.length - 1;
    action.id = response.data.id;
    action.title = response.data.title;
    bookmark.tags.push({ id: action.id, title: action.title });
  });

  return action;
}

function OverlayCard({
  url,
  currentBookmark,
  bookmark,
  inEditMode,
  edit,
  changeEditMode,
}: {
  url: string;
  currentBookmark: RefObject<Bookmark>;
  bookmark: Bookmark;
  inEditMode: boolean;
  edit: RefObject<Bookmark>;
  changeEditMode: () => void;
}): ReactNode {
  return (
    <Card className={style.bookmarkCard}>
      <div className="row g-0">
        <div className="col-4 col-sm-12">
          <Card.Img
            className={`${style.cImg}`}
            src={url + bookmark.screenshotUrl}
            alt="screenshot preview"
          ></Card.Img>
        </div>
        <div className="col-8 col-sm-12">
          <PlainCard
            changeEditMode={changeEditMode}
            bookmark={bookmark}
            currentBookmark={currentBookmark}
            inEditMode={inEditMode}
            edit={edit}
          ></PlainCard>
        </div>
      </div>
      <Card.Link
        style={{ marginLeft: "15px" }}
        target="_blank"
        href={bookmark.url}
      >
        {bookmark.url}
      </Card.Link>
    </Card>
  );
}

function PlainCard({
  currentBookmark,
  bookmark,
  inEditMode,
  edit,
  changeEditMode,
}: {
  currentBookmark: RefObject<Bookmark>;
  bookmark: Bookmark;
  inEditMode: boolean;
  edit: RefObject<Bookmark>;
  changeEditMode: () => void;
}): ReactNode {
  return (
    <CardBody
      bookmark={currentBookmark.current}
      highlight={bookmark.textHighlight}
      inEditMode={inEditMode}
      edit={edit}
      changeEditMode={changeEditMode}
    />
  );
}

export default function BookmarkCard({ bookmark }: Readonly<BookmarkProp>) {
  const dispatch = useTagsDispatch();
  const bkmkDispatch = useBookmarkDispatch();
  const [input, setInput] = useState("");
  const [inEditMode, setInEditMode] = useState(false);
  const [strTags, setStrTags] = useState<string[]>([]);
  const [show, setShow] = useState(false);
  /*
   * Create copies to compare state, its technically shallow but I have no nested properties
   * that are edited in the partial update. For example Tags would be shallow copied.
   * Thus set the before and after, initially they are the same.
   */
  const currentBookmark = useRef<Bookmark>({ ...bookmark });
  const edit = useRef<Bookmark>({ ...bookmark });

  // Set tags on the card from the bookmark json object.
  useEffect(() => {
    if (bookmark) {
      const tagList: string[] = [];
      if (bookmark.tags.length == 0) tagList.push("untagged");
      else {
        bookmark.tags.forEach((tag: Tag) => {
          tagList.push(tag.title);
        });
      }
      setStrTags(tagList);
    }
  }, [bookmark]);

  const handleClose = () => {
    setShow(false);
  };
  const handleShow = () => setShow(true);

  const handleEdits = (inEditMode: boolean) => {
    if (!inEditMode && isChanges(currentBookmark, edit)) {
      sendPatch(edit.current);
      currentBookmark.current = { ...edit.current };
    }
  };

  const sendPatch = (edit: Bookmark) => {
    api.updateBookmark({
      id: edit.id,
      title: edit.title,
      url: edit.url,
      isScrapable: edit.scrapable,
    });
  };

  const isChanges = (
    beforeEdit: RefObject<Bookmark>,
    edit: RefObject<Bookmark>,
  ) => {
    return JSON.stringify(beforeEdit.current) != JSON.stringify(edit.current);
  };

  /**
   * Decrement all the tags associated to this bookmark
   * then remove the bookmark itself.
   * Remove this from the inverse list of tags -> bookmarks when that map is created
   *
   * Consider creating a typescript class to act a handler for
   * the state of bookmarks.
   */
  function deleteBkmk() {
    let action: BookmarkAction = {
      type: "delete",
      bookmarkId: bookmark.id,
      bookmarks: [],
    };
    // delete the bookmark.
    bkmkDispatch(action);

    // decrement the bookmark counters
    bookmark.tags.forEach((tag) => {
      const idx = getIdxFromTitle(tag.title);
      const tagId = bookmark.tags[idx].id;
      // update the sidebar.
      let action: TagAction = {
        type: "delete",
        id: tagId,
        title: "",
        bookmark,
      };
      dispatch(action);
    });
  }

  const onDeleteTag = (idx: number) => {
    const tagId = bookmark.tags[idx].id;
    if (currentBookmark.current) {
      currentBookmark.current.tags = currentBookmark.current.tags.filter(
        (_t, i) => i !== idx,
      );
    }
    api.deleteTagById(bookmark.id, tagId);
    // if there are no tags to display, display "untagged", else display the tags for the bookmark
    let titles =
      currentBookmark.current.tags.length !== 0
        ? currentBookmark.current.tags.map((t) => t.title)
        : ["untagged"]; // just the titles display
    setStrTags(titles);

    // update the sidebar.
    let action: TagAction = {
      type: "delete",
      id: tagId,
      title: "",
      bookmark,
    };
    dispatch(action);
  };

  const onPushTag = (tag: string) =>
    addTagToBookmark(bookmark, tag).then((action) => {
      dispatch(action);
      setStrTags((prev) => {
        const cleaned = prev.filter((t) => t != "untagged");
        return [...cleaned, tag];
      });
      currentBookmark.current.tags.push({ id: action.id, title: action.title });
    });

  function getIdxFromTitle(title: string): number {
    return bookmark.tags.findIndex((t) => t.title == title);
  }

  const onChange = (e: any) => {
    const { value } = e.target;
    setInput(value);
  };

  function Content(): ReactNode {
    return bookmark.screenshotUrl ? (
      <OverlayCard
        url={imgApi}
        changeEditMode={changeEditMode}
        bookmark={bookmark}
        currentBookmark={currentBookmark}
        inEditMode={inEditMode}
        edit={edit}
      />
    ) : (
      <div>
        <PlainCard
          changeEditMode={changeEditMode}
          bookmark={bookmark}
          currentBookmark={currentBookmark}
          inEditMode={inEditMode}
          edit={edit}
        ></PlainCard>
      </div>
    );
  }

  function changeEditMode() {
    setInEditMode(!inEditMode);
    handleEdits(!inEditMode);
  }

  return (
    <div
      data-testid={`bookmark-${bookmark.title}`}
      className={`${style.cardRoot}`}
    >
      <div className={`card ${style.bookmarkCard}`}>
        <div className={style.cardHeader}>
          <button
            type="button"
            aria-label="Close"
            className={`${style.deleteBookmarkIcon} btn-close`}
            onClick={handleShow}
            data-testid={`bk-id-${bookmark.id}-deleteBtn`}
          />
          <button
            onClick={changeEditMode}
            className={`btn ${style.editBookmarkIcon}`}
            data-testid={`${bookmark.id}-edit-btn`}
          >
            <i className="bi bi-pen"></i>
          </button>
        </div>
        <DeleteModal
          show={show}
          handleClose={handleClose}
          deleteBkmk={deleteBkmk}
        />
        <Content />
        <div className={`card-footer ${style.cardFooter}`}>
          <TagInput
            tags={strTags}
            inputValue={input}
            setInputValue={setInput}
            onDeleteTag={onDeleteTag}
            onPushTag={onPushTag}
            testIdPrefix={`bk-${bookmark.id}-`}
          ></TagInput>
        </div>
      </div>
    </div>
  );
}
