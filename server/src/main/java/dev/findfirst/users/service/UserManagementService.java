package dev.findfirst.users.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.UnexpectedException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import dev.findfirst.core.service.BookmarkService;
import dev.findfirst.core.service.TagService;
import dev.findfirst.security.jwt.service.RefreshTokenService;
import dev.findfirst.security.jwt.service.TokenService;
import dev.findfirst.security.userauth.context.UserContext;
import dev.findfirst.security.userauth.models.RefreshToken;
import dev.findfirst.security.userauth.models.payload.request.SignupRequest;
import dev.findfirst.users.exceptions.EmailAlreadyRegisteredException;
import dev.findfirst.users.exceptions.NoUserFoundException;
import dev.findfirst.users.exceptions.UserNameTakenException;
import dev.findfirst.users.model.user.Role;
import dev.findfirst.users.model.user.SigninTokens;
import dev.findfirst.users.model.user.Token;
import dev.findfirst.users.model.user.User;
import dev.findfirst.users.repository.PasswordTokenRepository;
import dev.findfirst.users.repository.UserRepo;
import dev.findfirst.users.repository.VerificationTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

  private final UserRepo userRepo;
  private final VerificationTokenRepository tokenRepository;
  private final PasswordTokenRepository passwordTokenRepository;
  private final RefreshTokenService refreshTokenService;
  private final PasswordEncoder passwdEncoder;
  private final TokenService ts;
  private final UserContext ut;
  private final BookmarkService bookmarkService;
  private final TagService tagService;

  @Value("${findfirst.upload.location}")
  private String uploadLocation;

  public User getUserInfo() throws NoUserFoundException {
    return getUserById(ut.getUserId()).orElseThrow(NoUserFoundException::new);
  }

  public User getUserByEmail(String email) throws NoUserFoundException {
    return userRepo.findByEmail(email).orElseThrow(NoUserFoundException::new);
  }

  public User getUserByUsername(String username) throws NoUserFoundException {
    return userRepo.findByUsername(username).orElseThrow(NoUserFoundException::new);
  }

  public boolean getUserExistByUsername(String username) {
    return userRepo.existsByUsername(username);
  }

  public Optional<User> getUserById(int id) {
    return userRepo.findById(id);
  }

  public boolean getUserExistEmail(String email) {
    return userRepo.existsByEmail(email);
  }

  public User saveUser(User user) {
    return userRepo.save(user);
  }

  public void deleteUser(User user) {
    userRepo.delete(user);
  }

  public void changeUserPassword(User user, String password) {
    user.setPassword(encodePassword(password));
    saveUser(user);
  }

  public void changeUserPhoto(User user, MultipartFile file)
      throws IllegalStateException, IOException {

    // Create directory for uploads
    File uploadDir = new File(uploadLocation);
    if (!uploadDir.exists()) {
      uploadDir.mkdirs();
    }

    // Delete old photo (if exists)
    removeUserPhoto(user);

    // Save new photo
    String fileName = "userphoto_" + UUID.randomUUID() + ".png";
    File destinationFile = Path.of(uploadLocation, fileName).normalize().toFile();

    file.transferTo(destinationFile);
    String userPhoto = uploadLocation + fileName;

    log.info("Changing profile picture for user ID {}: {}", user.getUserId(), userPhoto);
    user.setUserPhoto(userPhoto);
    saveUser(user);
  }

  public void removeUserPhoto(User user) {
    String userPhoto = user.getUserPhoto();
    if (userPhoto != null && !userPhoto.isBlank()) {
      log.debug("Existing User photo {}", userPhoto);
      File photoFile = new File(userPhoto);
      if (photoFile.exists()) {
        log.info("Removing profile picture for user ID {}: {}", user.getUserId(), userPhoto);
        try {
          Files.delete(photoFile.toPath());
        } catch (IOException e) {
          log.error(e.getMessage());
        }
        user.setUserPhoto(null);
        saveUser(user);
      }
    }
  }

  public String createVerificationToken(User user) {
    String token = UUID.randomUUID().toString();
    Token verificationToken = new Token(AggregateReference.to(user.getUserId()), token);
    tokenRepository.save(verificationToken);
    return token;
  }

  public Token getVerificationToken(String verificationToken) {
    return tokenRepository.findByTokenVal(verificationToken);
  }

  public String createResetPwdToken(User user) {
    String token = UUID.randomUUID().toString();
    Token pwdToken = new Token(AggregateReference.to(user.getUserId()), token);
    log.debug("creating token for: {}", user);
    var old = passwordTokenRepository.findByUser(user);
    if (old != null) {
      passwordTokenRepository.delete(old);
    }
    passwordTokenRepository.save(pwdToken);
    log.debug("saving token");
    return token;
  }

  public Token getPasswordToken(String pwdToken) {
    return passwordTokenRepository.findByTokenVal(pwdToken);
  }

  public User getUserFromPasswordToken(String pwdToken) throws NoUserFoundException {
    var userId = passwordTokenRepository.findByTokenVal(pwdToken).getUser().getId();
    if (userId == null) {
      throw new NoUserFoundException();
    }
    return userRepo.findById(userId).orElseThrow(NoUserFoundException::new);
  }

  public String encodePassword(String password) {
    return passwdEncoder.encode(password);
  }

  public User createNewUserAccount(SignupRequest signupRequest)
      throws UserNameTakenException, EmailAlreadyRegisteredException, UnexpectedException {
    if (getUserExistByUsername(signupRequest.username())) {
      throw new UserNameTakenException();
    }

    if (getUserExistEmail(signupRequest.email())) {
      throw new EmailAlreadyRegisteredException();
    }

    // Create new user's account
    AggregateReference<Role, Integer> ref = AggregateReference.to(0);
    User user = new User(null, false, signupRequest.username(), signupRequest.email(),
        encodePassword(signupRequest.password()), "", ref);

    // create a new tenant
    try {
      log.debug("new user - saving user");
      return saveUser(user);
    } catch (Exception e) {
      // If any exception occurs we should delete the records that were just made.
      log.error(e.getMessage());
      deleteUser(user);
      throw new UnexpectedException("Unexpected error occured during signup, try again");
    }
  }

  /**
   * Wrapper for the Token Service.
   * 
   * @param userId the userId for token generation.
   */
  public String generateTokenFromUser(int userId) {
    return ts.generateTokenFromUser(userId);
  }

  public SigninTokens signinUser(String authorization) throws NoUserFoundException {
    String base64Credentials = authorization.substring("Basic".length()).trim();
    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
    String credentials = new String(credDecoded, StandardCharsets.UTF_8);
    // credentials = username:password
    final String[] values = credentials.split(":", 2);
    if (values.length == 2) {
      User user = getUserByUsername(values[0]);
      final RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
      String jwt = generateTokenFromUser(user.getUserId());
      return new SigninTokens(jwt, refreshToken.getToken());
    }
    throw new NoUserFoundException();
  }

  @Transactional
  public void deleteUserAccount() throws NoUserFoundException {
    Integer id = ut.getUserId();
    User user = getUserById(id)
            .orElseThrow(NoUserFoundException::new);
    log.info("Deleting user account"+id);

    bookmarkService.deleteAllBookmarks();
    tagService.deleteAllTags();
    userRepo.delete(user);

    log.info("Successfully deleted user "+id);
  }
}
