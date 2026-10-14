package me.suhsaechan.web.controller.view;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoController {

  private static final List<String> SITEMAP_PATHS = List.of(
      "/dashboard", "/issue-helper", "/translator", "/sejong-auth", "/suh-random", "/suh-devops-template");

  @Value("${site.base-url:https://lab.suhsaechan.kr}")
  private String siteBaseUrl;

  @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> robots() {
    String body = """
        User-agent: *
        Allow: /
        Disallow: /api/
        Disallow: /login
        Disallow: /docker-logs
        Disallow: /ai-server
        Disallow: /chatbot-management
        Disallow: /grass
        Disallow: /task-tracker
        Disallow: /somansa
        Disallow: /notice-management

        Sitemap: %s/sitemap.xml
        """.formatted(siteBaseUrl);
    return ResponseEntity.ok(body);
  }

  @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<String> sitemap() {
    StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    for (String path : SITEMAP_PATHS) {
      xml.append("  <url><loc>").append(siteBaseUrl).append(path).append("</loc></url>\n");
    }
    xml.append("</urlset>\n");
    return ResponseEntity.ok(xml.toString());
  }
}
