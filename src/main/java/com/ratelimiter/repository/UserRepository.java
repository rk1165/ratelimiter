package com.ratelimiter.repository;

import com.ratelimiter.model.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    @Query("SELECT * FROM users WHERE api_key = :apiKey")
    Optional<User> findByApiKey(@Param("apiKey") String apiKey);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findByTier(String tier);

    long countByTier(String tier);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

}
