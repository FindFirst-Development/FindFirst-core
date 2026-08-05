package dev.findfirst.core.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.findfirst.core.dto.*;
import dev.findfirst.core.exceptions.BookmarkAlreadyExistsException;
import dev.findfirst.core.exceptions.BookmarkNotFoundException;
import dev.findfirst.core.exceptions.PageGreaterThanTotalException;
import dev.findfirst.core.exceptions.TagNotFoundException;
import dev.findfirst.core.model.ExportBookmark;
import dev.findfirst.core.model.TagBookmarks;
import dev.findfirst.core.model.jdbc.BookmarkJDBC;
import dev.findfirst.core.model.jdbc.BookmarkTag;
import dev.findfirst.core.model.jdbc.TagJDBC;
import dev.findfirst.core.repository.jdbc.BookmarkJDBCRepository;
import dev.findfirst.core.repository.jdbc.BookmarkTagRepository;
import dev.findfirst.security.userauth.context.UserContext;
import dev.findfirst.users.service.UserManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookmarkService {

  private final BookmarkJDBCRepository bookmarkJDBCRepository;

  private final BookmarkTagRepository bookmarkTagRepository;

  private final TagService tagService;

  private final WebCheckService webCheckService;

  private final ScreenshotManager sManager;

  private final UserContext uContext;

  private final UserManagementService userService;

  private final TypesenseService typesense;

  public List<BookmarkDTO> listJDBC() {
    return convertBookmarkJDBCToDTO(
        bookmarkJDBCRepository.findAllBookmarksByUser(uContext.getUserId()), uContext.getUserId());
  }

  public PaginatedBookmarkRes listPaginatedJDBC(PaginatedBookmarkReq reqBkmk)
      throws PageGreaterThanTotalException {
    // Page is 0 indexed.
    int page = reqBkmk.page() - 1;

    Pageable pageable = Pageable.ofSize(reqBkmk.size()).withPage(page);

    Page<BookmarkJDBC> pageResult =
        bookmarkJDBCRepository.findAllByUserId(uContext.getUserId(), pageable);
    List<BookmarkJDBC> pageContent = pageResult.getContent();
    Integer totalPages = pageResult.getTotalPages();

    if (pageContent.isEmpty() && page > 0)
      throw new PageGreaterThanTotalException(page + 1);

    List<BookmarkDTO> bookmarks = convertBookmarkJDBCToDTO(pageContent, uContext.getUserId());

    return new PaginatedBookmarkRes(bookmarks, totalPages, reqBkmk.page());

  }

  public Optional<BookmarkDTO> getBookmarkById(long id) {
    var bkOpt = bookmarkJDBCRepository.findById(id);

    if (bkOpt.isPresent()) {
      return Optional
          .of(convertBookmarkJDBCToDTO(List.of(bkOpt.get()), uContext.getUserId()).get(0));
    }
    return Optional.ofNullable(null);
  }

  public Optional<BookmarkJDBC> findByIdJDBC(long id) {
    return bookmarkJDBCRepository.findById(id);
  }

  public Optional<BookmarkDTO> getBookmarkDTOById(Long id) {
    var ent = bookmarkJDBCRepository.findById(id);
    if (ent.isPresent()) {
      var bkmk = convertBookmarkJDBCToDTO(List.of(ent.get()), uContext.getUserId()).get(0);
      return Optional.of(bkmk);
    } else {
      return Optional.of(null);
    }
  }

  public List<BookmarkDTO> convertBookmarkJDBCToDTO(List<BookmarkJDBC> bookmarkEntities,
      List<String> highlights, int userId) {
    var bks = convertBookmarkJDBCToDTO(bookmarkEntities, userId);

    for (int i = 0; i < highlights.size() && i < bookmarkEntities.size(); i++) {
      bks.get(i).textHighlight(highlights.get(i));
    }
    return bks;
  }

  public List<BookmarkDTO> convertBookmarkJDBCToDTO(List<BookmarkJDBC> bookmarkEntities,
      int userId) {

    // Get the bookmarks that are associated to the Tag.
    return bookmarkEntities.stream().map(ent -> {
      var tagIds = bookmarkTagRepository.getAllTagIdsForBookmark(ent.getId(), userId);

      var tagEnts = tagService.findAllById(tagIds.stream().map(BookmarkTag::getTagId).toList());

      List<TagOnly> tagDTOs = new ArrayList<>();

      for (var t : tagEnts) {
        tagDTOs.add(new TagOnly(t.getId(), t.getTitle()));
      }

      return BookmarkDTO.builder().id(ent.getId()).title(ent.getTitle()).url(ent.getUrl())
          .screenshotUrl(ent.getScreenshotUrl()).scrapable(ent.getScrapable())
          .createdDate(ent.getCreatedDate()).lastModifiedOn(ent.getLastModifiedDate()).tags(tagDTOs)
          .build();

    }).toList();
  }

  public List<BookmarkOnly> convertBookmarkJDBCToBookmarkOnly(List<BookmarkJDBC> bookmarkEntities) {
    // Get the bookmarks that are associated to the Tag.
    return bookmarkEntities.stream()
        .map(ent -> new BookmarkOnly(ent.getId(), ent.getTitle(), ent.getUrl(),
            ent.getScreenshotUrl(), ent.getScrapable(), ent.getCreatedDate(),
            ent.getLastModifiedDate()))
        .toList();
  }

  public BookmarkDTO addBookmark(AddBkmkReq reqBkmk)
      throws TagNotFoundException, URISyntaxException {
    var tags = new ArrayList<Long>();

    if (bookmarkJDBCRepository.findByUrl(reqBkmk.url(), uContext.getUserId()).isPresent()) {
      String logMessage = "Bookmark already exists for user with id " + uContext.getUserId()
          + " and url: " + reqBkmk.url();
      throw new BookmarkAlreadyExistsException(logMessage);
    }

    if (reqBkmk.tagIds() != null) {
      for (var t : reqBkmk.tagIds()) {
        tags.add(tagService.findByIdJDBC(t).orElseThrow(TagNotFoundException::new).getId());
      }
    }

    Document retDoc = null;
    var screenshotUrlOpt = Optional.of("");
    boolean shouldScrape = reqBkmk.scrapable();

    // Can we scrape this?
    if (reqBkmk.scrapable()) {
      shouldScrape = webCheckService.isScrapable(reqBkmk.url());
    }

    if (shouldScrape) {
      log.debug("Scrapable: true.\tScrapping URL and taking screenshot.");
      retDoc = scrape(reqBkmk.url());
      screenshotUrlOpt = sManager.getScreenshot(reqBkmk.url());
    }

    String title = resolveTitle(reqBkmk, retDoc);

    var user = userService.getUserById(uContext.getUserId()).orElseThrow();

    var savedTags = new HashSet<BookmarkTag>();

    var newBkmkJdbc = new BookmarkJDBC(null, user.getUserId(), new Date(), user.getUsername(),
        user.getUsername(), new Date(), title, reqBkmk.url(), screenshotUrlOpt.orElse(""),
        reqBkmk.scrapable(), savedTags);

    var saved = bookmarkJDBCRepository.save(newBkmkJdbc);

    // After saving to PostgresDB grab record to associate document in typesense.
    if (shouldScrape) {
      typesense.addText(saved, retDoc);
    }

    for (var tag : tags) {
      var bt = new BookmarkTag(saved.getId(), tag);
      savedTags.add(bt);
      bookmarkTagRepository.saveBookmarkTag(bt);
    }

    newBkmkJdbc.setTags(savedTags);
    return convertBookmarkJDBCToDTO(List.of(newBkmkJdbc), user.getUserId()).get(0);
  }

  /**
   * Fetches the page so its title and text can be indexed.
   *
   * @param url the url to fetch.
   * @return the fetched document, or null when the page could not be reached.
   */
  private Document scrape(String url) {
    try {
      var doc = Jsoup.connect(url).get();
      log.debug("Scraped Title: {}", doc.title());
      return doc;
    } catch (IOException e) {
      log.error(e.toString());
      return null;
    }
  }

  /**
   * Resolves the title to store for a new bookmark. A title the user explicitly gave us always wins
   * over the one found while scraping the page, otherwise there would be no way to give a scrapable
   * bookmark a custom title.
   *
   * @param reqBkmk the add request as it came from the client.
   * @param scrapedDoc the scraped page, null when the url was not or could not be scraped.
   * @return the explicit title, else the scraped title, else the host of the url.
   * @throws URISyntaxException if there is no title to fall back on and the url is malformed.
   */
  private String resolveTitle(AddBkmkReq reqBkmk, Document scrapedDoc) throws URISyntaxException {
    if (hasExplicitTitle(reqBkmk)) {
      return reqBkmk.title();
    }

    if (scrapedDoc != null && !scrapedDoc.title().isBlank()) {
      return scrapedDoc.title();
    }

    // Nothing was given and nothing was scraped, so the host is the best we can do.
    var title = reqBkmk.title();
    return title == null || title.isBlank() ? new URI(reqBkmk.url()).getHost() : title;
  }

  /**
   * A title only counts as explicit when the user actually chose it. Clients default the field to
   * the url itself, which means unset rather than a user's choice.
   *
   * @param reqBkmk the add request as it came from the client.
   * @return true when the request carries a user chosen title.
   */
  private boolean hasExplicitTitle(AddBkmkReq reqBkmk) {
    var title = reqBkmk.title();
    return title != null && !title.isBlank() && !title.equals(reqBkmk.url());
  }

  public List<BookmarkDTO> addBookmarks(List<AddBkmkReq> bookmarks) {
    return bookmarks.stream().map(t -> {
      try {
        return addBookmark(t);
      } catch (BookmarkAlreadyExistsException | TagNotFoundException | URISyntaxException e) {
        log.debug(e.toString());
        return null;
      }
    }).toList();
  }

  public BookmarkDTO addTagById(BookmarkJDBC bk, long tagId) {
    bk.addTag(new BookmarkTag(bk.getId(), tagId));
    bookmarkJDBCRepository.save(bk);
    return convertBookmarkJDBCToDTO(List.of(bk), uContext.getUserId()).get(0);
  }

  /**
   * Exports bookmarks by their tag groups. The largest tag groups are exported first. Any bookmark
   * already accounted for in that group will be excluded from any other group that it was also
   * tagged.
   *
   * @return String representing HTLM file.
   */
  public String export() {
    var tags = tagService.getTags();
    var foundMap = new HashMap<Long, Long>();
    var uniqueBkmksWithTag = new ArrayList<TagBookmarks>();

    // Sort by the largest tags set.
    var sorted = tags.stream()
        .sorted((rTag, lTag) -> rTag.bookmarks().size() - lTag.bookmarks().size()).toList();

    // streams the sorted list
    sorted.stream().forEach(t -> {
      var uniques = new ArrayList<BookmarkOnly>();
      addUniqueBookmarks(t, uniques, foundMap, uniqueBkmksWithTag);
    });
    var exporter = new ExportBookmark(uniqueBkmksWithTag);
    return exporter.toString();
  }

  /**
   * Checks if a bookmark has already been found in previous tag group. If it has not it is added to
   * uniques, and the id added to map for fast lookups. Finally record that contains the title of
   * the tag `cooking` `docs` for example is created with it associated bookmarks. The record is
   * added to uniqueBkmkWithTags.
   *
   * @param t Tag
   * @param uniques List<Bookmark> of uniques
   * @param alreadyFound Map<Long, Long> for fast lookup
   * @param uniqueBkmksWithTag Record of Tag Title with Bookmark.
   */
  private void addUniqueBookmarks(TagDTO t, List<BookmarkOnly> uniques,
      Map<Long, Long> alreadyFound, List<TagBookmarks> uniqueBkmksWithTag) {
    t.bookmarks().stream().forEach(bkmk -> {
      var found = alreadyFound.get(bkmk.id());
      if (found == null) {
        alreadyFound.put(bkmk.id(), bkmk.id());
        uniques.add(bkmk);
      }
    });
    if (!uniques.isEmpty()) {
      uniqueBkmksWithTag.add(new TagBookmarks(t.title(), uniques));
    }
  }

  public void deleteBookmark(Long bookmarkId) {
    if (bookmarkId != null) {
      Optional<BookmarkJDBC> bookmark = bookmarkJDBCRepository.findById(bookmarkId);
      if (bookmark.isPresent() && bookmark.get().getUserId() == uContext.getUserId()) {
        try {
          bookmarkJDBCRepository.deleteById(bookmarkId);
        } catch (Exception exception) {
          // I don't know a fool proof way of preventing this error
          // other than blocking on the method. Which would not
          // be ideal given its a controller and the error itself
          // resolves.
        }
      }
    }
  }

  public void deleteAllBookmarks() {
    // Finds all that belong to the user and deletes them.
    // Otherwise the @preRemove throws an execption as it should.
    bookmarkJDBCRepository
        .deleteAll(bookmarkJDBCRepository.findAllBookmarksByUser(uContext.getUserId()));
  }

  public void addTagToBookmarkJDBC(BookmarkJDBC bookmark, TagJDBC tag) {
    if (bookmark == null || tag == null)
      throw new NoSuchFieldError();
    bookmarkTagRepository.saveBookmarkTag(new BookmarkTag(bookmark.getId(), tag.getId()));
  }

  public BookmarkTag deleteTag(BookmarkTag bt) throws TagNotFoundException {
    var deletedCount = bookmarkTagRepository.deleteBookmarkTag(bt);
    log.debug("DELETED: {} BookmarkTags", deletedCount);
    if (deletedCount == 0) {
      throw new TagNotFoundException();
    }
    return bt;
  }

  public Flux<BookmarkDTO> importBookmarks(String htmlFile) {
    var doc = Jsoup.parse(htmlFile);
    var hrefs = doc.getElementsByAttribute("href");
    hrefs.stream().forEach(e -> log.debug(e.attributes().get("href")));
    var sec = SecurityContextHolder.getContext();
    return Flux.fromStream(hrefs.stream()).map(el -> {
      String url = el.attributes().get("href");
      try {
        var retDoc = Jsoup.connect(url).get();
        log.debug("Response {}", retDoc.connection().response().statusMessage());
        log.debug(retDoc.title());
        // Issues with the context being lost between requests and database write.
        SecurityContextHolder.setContext(sec);
        String title = retDoc.title();
        if (!url.equals("") || url == null) {
          title = (title.equals("") || title == null) ? url : title;
          log.debug("Bookmark contains: \n\t{},\n\t{}", title, url);
          return addBookmark(
              new AddBkmkReq(title, URLDecoder.decode(url, StandardCharsets.UTF_8), null, true));
        }
      } catch (IOException | BookmarkAlreadyExistsException | TagNotFoundException
          | URISyntaxException ex) {
        log.error(ex.getMessage());
      }
      // empty bookmark.
      return new BookmarkDTO();
    }).delayElements(Duration.ofMillis(100));
  }

  public BookmarkDTO updateBookmark(UpdateBookmarkReq updateReq) throws BookmarkNotFoundException {
    var bkmk =
        bookmarkJDBCRepository.findById(updateReq.id()).orElseThrow(BookmarkNotFoundException::new);

    boolean allNull = true;

    if (updateReq.title() != null && !updateReq.title().isEmpty()) {
      allNull = false;
      log.debug("Update request Title {}", updateReq.title());
      bkmk.setTitle(updateReq.title());
    }

    if (updateReq.isScrapable() != null) {
      allNull = false;
      log.debug("is Scrapable {}", updateReq.isScrapable());
      bkmk.setScrapable(updateReq.isScrapable());
    }

    if (updateReq.url() != null && !updateReq.url().isBlank()) {
      allNull = false;
      log.debug("Update request url {}", updateReq.url());
      bkmk.setUrl(updateReq.url());
    }

    if (allNull) {
      return convertBookmarkJDBCToDTO(List.of(bkmk), uContext.getUserId()).get(0);
    }

    return convertBookmarkJDBCToDTO(List.of(bookmarkJDBCRepository.save(bkmk)),
        uContext.getUserId()).get(0);
  }

  @Scheduled(cron = "0 * 2 * * *", zone = "America/New_York")
  @Transactional
  public void addMissingScreenShotUrlToBookMarks() {
    log.info("Executing addMissingScreenShotUrlToBookMarks");
    List<BookmarkJDBC> list = bookmarkJDBCRepository.findBookmarksWithEmptyOrBlankScreenShotUrl();
    list.forEach(bookmark -> {
      if (bookmark.getScrapable() != null && bookmark.getScrapable()) {
        sManager.getScreenshot(bookmark.getUrl()).ifPresentOrElse(bookmark::setScreenshotUrl,
            () -> log.error("Failed to scrap bookmark with id: {}", bookmark.getId()));
      }
    });
    bookmarkJDBCRepository.saveAll(list);
    log.info("Finished addMissingScreenShotUrlToBookMarks");
  }

}
