package dev.findfirst.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;

import dev.findfirst.core.service.RobotsFetcher.RobotsTxtResponse;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class RobotsFetcherTest {

  private final RestTemplate restMock = Mockito.mock(RestTemplate.class);

  private final RobotsFetcher instance = new RobotsFetcher(restMock);

  @Test
  void testGettingRobotsTxt() {
    byte[] expectedContent = "User-agent: *\nDisallow: /admin".getBytes();
    String domain = "https://findfirst.com/robots.txt";

    var expected = new RobotsTxtResponse(200, expectedContent, MediaType.TEXT_PLAIN.toString());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    ResponseEntity<String> mockReturn =
        new ResponseEntity<String>(new String(expectedContent), headers, HttpStatus.OK);

    when(restMock.getForEntity(any(URI.class), eq(String.class))).thenReturn(mockReturn);
    RobotsTxtResponse rTxtResponse = instance.getRobotsTxt(domain);
    assertEquals(expected, rTxtResponse);
  }

  @Test
  void missingContentTypeAndBodyAreNotAnError() {
    ResponseEntity<String> mockReturn =
        new ResponseEntity<String>(null, new HttpHeaders(), HttpStatus.NO_CONTENT);

    when(restMock.getForEntity(any(URI.class), eq(String.class))).thenReturn(mockReturn);

    assertEquals(new RobotsTxtResponse(204, new byte[0], ""),
        instance.getRobotsTxt("https://findfirst.com"));
  }

  /** A site with no robots.txt has no rules to break, so its 404 has to reach the parser as one. */
  @Test
  void notFoundKeepsItsStatus() {
    when(restMock.getForEntity(any(URI.class), eq(String.class)))
        .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found",
            new HttpHeaders(), new byte[0], null));

    assertEquals(new RobotsTxtResponse(404, new byte[0], ""),
        instance.getRobotsTxt("https://findfirst.com"));
  }

  @Test
  void serverErrorIsReportedRatherThanThrown() {
    when(restMock.getForEntity(any(URI.class), eq(String.class)))
        .thenThrow(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Unavailable",
            new HttpHeaders(), new byte[0], null));

    assertEquals(new RobotsTxtResponse(503, new byte[0], ""),
        instance.getRobotsTxt("https://findfirst.com"));
  }

  @Test
  void unreachableSiteIsReportedRatherThanThrown() {
    when(restMock.getForEntity(any(URI.class), eq(String.class)))
        .thenThrow(new ResourceAccessException("findfirst.com: unknown host"));

    assertEquals(new RobotsTxtResponse(500, new byte[0], ""),
        instance.getRobotsTxt("https://findfirst.com"));
  }

  @Test
  void urlWithNoSchemeIsReportedRatherThanThrown() {
    when(restMock.getForEntity(any(URI.class), eq(String.class)))
        .thenThrow(new IllegalArgumentException("URI is not absolute"));

    assertEquals(new RobotsTxtResponse(500, new byte[0], ""), instance.getRobotsTxt("findfirst"));
  }

  @Test
  void malformedUrlIsReportedRatherThanThrown() {
    assertEquals(new RobotsTxtResponse(500, new byte[0], ""),
        instance.getRobotsTxt("https://find first.com/ ^"));
  }

  @Test
  void missingUrlIsReportedRatherThanThrown() {
    assertEquals(new RobotsTxtResponse(500, new byte[0], ""), instance.getRobotsTxt(null));
    assertEquals(new RobotsTxtResponse(500, new byte[0], ""), instance.getRobotsTxt("  "));
  }
}
