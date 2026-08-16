package dev.findfirst.core.controller;

import static dev.findfirst.utilities.HttpUtility.getHttpEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import dev.findfirst.core.annotations.IntegrationTest;
import dev.findfirst.core.annotations.MockTypesense;
import dev.findfirst.core.dto.*;
import dev.findfirst.core.exceptions.BookmarkNotFoundException;
import dev.findfirst.core.exceptions.TagNotFoundException;
import dev.findfirst.core.model.jdbc.BookmarkTag;
import dev.findfirst.core.repository.jdbc.BookmarkJDBCRepository;
import dev.findfirst.security.jwt.UserAuthenticationToken;
import dev.findfirst.security.userauth.models.TokenRefreshResponse;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@IntegrationTest
@MockTypesense
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class BookmarkControllerTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

  @Autowired
  BookmarkControllerTest(BookmarkJDBCRepository bookmarkJDBCRepository,
      TestRestTemplate tRestTemplate, WebApplicationContext wContext) {
    this.bookmarkJDBCRepository = bookmarkJDBCRepository;
    this.restTemplate = tRestTemplate;
    this.wac = wContext;
  }

  final BookmarkJDBCRepository bookmarkJDBCRepository;
  final TestRestTemplate restTemplate;
  final WebApplicationContext wac;

  private WebTestClient client;

  @BeforeEach
  void setUp() {
    client = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
  }

  private final String bookmarksURI = "/api/bookmarks";
  private final String bookmarkURI = "/api/bookmark";
  private final String bookmarksPaginationURI = "/api/paginated/bookmarks";

  private List<String> expectedBkmkTitles = List.of("Best Cheesecake Recipe2",
      "Best Cheesecake Recipe3", "Best Cheesecake Recipe4", "Best Cheesecake Recipe5",
      "Best Cheesecake Recipe6", "Ultimate Chocolate Cake", "Top 10 Travel Destinations",
      "Effective Java Programming", "Healthy Meal Plans", "Best Running Shoes 2024",
      "Beginner’s Guide to Investing", "How to Brew the Perfect Coffee");

  @Test
  void containerStarupTest() {
    assertEquals(true, postgres.isRunning());
  }

  @Test
  void shouldFailBecauseNoOneIsAuthenticated() {
    var response = restTemplate.getForEntity(bookmarksURI, BookmarkDTO.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void shouldPassIsAuthenticated() {
    var response = restTemplate.exchange(bookmarksURI, HttpMethod.GET, getHttpEntity(restTemplate),
        BookmarkDTO[].class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void getBookmarkById() {
    var response = restTemplate.exchange(bookmarkURI + "?id=1", HttpMethod.GET,
        getHttpEntity(restTemplate), BookmarkDTO.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var bkmkOpt = Optional.ofNullable(response.getBody());
    var bkmk = bkmkOpt.orElseThrow();
    assertEquals("Best Cheesecake Recipe", bkmk.title());
  }

  @Test
  void getBookmarksFromPage1WithSize6() {
    Integer page = 1;
    Integer size = 6;
    var response =
        restTemplate.exchange(bookmarksPaginationURI + "?page={page}&size={size}", HttpMethod.GET,
            getHttpEntity(restTemplate, "linus", "test"), PaginatedBookmarkRes.class, page, size);

    var bkmkOpt = Optional.ofNullable(response.getBody());
    PaginatedBookmarkRes bkmksResponse = bkmkOpt.orElseThrow();
    List<BookmarkDTO> bkmks = bkmksResponse.bookmarks();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(size, bkmks.size());
    assertEquals(3, bkmksResponse.totalPages());

    // Check that the titles are as expected.
    for (int i = 0; i < bkmks.size(); i++) {
      assertEquals(expectedBkmkTitles.get(i), bkmks.get(i).title());
    }

  }

  @Test
  void pageGreaterThanTotalPage() {
    Integer page = 6;
    Integer size = 6;

    var response = restTemplate.exchange(bookmarksPaginationURI + "?page={page}&size={size}",
        HttpMethod.GET, getHttpEntity(restTemplate), PaginatedBookmarkRes.class, page, size);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

  }

  @Test
  void pageLowerThanOne() {
    Integer page = 0;
    Integer size = 6;

    var response = restTemplate.exchange(bookmarksPaginationURI + "?page={page}&size={size}",
        HttpMethod.GET, getHttpEntity(restTemplate), PaginatedBookmarkRes.class, page, size);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void sizeLowerThanMinValue() {
    Integer page = 1;
    Integer size = 2;

    var response = restTemplate.exchange(bookmarksPaginationURI + "?page={page}&size={size}",
        HttpMethod.GET, getHttpEntity(restTemplate), PaginatedBookmarkRes.class, page, size);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void sizeGreaterThanMaxValue() {
    Integer page = 1;
    Integer size = 50;

    var response = restTemplate.exchange(bookmarksPaginationURI + "?page={page}&size={size}",
        HttpMethod.GET, getHttpEntity(restTemplate), PaginatedBookmarkRes.class, page, size);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void addBookmark() {
    // Test with Scrapping (default behavior)
    var ent = getHttpEntity(restTemplate,
        new AddBkmkReq("Stack Overflow", "https://stackoverflow.com", List.of(), true));
    var response = restTemplate.exchange(bookmarkURI, HttpMethod.POST, ent, BookmarkDTO.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var bkmk = Optional.ofNullable(response.getBody());
    assertEquals("Stack Overflow", bkmk.orElseThrow().title());

    // Test with scraping (but not allowed as per robot.txt)
    var entFailScrape = getHttpEntity(restTemplate,
        new AddBkmkReq("Facebook", "https://facebook.com", List.of(), true));
    var responseFailScrape =
        restTemplate.exchange(bookmarkURI, HttpMethod.POST, entFailScrape, BookmarkDTO.class);
    assertEquals(HttpStatus.OK, responseFailScrape.getStatusCode());
    var bkmkFailScrape = Optional.ofNullable(responseFailScrape.getBody());
    assertEquals("Facebook", bkmkFailScrape.orElseThrow().title());

    // Test without scrapping
    var entNoScrape = getHttpEntity(restTemplate,
        new AddBkmkReq("Wikipedia", "https://wikipedia.org", List.of(), false));
    var noScrapeResponse =
        restTemplate.exchange(bookmarkURI, HttpMethod.POST, entNoScrape, BookmarkDTO.class);
    assertEquals(HttpStatus.OK, noScrapeResponse.getStatusCode());

    var noScrapeBkmk = Optional.ofNullable(noScrapeResponse.getBody());
    assertEquals("", noScrapeBkmk.orElseThrow().screenshotUrl());
  }

  @Test
  void updateBookmark() {
    this.restTemplate.getRestTemplate()
        .setRequestFactory(new HttpComponentsClientHttpRequestFactory());

    String oldTitle = "Dark mode guide";
    String newTitle = "Dark MODE";
    String url = "https://blog.logrocket.com/dark-mode-react-in-depth-guide/#what-dark-mode";

    var ent = getHttpEntity(restTemplate, new AddBkmkReq(oldTitle, url, List.of(), true));
    var response = restTemplate.exchange(bookmarkURI, HttpMethod.POST, ent, BookmarkDTO.class);
    var id = response.getBody().id();

    var noChangeReq = restTemplate.exchange(bookmarkURI, HttpMethod.PATCH,
        getHttpEntity(restTemplate, new UpdateBookmarkReq(id, null, null, null)),
        BookmarkDTO.class);
    var bkmkDTO = noChangeReq.getBody();

    // The title was given on the add request, so it is kept even though the url was
    // scraped.
    assertEquals(oldTitle, bkmkDTO.title());
    assertEquals(url, bkmkDTO.url());

    var updateReq = restTemplate.exchange(bookmarkURI, HttpMethod.PATCH,
        getHttpEntity(restTemplate, new UpdateBookmarkReq(2, newTitle, url + "newpath", false)),
        BookmarkDTO.class);

    bkmkDTO = updateReq.getBody();
    assertEquals(newTitle, bkmkDTO.title());
    assertEquals(false, bkmkDTO.scrapable());
    assertEquals(url + "newpath", bkmkDTO.url());

  }

  @Test
  void addAllBookmarks() {
    saveBookmarks(new AddBkmkReq("", "https://example.com", List.of(), true),
        new AddBkmkReq("goolgleExample", "https://google.com", List.of(), true));
  }

  @Test
  void exportAllUsersBookmarks() {
    var response = restTemplate.exchange(bookmarksURI + "/export", HttpMethod.GET,
        getHttpEntity(restTemplate), byte[].class);
    String docStr = new String(response.getBody(), StandardCharsets.UTF_8);
    assertNotNull(docStr);
  }

  @Test
  // should test that all of JSmith's bookmarks are deleted but no one else's
  // were removed.
  void deleteAllBookmarks() {
    var response = restTemplate.exchange(bookmarksURI, HttpMethod.DELETE,
        getHttpEntity(restTemplate), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(bookmarkJDBCRepository.count() > 0);

    var bkmks = restTemplate.exchange(bookmarksURI, HttpMethod.GET, getHttpEntity(restTemplate),
        BookmarkDTO[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    var bkmkOpt = Optional.ofNullable(bkmks.getBody());

    assertEquals(0, bkmkOpt.orElseThrow().length);
  }

  @Test
  void deleteTagFromBookmarkByTagTitle() {
    var bkmkResp = saveBookmarks(new AddBkmkReq("yahoo", "https://yahoo.com", List.of(), true));
    var bkmk = bkmkResp.get(0);

    // Add web_dev to bookmark
    var ent = getHttpEntity(restTemplate);
    restTemplate.exchange(bookmarkURI + "/{bookmarkID}/tag?tag={title}", HttpMethod.POST, ent,
        TagDTO.class, bkmk.id(), "web_dev");

    // Add design tag to bookmark.
    ent = getHttpEntity(restTemplate);
    restTemplate.exchange(bookmarkURI + "/{bookmarkID}/tag?tag={title}", HttpMethod.POST, ent,
        TagDTO.class, bkmk.id(), "design");

    var response = restTemplate.exchange(bookmarkURI + "?id={id}", HttpMethod.GET,
        getHttpEntity(restTemplate), BookmarkDTO.class, bkmk.id());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete first tag.
    restTemplate.exchange(bookmarkURI + "/{bookmarkID}/tag?tag={tagName}", HttpMethod.DELETE,
        getHttpEntity(restTemplate), TagDTO.class, bkmk.id(), "web_dev");

    response = restTemplate.exchange(bookmarkURI + "?id={id}", HttpMethod.GET,
        getHttpEntity(restTemplate), BookmarkDTO.class, bkmk.id());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var bkmkOpt = Optional.ofNullable(response.getBody());
    var bkmkDto = bkmkOpt.orElseThrow();
    assertEquals(1, bkmkDto.tags().size());
    assertFalse(bkmk.tags().stream().anyMatch(t -> t.title().equals("web_dev")));
  }

  @Test
  void deleteTagFromBookmarkById() {
    var bkmkResp = saveBookmarks(new AddBkmkReq("X", "https://x.com", List.of(), true));
    var bkmk = bkmkResp.get(0);

    // Add Tag web_dev
    var ent = getHttpEntity(restTemplate);
    restTemplate.exchange(bookmarkURI + "/{bookmarkID}/tag?tag={title}", HttpMethod.POST, ent,
        TagDTO.class, bkmk.id(), "web_dev");

    // Add Tag design
    // Store tag response to delete the tag next
    ent = getHttpEntity(restTemplate);
    var tagOpt =
        Optional.ofNullable(restTemplate.exchange(bookmarkURI + "/{bookmarkID}/tag?tag={title}",
            HttpMethod.POST, ent, TagDTO.class, bkmk.id(), "design").getBody());
    long tagId = tagOpt.orElseThrow().id();

    // Delete by the id.
    restTemplate.exchange(bookmarkURI + "/{bookmarkID}/tagId?tagId={id}", HttpMethod.DELETE,
        getHttpEntity(restTemplate), BookmarkTag.class, bkmk.id(), tagId);

    // See that one tag remains on the bookmark
    var response = restTemplate.exchange(bookmarkURI + "?id={id}", HttpMethod.GET,
        getHttpEntity(restTemplate), BookmarkDTO.class, bkmk.id());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var bkmkOptJDBC = Optional.ofNullable(response.getBody());
    var bkmkDto = bkmkOptJDBC.orElseThrow();
    assertFalse(bkmkDto.tags().stream().anyMatch(t -> t.id() == tagId));
  }

  @Test
  void tagNotFoundOnAddRequest() {
    var ent = getHttpEntity(restTemplate,
        new AddBkmkReq("duckduckgo privacy", "https://spreadprivacy.com/", List.of(20L), true));

    var bkmkResp = restTemplate.exchange(bookmarkURI, HttpMethod.POST, ent, String.class);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, bkmkResp.getStatusCode());
    assertEquals(new TagNotFoundException().getMessage(), bkmkResp.getBody());
  }

  @Test
  void addTagToBookmarkById() {
    var bkmk =
        saveBookmarks(new AddBkmkReq("duckduckgo", "https://duckduckgo.com", List.of(1L), true));

    var addReq = restTemplate.exchange(bookmarkURI + "/{bookmarkID}/tagId?tagId={id}",
        HttpMethod.POST, getHttpEntity(restTemplate), BookmarkDTO.class, bkmk.get(0).id(), 5);

    var tags = addReq.getBody().tags();

    assertEquals(2, tags.size());
    assertEquals(2, tags.stream().filter(t -> t.id() == 5 || t.id() == 1).count());

    // No Existent bookmarkID
    var badReq = restTemplate.exchange(bookmarkURI + "/{bookmarkID}/tagId?tagId={id}",
        HttpMethod.POST, getHttpEntity(restTemplate), String.class, 22, 5);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, badReq.getStatusCode());
    assertEquals(new BookmarkNotFoundException().getMessage(), badReq.getBody());
  }

  @Test
  void deleteBookmarkById() {
    saveBookmarks(
        new AddBkmkReq("web scraping", "https://www.scrapethissite.com", List.of(1L, 6L), true));
    var response = restTemplate.exchange(bookmarksURI, HttpMethod.GET, getHttpEntity(restTemplate),
        BookmarkDTO[].class);
    var bkmkOpt = Optional.ofNullable(response.getBody());

    var id = bkmkOpt.orElseThrow()[0].id();
    var delResp = restTemplate.exchange(bookmarkURI + "?id={id}", HttpMethod.DELETE,
        getHttpEntity(restTemplate), String.class, id);
    assertEquals(HttpStatus.OK, delResp.getStatusCode());
  }

  @Test
  void attemptToDeleteBookmarkTagThatDoesNotExist() {

    var header = getHttpEntity(restTemplate);
    var delResp = restTemplate.exchange(bookmarkURI + "/100" + "/tag?tag={tag}", HttpMethod.DELETE,
        header, String.class, "random");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, delResp.getStatusCode());
    assertEquals(new BookmarkNotFoundException().getMessage(), delResp.getBody());

    // add the tag.
    restTemplate.exchange("/api/tags", HttpMethod.POST,
        new HttpEntity<>(List.of("buildings"), header.getHeaders()), TagDTO[].class);

    delResp = restTemplate.exchange(bookmarkURI + "/2" + "/tag?tag={tag}", HttpMethod.DELETE,
        header, String.class, "buildings");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, delResp.getStatusCode());
    assertEquals(new TagNotFoundException().getMessage(), delResp.getBody());

  }

  /**
   * Test the Flux endpoint for importing bookmarks.
   *
   * @throws IOException
   * @throws InterruptedException
   */
  @Test
  void importBookmarks() throws IOException {
    assertNotNull(new File("google_bookmarks_1_21_24.html"));
    var bodyBuilder = new MultipartBodyBuilder();
    byte[] fileContent =
        new ClassPathResource("google_bookmarks_1_21_24.html").getInputStream().readAllBytes();

    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    Mockito.when(securityContext.getAuthentication())
        .thenReturn(new UserAuthenticationToken(authentication, 0, null, 1));

    bodyBuilder.part("file", fileContent).filename("BookmarksExample.html")
        .contentType(MediaType.TEXT_HTML);
    HttpHeaders headers = new HttpHeaders();
    // test user
    headers.setBasicAuth("jsmith", "test");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    var signResp = restTemplate.postForEntity("/user/signin", entity, TokenRefreshResponse.class);

    // Get the cookie from signin.
    var cookieOpt = Optional.ofNullable(signResp.getHeaders().get("Set-Cookie"));
    var cookie = cookieOpt.orElseThrow().get(0);
    client.post().uri(bookmarkURI + "/import").accept(MediaType.APPLICATION_NDJSON)
        .cookie("findfirst", cookie).bodyValue(bodyBuilder.build()).exchange().expectStatus().isOk()
        .expectBodyList(BookmarkDTO.class).hasSize(3);
  }

  /**
   * Tests importing a file with an invalid content type (not text/html).
   */
  @Test
  void importBookmarksWithInvalidContentType() {
    // Create a byte array with text/plain content type
    byte[] fileContent = "<html><body>Test Content</body></html>".getBytes(StandardCharsets.UTF_8);

    // Build the multipart request with the invalid file
    var bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("invalid_file.txt")
        .contentType(MediaType.TEXT_PLAIN);

    // Set up headers with basic authentication
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("jsmith", "test");
    HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity =
        new HttpEntity<>(bodyBuilder.build(), headers);

    // Perform the POST request to import bookmarks
    ResponseEntity<String> response = restTemplate.exchange(bookmarkURI + "/import",
        HttpMethod.POST, requestEntity, String.class);

    // Assert that the response status is 400 Bad Request
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Assert the error message in the response body
    assertEquals("{\"error\":\"Uploaded file must have .html extension\"}", response.getBody());
  }

  /**
   * Tests importing a file that exceeds the maximum allowed size of 250MB.
   */
  @Test
  void importBookmarksWithFileTooLarge() {
    // Create a byte array slightly larger than 250MB (31,250,001 bytes)
    byte[] largeFile = new byte[31_250_001];

    // Build the multipart request with the large file
    var bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", largeFile).filename("large_file.html")
        .contentType(MediaType.TEXT_HTML);

    // Set up headers with basic authentication
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("jsmith", "test");
    HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity =
        new HttpEntity<>(bodyBuilder.build(), headers);

    // Perform the POST request to import bookmarks
    ResponseEntity<String> response = restTemplate.exchange(bookmarkURI + "/import",
        HttpMethod.POST, requestEntity, String.class);

    // Assert that the response status is 413 Payload Too Large
    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());

    // Assert the error message in the response body
    assertEquals("{\"error\":\"File too large. Maximum allowed size is 250MB\"}",
        response.getBody());
  }

  /**
   * Tests importing a .html file with an unsupported content type (e.g., application/json).
   */
  @Test
  void importBookmarksWithUnsupportedContentType() {
    // Create a byte array with application/json content
    byte[] fileContent = "{\"bookmarks\": []}".getBytes(StandardCharsets.UTF_8);

    // Build the multipart request with a .html extension but unsupported content
    // type
    var bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("invalid_content.html")
        .contentType(MediaType.APPLICATION_JSON); // Unsupported content type

    // Set up headers with basic authentication
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("jsmith", "test");
    HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity =
        new HttpEntity<>(bodyBuilder.build(), headers);

    // Perform the POST request to import bookmarks
    ResponseEntity<String> response = restTemplate.exchange(bookmarkURI + "/import",
        HttpMethod.POST, requestEntity, String.class);

    // Assert that the response status is 400 Bad Request
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Assert the error message in the response body
    assertEquals(
        "{\"error\":\"Unsupported content type. Supported types are text/html and text/plain.\"}",
        response.getBody());
  }

  /**
   * Tests importing a .html file without specifying a content type.
   */
  @Test
  void importBookmarksWithMissingContentType() {
    // Create a byte array with valid HTML content
    byte[] fileContent = "<html><body>Test Content</body></html>".getBytes(StandardCharsets.UTF_8);

    // Build the multipart request with a .html extension but no content type
    var bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("no_content_type.html"); // No content type
                                                                            // specified

    // Set up headers with basic authentication
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("jsmith", "test");
    HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity =
        new HttpEntity<>(bodyBuilder.build(), headers);

    // Perform the POST request to import bookmarks
    ResponseEntity<String> response = restTemplate.exchange(bookmarkURI + "/import",
        HttpMethod.POST, requestEntity, String.class);

    // Assert that the response status is 400 Bad Request
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Assert the error message in the response body
    assertEquals(
        "{\"error\":\"Unsupported content type. Supported types are text/html and text/plain.\"}",
        response.getBody());
  }

  /**
   * Tests importing a .html file with text/plain content type.
   */
  @Test
  void importBookmarksWithTextPlainContentType() {
    // Create a byte array with text/plain content
    byte[] fileContent =
        "<html><body>Plain Text Content</body></html>".getBytes(StandardCharsets.UTF_8);

    // Build the multipart request with a .html extension and text/plain content
    // type
    var bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("plainText.html")
        .contentType(MediaType.TEXT_PLAIN); // Supported content type

    // Set up headers with basic authentication
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("jsmith", "test");
    HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity =
        new HttpEntity<>(bodyBuilder.build(), headers);

    // Perform the POST request to import bookmarks
    ResponseEntity<String> response = restTemplate.exchange(bookmarkURI + "/import",
        HttpMethod.POST, requestEntity, String.class);

    // Assert that the response status is 200 OK
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void jdbcRepo() {
    long count = bookmarkJDBCRepository.count();
    assertTrue(count > 0);
  }

  @Test
  void triggerBookmarkAlreadyExistException() {
    saveBookmarks(new AddBkmkReq("search", "https://bing.com", new ArrayList<>(), true));
    var ent = getHttpEntity(restTemplate,
        new AddBkmkReq("search", "https://bing.com", new ArrayList<>(), true));
    var blResp = restTemplate.exchange(bookmarkURI, HttpMethod.POST, ent, Map.class);
    assertEquals(HttpStatus.CONFLICT, blResp.getStatusCode());
    assertEquals("Bookmark already exists.", (blResp.getBody()).get("error"));
  }

  @Test
  void export() {
    var ent = getHttpEntity(restTemplate);
    var response =
        restTemplate.exchange(bookmarksURI + "/export", HttpMethod.GET, ent, byte[].class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().length > 0);
  }

  private List<BookmarkDTO> saveBookmarks(AddBkmkReq... newBkmks) {
    HttpEntity<?> ent;
    // Test can not handle covariant return type of [] vs a single Bookmark.
    if (newBkmks.length == 1) {
      ent = getHttpEntity(restTemplate, newBkmks[0]);
      var bkmkResp = restTemplate.exchange(bookmarkURI, HttpMethod.POST, ent, BookmarkDTO.class);
      return List.of(bkmkResp.getBody());
    }
    ent = getHttpEntity(restTemplate, Arrays.asList(newBkmks));
    var blResp = restTemplate.exchange(bookmarkURI + "/addBookmarks", HttpMethod.POST, ent,
        BookmarkDTO[].class);
    assertEquals(HttpStatus.OK, blResp.getStatusCode());
    return List.of(blResp.getBody());
  }
}
