"use client";
import React, { useEffect, useState } from "react";
import { Badge, ListGroup } from "react-bootstrap";
import { useTags } from "@/contexts/TagContext";
import useAuth from "@components/UseAuth";
import api from "@/api/Api";
import { TagReqPayload, TagWithCnt, UNTAGGED } from "@/types/Bookmarks/Tag";
import itemStyle from "./tag-list-item.module.scss";
import menuStyle from "styles/tag.module.scss";
import { useSelectedTags } from "@/contexts/SelectedContext";
import { useScreenSize } from "@/contexts/ScreenSizeContext";
import { useBookmarks } from "@/contexts/BookmarkContext";

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

  const noTags = bookmark.fetchedBookmarks.filter(b => b.tags.length === 0); useEffect(() => {
    if (userAuth && tagMap.size == 0 && !bookmark.loading) {
      setLoading(true); api.getAllTags()
        .then((results) => {
          const tags: TagReqPayload[] = results.data as TagReqPayload[];
          for (let tag of tags) {
            const twc: TagWithCnt = {
              id: tag.id,
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
      if (hasUntaggedBookmark) {
        const untaggeds: TagWithCnt = {
          id: -1,
          title: UNTAGGED,
          count: noTags.length,
          associatedBkmks: noTags,
        };
        tagMap.set(-1, untaggeds);
      }
    }

  }, [tagMap, userAuth, hasUntaggedBookmark, bookmark.loading]);

  function selectTag(event: any, id: number) {
    const idx = selected.indexOf(id);
    if (idx >= 0) {
      const updated = [...selected];
      updated.splice(idx, 1);
      setSelected(updated);
      event.target.classList.remove(itemStyle.on);
    } else {
      setSelected([...selected, id]);
      event.target.classList.add(itemStyle.on);
    }
  }

  let groupItems: any = [];

  tagMap.forEach((tagCnt) => {
    if (tagCnt.count > 0) {
      groupItems.push(
        <ListGroup.Item
          key={`${tagCnt.title}-item`}
          className={`${itemStyle.item}`}
        >
          <button
            onClick={(event) => selectTag(event, tagCnt.id!)}
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
    }
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
