"use client";
import React, { useEffect, useState } from "react";
import { Badge, ListGroup } from "react-bootstrap";
import { useTags } from "@/contexts/TagContext";
import useAuth from "@components/UseAuth";
import api from "@/api/Api";
import { TagReqPayload, TagWithCnt } from "@/types/Bookmarks/Tag";
import itemStyle from "./tag-list-item.module.scss";
import menuStyle from "styles/tag.module.scss";
import { useSelectedTags } from "@/contexts/SelectedContext";
import { useScreenSize } from "@/contexts/ScreenSizeContext";
import { useBookmarks } from "@/contexts/BookmarkContext"; // import bookmarks for the "untagged" tag

const TagList = () => {
  const userAuth = useAuth();
  const tagMap = useTags();
  const [loading, setLoading] = useState(false);
  const { selected, setSelected } = useSelectedTags();
  const isPC = useScreenSize();
  const bookmark = useBookmarks();
  // check if at least one bookmark has no tags
  const hasUntaggedBookmark = bookmark.fetchedBookmarks.some(
    (t) => t.tags.length === 0,
  );
  useEffect(() => {
    if (userAuth && tagMap.size == 0) {
      setLoading(true);
      api
        .getAllTags()
        .then((results) => {
          const tags: TagReqPayload[] = results.data as TagReqPayload[];
          for (let tag of tags) {
            const twc: TagWithCnt = {
              title: tag.title,
              count: tag.bookmarks.length,
              associatedBkmks: tag.bookmarks,
            };
            tagMap.set(tag.id, twc);
          }
        })
        .then(() => {
          setLoading(false);
        });
    }
  }, [tagMap, userAuth]);

  function selectTag(event: any, title: string) {
    const idx = selected.indexOf(title);
    if (idx >= 0) {
      const updated = [...selected];
      updated.splice(idx, 1);
      setSelected(updated);
      event.target.classList.remove(itemStyle.on);
    } else {
      setSelected([...selected, title]);
      event.target.classList.add(itemStyle.on);
    }
  }

  let groupItems: any = [];
  // adds "untagged" tag to the tag list
  if (hasUntaggedBookmark) {
    groupItems.push(
      <ListGroup.Item key={"untagged-item"} className={`${itemStyle.item}`}>
        <button
          onClick={(event) => selectTag(event, "untagged")}
          className={`d-flex btn ${itemStyle.btn} justify-content-between align-items-start`}
        >
          untagged
          <Badge bg="primary" pill>
            {bookmark.fetchedBookmarks.filter((b) => b.tags.length == 0).length}
          </Badge>
        </button>
      </ListGroup.Item>,
    );
  }
  tagMap.forEach((tagCnt) => {
    groupItems.push(
      <ListGroup.Item
        key={`${tagCnt.title}-item`}
        className={`${itemStyle.item}`}
      >
        <button
          onClick={(event) => selectTag(event, tagCnt.title)}
          data-testid={`${tagCnt.title}-list-item`}
          key={`${tagCnt.title}-list-item`}
          className={`d-flex btn ${itemStyle.btn} justify-content-between align-items-start`}
        >
          {tagCnt.title}
          <Badge bg="primary" pill>
            <div
              data-testid={`${tagCnt.title}-list-item-cnt`}
              key={`${tagCnt.title}-list-item-badge`}
            >
              {tagCnt.count}
            </div>
          </Badge>
        </button>
      </ListGroup.Item>,
    );
  });

  if (groupItems.length == 0) {
    groupItems.push(
      <ListGroup.Item
        key="no-items"
        className="h-10 d-flex justify-content-between align-items-start"
      >
        Tag List
        <Badge bg="primary" pill>
          Count
        </Badge>
      </ListGroup.Item>,
    );
  }

  return (
    isPC && (
      <div className={menuStyle.tagList}>
        {!loading ? <ListGroup>{groupItems || []}</ListGroup> : null}
      </div>
    )
  );
};

export default TagList;
