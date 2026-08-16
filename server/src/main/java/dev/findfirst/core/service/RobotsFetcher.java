package dev.findfirst.core.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotsFetcher {

  /**
   * Reported when no response ever came back, so there is no status of the server's to pass on. A
   * server error is the honest stand in: it tells the parser we do not know the site's rules, as
   * opposed to knowing that it has none.
   */
  private static final int NO_RESPONSE = 500;

  private final RestTemplate rest;

  /**
   * Fetches a site's robots.txt. Fetching is best effort: a site that is down, slow, unresolvable
   * or that answers with something other than a robots.txt is a normal thing to run into, so those
   * all come back as a response carrying the status rather than as an exception. Callers get to
   * decide what an unknown ruleset means to them, and adding a bookmark never fails over one.
   *
   * @param url any url on the site whose robots.txt is wanted.
   * @return the fetched robots.txt, or an empty body with the status that explains why there is
   *         none.
   */
  public RobotsTxtResponse getRobotsTxt(String url) {
    if (url == null || url.isBlank()) {
      log.error("Asked for the robots.txt of a url that is not there.");
      return noResponse();
    }

    URI robotsUri;
    try {
      URI uri = new URI(url);
      robotsUri = new URI(uri.getScheme(), uri.getAuthority(), "/robots.txt", uri.getQuery(),
          uri.getFragment());
    } catch (URISyntaxException ex) {
      log.error("Could not build a robots.txt url from {}: {}", url, ex.toString());
      return noResponse();
    }

    try {
      ResponseEntity<String> robots = rest.getForEntity(robotsUri, String.class);
      String body = robots.getBody();
      MediaType contentType = robots.getHeaders().getContentType();

      return new RobotsTxtResponse(robots.getStatusCode().value(),
          body == null ? new byte[0] : body.getBytes(),
          contentType == null ? "" : contentType.toString());

    } catch (HttpStatusCodeException ex) {
      // The site answered, just not with a robots.txt. Its status is worth passing on: a 4xx says
      // the site publishes no rules and may be crawled, a 5xx says to come back later.
      log.warn("robots.txt for {} answered with {}", url, ex.getStatusCode());
      return new RobotsTxtResponse(ex.getStatusCode().value(), new byte[0], "");

    } catch (RestClientException | IllegalArgumentException ex) {
      // Unknown host, refused connection, timeout, unreadable response, a url with no scheme to
      // request against; whatever the cause, we simply never learned the site's rules.
      log.error("Could not fetch robots.txt for {}: {}", url, ex.toString());
      return noResponse();
    }
  }

  private RobotsTxtResponse noResponse() {
    return new RobotsTxtResponse(NO_RESPONSE, new byte[0], "");
  }

  public record RobotsTxtResponse(int statusCode, byte[] text, String contentType) {

    @Override
    public final boolean equals(Object obj) {
      if (obj != null && obj instanceof RobotsTxtResponse(int statusCode, byte[] text, String ct)) {
        return statusCode == this.statusCode() && Arrays.equals(this.text(), text)
            && this.contentType.equals(ct);
      } else {
        return false;
      }

    }

    @Override
    public final int hashCode() {
      int contentTypeHash = 0;
      if (contentType() != null) {
        contentTypeHash = contentType.hashCode();
      }
      return this.statusCode + Arrays.hashCode(this.text) + contentTypeHash;
    }

    @Override
    public final String toString() {
      return String.format("Status: %s, contentType: %s, text %s", this.statusCode, this.contentType,
          new String(this.text));
    }

  }
}
