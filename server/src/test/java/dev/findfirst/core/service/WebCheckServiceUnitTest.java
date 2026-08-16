package dev.findfirst.core.service;

import static org.mockito.ArgumentMatchers.anyString;

import dev.findfirst.core.service.RobotsFetcher.RobotsTxtResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Covers what {@link WebCheckService} makes of a robots.txt it could not fetch or could not read.
 * None of it may throw: the bookmark still has to be created, it simply is not scraped.
 */
class WebCheckServiceUnitTest {

  private final RobotsFetcher robotsFetcher = Mockito.mock(RobotsFetcher.class);

  private final WebCheckService webCheckService = new WebCheckService(robotsFetcher);

  private void respondWith(int statusCode, String body) {
    Mockito.when(robotsFetcher.getRobotsTxt(anyString()))
        .thenReturn(new RobotsTxtResponse(statusCode, body.getBytes(), "text/plain"));
  }

  /** No robots.txt means no rules to break. */
  @Test
  void notFoundIsScrapable() {
    respondWith(404, "");
    Assertions.assertTrue(webCheckService.isScrapable("https://findfirst.dev/about"));
  }

  /** RFC 9309 reads any 4xx the same way: the site publishes no rules, so none are broken. */
  @Test
  void forbiddenIsScrapable() {
    respondWith(403, "");
    Assertions.assertTrue(webCheckService.isScrapable("https://findfirst.dev/about"));
  }

  /** We never learned the rules, so we stay out until we do. */
  @Test
  void unreachableSiteIsNotScrapable() {
    respondWith(500, "");
    Assertions.assertFalse(webCheckService.isScrapable("https://findfirst.dev/about"));
  }

  @Test
  void emptyRobotsTxtIsScrapable() {
    respondWith(200, "");
    Assertions.assertTrue(webCheckService.isScrapable("https://findfirst.dev/about"));
  }

  /** Neither junk where a robots.txt should be nor a url we cannot parse is worth an error. */
  @Test
  void unreadableRobotsTxtIsNotAnError() {
    respondWith(200, "not a robots.txt at all");
    Assertions.assertTrue(webCheckService.isScrapable("not a url"));
  }

  @Test
  void failureToFetchIsNotAnError() {
    Mockito.when(robotsFetcher.getRobotsTxt(anyString()))
        .thenThrow(new RuntimeException("something no one saw coming"));

    Assertions.assertFalse(webCheckService.isScrapable("https://findfirst.dev/about"));
  }
}
