package dev.findfirst.users.repository;

import java.util.Optional;

import dev.findfirst.users.model.user.User;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends CrudRepository<User, Integer> {

  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Modifying
  @Query("DELETE FROM bookmark b WHERE b.user_id= :userId")
  int deleteAllUserBookmarks(@Param("userId") Integer userId);

  @Modifying
  @Query("DELETE FROM bookmark_tag bt USING bookmark WHERE bt.bookmark_id = bookmark.id AND bookmark.user_id = :userId")
  int deleteAllBookmarkTags(@Param("userId") Integer id);

  @Modifying
  @Query("DELETE FROM tag t WHERE t.user_id= :userId")
  int deleteAllUserTags(@Param("userId") Integer id);

}
