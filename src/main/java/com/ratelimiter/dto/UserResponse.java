package com.ratelimiter.dto;

import com.ratelimiter.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class UserResponse {

    private long id;
    private String username;
    private String email;
    private String apiKey;
    private String tier;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;


    public static UserResponse fromUser(User user) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setApiKey(user.getApiKey());
        userResponse.setTier(user.getTier());
        userResponse.setEnabled(user.isEnabled());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());
        return userResponse;
    }

    public static UserResponse fromUserWithoutApiKey(User user) {
        UserResponse userResponse = fromUser(user);
        userResponse.setApiKey(maskApiKey(user.getApiKey()));
        return userResponse;
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 10) {
            return "***";
        }
        return apiKey.substring(0, 7) + "..." + apiKey.substring(apiKey.length() - 4);
    }

}
