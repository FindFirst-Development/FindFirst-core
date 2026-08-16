package dev.findfirst.core.service;

import java.util.List;

import dev.findfirst.core.service.RobotsFetcher.RobotsTxtResponse;

import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class WebCheckService {

  private final RobotsFetcher robotsFetcher;

  public WebCheckService(@Autowired RobotsFetcher robotsFetcher) {
    this.robotsFetcher = robotsFetcher;
  }

  /**
   * Find out whether the robots.txt file of a website allows scraping. Answering this is best
   * effort: a site we cannot reach, or one whose robots.txt we cannot make sense of, is reported as
   * off limits rather than raised as an error, so that a bookmark for it can still be created.
   *
   * @param url the url that would be scraped.
   * @return true only when the site's rules are known to allow it.
   */
  public boolean isScrapable(String url) {
    try {
      RobotsTxtResponse robotsTxtResponse = robotsFetcher.getRobotsTxt(url);
      int statusCode = robotsTxtResponse.statusCode();
      SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
      SimpleRobotRules rules;

      // Anything but a 2xx means we hold no robots.txt to read, so the status is what decides.
      // Per RFC 9309 a 4xx means the site publishes no rules and is open to crawling, while a
      // server error means its rules are only unknown for now, so we leave it be.
      if (statusCode >= 200 && statusCode < 300) {
        rules = parser.parseContent(url, robotsTxtResponse.text(), robotsTxtResponse.contentType(),
            List.of());
      } else {
        rules = parser.failedFetch(statusCode);
      }

      return rules.isAllowed(url);
    } catch (RuntimeException ex) {
      log.error("Could not work out the robots.txt rules for {}, treating it as off limits: {}",
          url, ex.toString());
      return false;
    }
  }
}
