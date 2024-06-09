## Spring Security OAuth legacy project

### Relevant information:

1. `oauth-authorization-server-legacy` - the Spring based Authorization Server
2. There are two users registered in the Authorization Server:
   1. john / 123
   2. tom / 111
3. `oauth-resource-server-legacy-1` - the Resource Server based on old stack
4. `oauth-resource-server-legacy-2` - the secondary Resource Server 
5. `oauth-ui-authorization-code-angular-legacy` - Authorization Code Grant UI Module - using Angular 7, hitting oauth-resource-server-legacy-1
6. `oauth-ui-implicit-angular-legacy` - another version of the Implicit Grant UI Module - using Angular 7, hitting oauth-resource-server-legacy-1
7. `oauth-ui-password-angular-legacy` - another version of the Password Grant UI Module - using Angular 7, hitting oauth-resource-server-legacy-1
8. `oauth-ui-implicit-angularjs-legacy` - Implicit Grant UI Module - using AngularJS, hitting oauth-resource-server-legacy-1
9. `oauth-ui-password-angularjs-legacy` - Password Grant UI Module - using AngularJS, hitting oauth-resource-server-legacy-1
10. `oauth-zuul-gateway` - Zuul Gateway, hitting oauth-resource-server-legacy-1
11. The module uses the legacy OAuth stack with Java 8


## Relevant Articles: 

- [Spring REST API + OAuth2 + Angular (using the Spring Security OAuth legacy stack)](https://www.baeldung.com/rest-api-spring-oauth2-angular-legacy)
- [Using JWT with Spring Security OAuth (legacy stack)](http://www.baeldung.com/spring-security-oauth-jwt-legacy)
- [OAuth2 for a Spring REST API – Handle the Refresh Token in AngularJS (legacy OAuth stack)](http://www.baeldung.com/spring-security-oauth2-refresh-token-angular-js-legacy)
- [Logout in an OAuth Secured Application (using the Spring Security OAuth legacy stack)](http://www.baeldung.com/logout-spring-security-oauth-legacy)
- [Spring Security OAuth2 – Simple Token Revocation (using the Spring Security OAuth legacy stack)](http://www.baeldung.com/spring-security-oauth-revoke-tokens)
- [OAuth2.0 and Dynamic Client Registration (using the Spring Security OAuth legacy stack)](http://www.baeldung.com/spring-security-oauth-dynamic-client-registration)
- [Testing an OAuth Secured API with Spring MVC (using the Spring Security OAuth legacy stack)](https://www.baeldung.com/oauth-api-testing-with-spring-mvc)
- [OAuth2 Remember Me with Refresh Token (using the Spring Security OAuth legacy stack)](http://www.baeldung.com/spring-security-oauth2-remember-me)
- [New in Spring Security OAuth2 – Verify Claims](http://www.baeldung.com/spring-security-oauth-2-verify-claims)
- [Front-End App with Spring Security OAuth – Authorization Code Flow](http://www.baeldung.com/spring-security-oauth-authorization-code-flow)
- [Setting Up Swagger 2 with a Spring REST API Using Springfox](https://www.baeldung.com/swagger-2-documentation-for-spring-rest-api)
