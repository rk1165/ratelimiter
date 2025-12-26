### Distributed Rate Limiter

- What should my application offer:
  - A user should be to signup, login, logout using username and password 
  - They should be assigned by default FREE tier or else they should be given option to select from premium/enterprise on choice of which they are assigned an API key.
  - All the APIs can be marked using RateLimit annotation which has tier and token consumption configured.
  - Rate Limit should apply in a staggered manner 
    - If a user is on free tier they should be able to hit the api for free limit if they are on premium tier they should be able to hit it up using enterprise tier... and son on
  - If a non-registered user hits our API they should be rate limited based on IP address
  - First we should rate limit by API key, then by user ID , then by IP address 
- Suppose I have an API `/api/v1/joke`
- 