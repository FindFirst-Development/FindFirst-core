import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import Bookmark from "@type/Bookmarks/Bookmark";
import Tag, { TagReqPayload, UNTAGGED } from "@type/Bookmarks/Tag";
import { hitEnter } from "../utilities/fireEvents";

const userEvnt = userEvent.setup();

const cooking: Tag = { id: 1, title: "Cooking" };
const webDev: Tag = { id: 2, title: "web_dev" };

/**
 * Fixtures are built fresh for every test, the components mutate
 * `bookmark.tags` in place when a tag is added or removed.
 */
function cheesecake(tags: Tag[]): Bookmark {
  return {
    id: 1,
    title: "Best Cheesecake Recipe",
    url: "https://sugarspunrun.com/best-cheesecake-recipe/",
    tags: [...tags],
    screenshotUrl: "",
    scrapable: true,
  };
}

function darkMode(tags: Tag[]): Bookmark {
  return {
    id: 2,
    title: "Dark mode guide",
    url: "https://blog.logrocket.com/dark-mode-react-in-depth-guide/",
    tags: [...tags],
    screenshotUrl: "",
    scrapable: true,
  };
}

function chickenParm(tags: Tag[]): Bookmark {
  return {
    id: 3,
    title: "Chicken Parm",
    url: "https://www.foodnetwork.com/recipes/bobby-flay/chicken-parmigiana-recipe-1952359",
    tags: [...tags],
    screenshotUrl: "",
    scrapable: true,
  };
}

/**
 * The tag counts live in a module scoped Map inside TagContext that TagList
 * seeds directly, so the module registry has to be dropped between tests to
 * get a clean sidebar. Everything the test touches (the axios instance, the
 * auth service, the page) therefore has to be imported after the reset so it
 * comes from the same fresh registry as the rendered components.
 */
async function renderPage(bookmarks: Bookmark[], tags: TagReqPayload[]) {
  vi.resetModules();

  const { instance } = await import("@api/Api");
  const authService = (await import("@services/auth.service")).default;
  const { Providers } = await import("@/app/providers");
  const Page = (await import("@/app/page")).default;
  const MockAdapter = (await import("axios-mock-adapter")).default;

  const mock = new MockAdapter(instance);
  mock.onGet("/bookmarks").reply(200, JSON.stringify(bookmarks));
  mock.onGet("/tags").reply(200, JSON.stringify(tags));

  vi.spyOn(authService, "getUser").mockImplementation(() => ({
    username: "jsmith",
    refreshToken: "blahblajhdfh34234",
    id: 1,
  }));
  vi.spyOn(authService, "getAuthorized").mockImplementation(() => 1);

  render(
    <div>
      <Providers>
        <Page />
      </Providers>
    </div>,
  );

  // wait for the bookmarks and the tag sidebar to settle.
  for (const bookmark of bookmarks) {
    await screen.findByTestId(`bookmark-${bookmark.title}`, undefined, {
      timeout: 2000,
    });
  }

  return mock;
}

/** The count badge of a tag in the sidebar, null when the tag is not listed. */
function tagCount(title: string): number | null {
  const badge = screen.queryByTestId(`${title}-list-item-cnt`);
  return badge ? Number(badge.textContent) : null;
}

/** Types a tag into a bookmark card's tag input and submits it. */
async function addTag(bookmarkId: number, tag: string) {
  const input = screen.getByTestId(`bk-${bookmarkId}-tag-input`);
  await userEvnt.type(input, tag);
  hitEnter(input);
  await screen.findByTestId(`bk-${bookmarkId}-tag-${tag}`);
}

/** Clicks a tag pill on a bookmark card, which removes it. */
async function removeTag(bookmarkId: number, tag: string) {
  await userEvnt.click(screen.getByTestId(`bk-${bookmarkId}-tag-${tag}`));
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("Untagged count when tags are added", () => {
  let mock: any;

  beforeEach(async () => {
    // Cheesecake is tagged, the other two are not: untagged == 2.
    mock = await renderPage(
      [cheesecake([cooking]), darkMode([]), chickenParm([])],
      [{ id: cooking.id, title: cooking.title, bookmarks: [] }],
    );
  });

  test("tagging an untagged bookmark decrements the untagged count", async () => {
    expect(tagCount(UNTAGGED)).toBe(2);

    mock.onPost().replyOnce(() => [200, JSON.stringify(webDev)]);
    await addTag(darkMode([]).id, webDev.title);

    await waitFor(() => expect(tagCount(UNTAGGED)).toBe(1));
  });

  test("the untagged count disappears once every bookmark is tagged", async () => {
    expect(tagCount(UNTAGGED)).toBe(2);

    mock.onPost().replyOnce(() => [200, JSON.stringify(webDev)]);
    await addTag(darkMode([]).id, webDev.title);

    mock.onPost().replyOnce(() => [200, JSON.stringify(cooking)]);
    await addTag(chickenParm([]).id, cooking.title);

    await waitFor(() =>
      expect(screen.queryByTestId(`${UNTAGGED}-list-item-cnt`)).toBeNull(),
    );
    expect(screen.queryByText(UNTAGGED)).toBeNull();
  });
});

describe("Untagged count when tags are removed", () => {
  test("removing the last tag of a bookmark increments the untagged count", async () => {
    // Cheesecake is tagged, Dark mode is not: untagged == 1.
    const mock = await renderPage(
      [cheesecake([cooking]), darkMode([])],
      [{ id: cooking.id, title: cooking.title, bookmarks: [cheesecake([])] }],
    );
    expect(tagCount(UNTAGGED)).toBe(1);

    mock.onDelete().replyOnce(() => [200, JSON.stringify(cooking)]);
    await removeTag(cheesecake([]).id, cooking.title);

    await waitFor(() => expect(tagCount(UNTAGGED)).toBe(2));
  });

  test("the untagged count returns when a tag is removed from a fully tagged list", async () => {
    // Every bookmark is tagged, so there is no untagged entry to begin with.
    const mock = await renderPage(
      [cheesecake([cooking]), darkMode([webDev])],
      [
        { id: cooking.id, title: cooking.title, bookmarks: [cheesecake([])] },
        { id: webDev.id, title: webDev.title, bookmarks: [darkMode([])] },
      ],
    );
    expect(screen.queryByTestId(`${UNTAGGED}-list-item-cnt`)).toBeNull();

    mock.onDelete().replyOnce(() => [200, JSON.stringify(cooking)]);
    await removeTag(cheesecake([]).id, cooking.title);

    await waitFor(() => expect(tagCount(UNTAGGED)).toBe(1));
  });
});
