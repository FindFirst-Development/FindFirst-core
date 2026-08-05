package dev.findfirst.core.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.findfirst.core.dto.AddBkmkReq;
import dev.findfirst.core.dto.BookmarkDTO;
import dev.findfirst.core.model.jdbc.BookmarkJDBC;
import dev.findfirst.core.repository.jdbc.BookmarkJDBCRepository;
import dev.findfirst.core.repository.jdbc.BookmarkTagRepository;
import dev.findfirst.security.userauth.context.UserContext;
import dev.findfirst.users.model.user.User;
import dev.findfirst.users.service.UserManagementService;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

  @InjectMocks
  private BookmarkService bookmarkService;
  @Mock
  private BookmarkJDBCRepository bookmarkRepository;
  @Mock
  private BookmarkTagRepository bookmarkTagRepository;
  @Mock
  private TagService tagService;
  @Mock
  private WebCheckService webCheckService;
  @Mock
  private ScreenshotManager sManager;
  @Mock
  private UserContext uContext;
  @Mock
  private UserManagementService userService;
  @Mock
  private TypesenseService typesense;

  private static final int USER_ID = 1;
  private static final long BOOKMARK_ID = 42L;
  private static final String URL = "https://a-website.com/some/page";
  private static final String SCRAPED_TITLE = "A Website - Some Page";
  private static final String SCREENSHOT_URL = "http://example.com/screenshot.png";

  /**
   * Tests that addMissingScreenShotUrlToBookMarks method of BookmarkService class add screenshots
   * urls to scrapable bookmarks.
   *
   * @param scrapable
   */
  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = {true, false})
  void addMissingScreenShotUrlToBookMarksTests(Boolean scrapable) {
    String screenshotUrl = "http://example.com/3";
    BookmarkJDBC bookmark = new BookmarkJDBC();
    bookmark.setScrapable(scrapable);
    List<BookmarkJDBC> list = new ArrayList<>();
    list.add(bookmark);
    if (Boolean.TRUE.equals(scrapable)) {
      when(sManager.getScreenshot(any())).thenReturn(Optional.of(screenshotUrl));
      when(bookmarkRepository.saveAll(any())).thenReturn(list);
    }
    when(bookmarkRepository.findBookmarksWithEmptyOrBlankScreenShotUrl()).thenReturn(list);
    bookmarkService.addMissingScreenShotUrlToBookMarks();
    if (Boolean.TRUE.equals(scrapable)) {
      Assertions.assertEquals(screenshotUrl, bookmark.getScreenshotUrl(), "URL is not updated");
      verify(sManager, times(1)).getScreenshot(any());
    } else {
      Assertions.assertNull(bookmark.getScreenshotUrl(), "URL is updated");
      verify(sManager, never()).getScreenshot(any());
    }
    verify(bookmarkRepository, times(1)).findBookmarksWithEmptyOrBlankScreenShotUrl();
    verify(bookmarkRepository, times(1)).saveAll(list);
  }

  /**
   * A scrapable request without a title of its own falls back to the scraped title. Clients default
   * the title to the url, so that counts as no title at all.
   *
   * @param requestedTitle the title as it arrived on the request.
   */
  @ParameterizedTest
  @NullSource
  @EmptySource
  @ValueSource(strings = {"   ", URL})
  @DisplayName("addBookmark() -> scraped title is used when no title was provided")
  void addBookmarkUsesScrapedTitleWhenNoneProvided(String requestedTitle) throws Exception {
    var bkmk = addScrapableBookmark(requestedTitle, pageTitled(SCRAPED_TITLE), null);

    Assertions.assertEquals(SCRAPED_TITLE, bkmk.title());
  }

  /**
   * An explicitly given title is the user's choice and must survive the scrape, otherwise a
   * scrapable bookmark could never be given a custom title.
   */
  @Test
  @DisplayName("addBookmark() -> explicit title wins over the scraped title")
  void addBookmarkKeepsExplicitTitleOverScrapedTitle() throws Exception {
    String explicitTitle = "My Own Title";
    var scrapedPage = pageTitled(SCRAPED_TITLE);

    var bkmk = addScrapableBookmark(explicitTitle, scrapedPage, null);

    Assertions.assertEquals(explicitTitle, bkmk.title());
    // The page is still scraped and indexed, only the title is left alone.
    verify(typesense).addText(any(BookmarkJDBC.class), eq(scrapedPage));
    verify(sManager).getScreenshot(URL);
  }

  /**
   * The page has no title of its own, so there is nothing to override the requested title with.
   */
  @Test
  @DisplayName("addBookmark() -> requested title is kept when the page has no title")
  void addBookmarkKeepsRequestedTitleWhenPageHasNoTitle() throws Exception {
    String explicitTitle = "My Own Title";
    var pageWithoutTitle = Jsoup.parse("<html><head></head><body>no title here</body></html>");

    var bkmk = addScrapableBookmark(explicitTitle, pageWithoutTitle, null);

    Assertions.assertEquals(explicitTitle, bkmk.title());
  }

  /**
   * Nothing was given and nothing could be scraped, so the host is used as the title.
   */
  @Test
  @DisplayName("addBookmark() -> host is used when there is no title to be found")
  void addBookmarkFallsBackToHostWhenScrapeFails() throws Exception {
    var bkmk = addScrapableBookmark(null, null, new IOException("could not reach the page"));

    Assertions.assertEquals("a-website.com", bkmk.title());
  }

  /**
   * @param title the title the page carries.
   * @return a document as it would come back from a scrape.
   */
  private static Document pageTitled(String title) {
    return Jsoup.parse("<html><head><title>" + title + "</title></head></html>");
  }

  /**
   * Adds a bookmark for {@link #URL} as a scrapable request.
   *
   * @param requestedTitle the title to send along with the request.
   * @param scraped the document the scrape should return.
   * @param scrapeFailure thrown by the scrape instead of returning a document, may be null.
   * @return the created bookmark.
   */
  private BookmarkDTO addScrapableBookmark(String requestedTitle, Document scraped,
      IOException scrapeFailure) throws Exception {
    when(uContext.getUserId()).thenReturn(USER_ID);
    when(bookmarkRepository.findByUrl(URL, USER_ID)).thenReturn(Optional.empty());
    when(webCheckService.isScrapable(URL)).thenReturn(true);
    when(sManager.getScreenshot(URL)).thenReturn(Optional.of(SCREENSHOT_URL));
    when(userService.getUserById(USER_ID)).thenReturn(Optional.of(new User(USER_ID, "jsmith", "")));
    when(bookmarkRepository.save(any(BookmarkJDBC.class))).thenAnswer(invocation -> {
      BookmarkJDBC toSave = invocation.getArgument(0);
      toSave.setId(BOOKMARK_ID);
      return toSave;
    });

    var connection = mock(Connection.class);
    // Jsoup.connect is static, so the whole class is stubbed for the duration of the call. Any
    // document has to be parsed before that happens.
    try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
      jsoup.when(() -> Jsoup.connect(URL)).thenReturn(connection);
      if (scrapeFailure != null) {
        when(connection.get()).thenThrow(scrapeFailure);
      } else {
        when(connection.get()).thenReturn(scraped);
      }
      return bookmarkService.addBookmark(new AddBkmkReq(requestedTitle, URL, null, true));
    }
  }
}
