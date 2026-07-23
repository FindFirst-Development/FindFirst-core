package dev.findfirst.users.controller;

import static dev.findfirst.utilities.HttpUtility.getHttpEntity;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import dev.findfirst.core.annotations.IntegrationTest;
import dev.findfirst.core.annotations.MockTypesense;
import dev.findfirst.core.repository.jdbc.BookmarkJDBCRepository;
import dev.findfirst.core.repository.jdbc.TagJDBCRepository;
import dev.findfirst.core.service.TypesenseService;
import dev.findfirst.security.userauth.models.TokenRefreshResponse;
import dev.findfirst.security.userauth.models.payload.request.SignupRequest;
import dev.findfirst.users.exceptions.NoUserFoundException;
import dev.findfirst.users.model.MailHogMessage;
import dev.findfirst.users.model.oauth2.Oauth2Source;
import dev.findfirst.users.model.user.TokenPassword;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.findfirst.users.model.user.User;
import dev.findfirst.users.repository.UserRepo;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@IntegrationTest
@MockTypesense
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.yml",
    properties = {"spring.security.oauth2.client.registration.github.client-secret=secret-oauth",
        "spring.security.oauth2.client.registration.github.client-id=test-id"})
class UserControllerTest {

  TestRestTemplate restTemplate = new TestRestTemplate();

  @Value("${findfirst.screenshot.location}")
  String testPicture;

  @InjectMocks
  private TypesenseService typesense;

  @Autowired
  UserControllerTest(TestRestTemplate tRestTemplate) {
    this.restTemplate = tRestTemplate;
  }

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

  @Container
  public static GenericContainer<?> mailhog =
      new GenericContainer<>(DockerImageName.parse("mailhog/mailhog:latest")).withExposedPorts(1025,
          8025);

  @TestConfiguration
  public static class JavaMailSenderConfiguration {
    @Bean
    public JavaMailSender javaMailSender() {
      JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

      mailSender.setHost(mailhog.getHost());
      mailSender.setPort(mailhog.getFirstMappedPort());
      mailSender.setProtocol("smtp");

      Properties properties = new Properties();
      properties.setProperty("mail.smtp.auth", "false");
      properties.setProperty("mail.smtp.starttls.enable", "false");

      mailSender.setJavaMailProperties(properties);

      return mailSender;
    }
  }

  private String userUrl = "/user";

  /**
   * Tests that a user should be able to sign up. After signing up another user should not be able
   * use the same username or email.
   */
  @Test
  void userSignup() {
    var headers = new HttpHeaders();
    var ent = new HttpEntity<>(
        new SignupRequest("Steve-Man2", "steve2@google.com", "$tev3s_sup3rH@rdPassword"), headers);
    var response = restTemplate.exchange(userUrl + "/signup", HttpMethod.POST, ent, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    /** This should fail as the user should already exist. */
    response = restTemplate.exchange(userUrl + "/signup", HttpMethod.POST, ent, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  /**
   * Create a user, gets the registration token from the email. Uses the token to complete
   * registration.
   */
  @Test
  void completeSignupAndRegistration() {
    var headers = new HttpHeaders();
    var ent = new HttpEntity<>(
        new SignupRequest("Steve-Man", "steveg@test.com", "$tev3s_sup3rH@rdPassword"), headers);
    var response = restTemplate.exchange(userUrl + "/signup", HttpMethod.POST, ent, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    try {
      var token = getTokenFromEmail(0, 1);
      var regResponse = restTemplate.getForEntity(userUrl + "/regitrationConfirm?token={token}",
          String.class, token);
      assertEquals(HttpStatus.SEE_OTHER, regResponse.getStatusCode());
    } catch (Exception e) {
      // fail the test should show message
      assertTrue(false, e.getMessage());
    }
  }

  @Test
  @Order(1)
  void resetPassword() {
    String token = "";
    var response = restTemplate.exchange(userUrl + "/resetPassword?email={email}", HttpMethod.POST,
        new HttpEntity<>(new HttpHeaders()), String.class, "jsmith@google.com");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    try {
      token = getTokenFromEmail(0, 2);
    } catch (Exception e) {
      // fail the test should show message
      assertTrue(false, e.getMessage());
    }
    response = restTemplate.exchange(userUrl + "/changePassword?token={tkn}", HttpMethod.GET,
        new HttpEntity<>(new HttpHeaders()), String.class, token);
    assertEquals(HttpStatus.SEE_OTHER, response.getStatusCode());

    var loc = Optional.ofNullable(response.getHeaders().get("Location")).orElseThrow().get(0);
    var urlStruct = loc.split("/");
    // token is the last part of the string
    var tknParam = urlStruct[urlStruct.length - 1];
    assertNotNull(tknParam);
    restTemplate.exchange(userUrl + "/changePassword?tokenPassword={tkn}", HttpMethod.POST,
        new HttpEntity<>(new TokenPassword(tknParam, "jsmithsNewsPassword!"), new HttpHeaders()),
        String.class, token);
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("jsmith", "jsmithsNewsPassword!");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    var signResp = restTemplate.postForEntity("/user/signin", entity, TokenRefreshResponse.class);
    assertEquals(HttpStatus.OK, signResp.getStatusCode());

  }

  String getTokenFromEmail(int emailIdx, int lineWithToken) throws Exception {
    String host = mailhog.getHost();
    int port = mailhog.getMappedPort(8025);

    String url = "http://" + host + ":" + port + "/api/v2/messages";
    var messageRaw = restTemplate.getForEntity(url, String.class).getBody();

    ObjectMapper mapper = new ObjectMapper();
    MailHogMessage mailHogMessage = mapper.readValue(messageRaw, MailHogMessage.class);
    var message = mailHogMessage.items()[emailIdx];
    var body = message.Content().Body();
    var secondLine = body.split("\n")[lineWithToken];
    var token = secondLine.split("=")[1];
    return token.strip();
  }

  @Test
  void refreshToken() {
    HttpHeaders headers = new HttpHeaders();
    // test user
    headers.setBasicAuth("jsmith", "test");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    var signResp = restTemplate.postForEntity("/user/signin", entity, TokenRefreshResponse.class);
    var tknRefresh = Optional.ofNullable(signResp.getBody()).orElseThrow();
    var refreshTkn = tknRefresh.refreshToken();
    var resp = restTemplate.exchange(userUrl + "/refreshToken?token={refreshToken}",
        HttpMethod.POST, new HttpEntity<>(new HttpHeaders()), String.class, refreshTkn);
    assertEquals(HttpStatus.OK, resp.getStatusCode());
  }

  @Test
  void testUserProfile_PhotoTooLarg() {
    byte[] largeContent = new byte[3 * 1024 * 1024]; // 2 MB Max
    // Use MultipartBodyBuilder to build the multipart request
    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", largeContent).filename("fakePic.png").contentType(MediaType.IMAGE_PNG);

    var requestEntity = getHttpEntity(restTemplate, "king", "test", bodyBuilder.build());

    var response = restTemplate.postForEntity("/user/profile-picture", requestEntity, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testGetUserProfilePicture_NotFound() {

    byte[] largeContent = new byte[2 * 1024 * 1024]; // 2 MB Max

    // Use MultipartBodyBuilder to build the multipart request
    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", largeContent).filename("fakePic.png").contentType(MediaType.IMAGE_PNG);

    var requestEntity = getHttpEntity(restTemplate, "king", "test", bodyBuilder.build());

    var response = restTemplate.postForEntity("/user/profile-picture", requestEntity, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testRemoveUserPhoto_Success() throws Exception {
    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", Files.readAllBytes(Path.of(testPicture + "/facebook.com.png")))
        .filename("facebook.com.png").contentType(MediaType.IMAGE_PNG);

    var requestEntity = getHttpEntity(restTemplate, "king", "test", bodyBuilder.build());

    var response = restTemplate.postForEntity("/user/profile-picture", requestEntity, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void getAllProivders() {
    var response = restTemplate.getForEntity("/user/oauth2Providers", Oauth2Source[].class);
    var sources = response.getBody();
    assertTrue(sources.length == 1);
    assertEquals("GitHub", sources[0].provider(), "Github should be the provider.");
  }

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private BookmarkJDBCRepository bookmarkJDBCRepository;

  @Autowired
  private TagJDBCRepository tagJDBCRepository;

  @Test
  void testDeleteUser() throws Exception {

    SignupRequest request = new SignupRequest(
            "testUser",
            "testUser@gmail.com",
            "testPassword"
    );

    restTemplate.postForEntity("/user/signup",request,String.class);

      var token = getTokenFromEmail(0, 1);
      
      var regResponse = restTemplate.getForEntity("/user/regitrationConfirm?token={token}",
              String.class, token);
      assertEquals(HttpStatus.SEE_OTHER, regResponse.getStatusCode());

//      Sign in
    HttpHeaders signInHeaders = new HttpHeaders();
    signInHeaders.setBasicAuth("testUser","testPassword");
    HttpEntity<String> signInEntity = new HttpEntity<>(signInHeaders);
    var signInResponse = restTemplate.exchange("/user/signin",HttpMethod.POST,signInEntity, TokenRefreshResponse.class);
    assertEquals(HttpStatus.OK,signInResponse.getStatusCode());

    User user = userRepo.findByUsername("testUser")
            .orElseThrow(NoUserFoundException::new);

//   first delete
    HttpHeaders headers = new HttpHeaders();
    String accessToken = signInResponse.getBody().accessToken();
    headers.setBearerAuth(accessToken);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    var response = restTemplate.exchange("/user",HttpMethod.DELETE,entity,Void.class);
    assertEquals(HttpStatus.OK,response.getStatusCode());

//    Checking

    assertFalse(userRepo.existsByUsername("testUser"));
    assertTrue(bookmarkJDBCRepository.findAllBookmarksByUser(user.getUserId()).isEmpty());
    assertTrue(tagJDBCRepository.findAllByUserId(user.getUserId()).isEmpty());

  }
}
