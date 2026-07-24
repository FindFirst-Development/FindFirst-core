-- Added indexes to speed up the queries
CREATE INDEX IF NOT EXISTS bookmark_user_url
ON bookmark(user_id, url);

CREATE INDEX IF NOT EXISTS bt_tag_id
ON bookmark_tag(tag_id);

CREATE INDEX IF NOT EXISTS tag_user_id_tag_title
ON tag(user_id, tag_title);
