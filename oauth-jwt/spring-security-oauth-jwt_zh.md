# [将 JWT 与 Spring Security OAuth 结合使用](https://www.baeldung.com/spring-security-oauth-jwt)

1. 概述

    在本教程中，我们将讨论如何让 Spring Security OAuth2 实现使用 JSON Web 标记。

    在本 OAuth 系列中，我们还将继续讨论 Spring REST API + OAuth2 + Angular [文章](https://www.baeldung.com/rest-api-spring-oauth2-angular)。

2. OAuth2 授权服务器

    之前，Spring Security OAuth 栈提供了将授权服务器设置为 Spring 应用程序的可能性。然后，我们必须将其配置为使用 JwtTokenStore，这样才能使用 JWT 标记。

    不过，OAuth 协议栈已被 Spring 弃用，现在我们将使用 Keycloak 作为授权服务器。

    因此，这次我们将把授权服务器设置为 [Spring Boot 应用程序中的嵌入式 Keycloak 服务器](https://www.baeldung.com/keycloak-embedded-in-a-spring-boot-application)。它默认情况下会发出 JWT 标记，因此无需进行任何其他配置。

3. 资源服务器

    现在让我们看看如何配置资源服务器以使用 JWT。

    我们将在 application.yml 文件中进行配置：

    ```yml
    server: 
    port: 8081
    servlet: 
        context-path: /resource-server

    spring:
    jpa:
        defer-datasource-initialization: true
    security:
        oauth2:
        resourceserver:
            jwt:
            issuer-uri: http://localhost:8083/auth/realms/baeldung
            jwk-set-uri: http://localhost:8083/auth/realms/baeldung/protocol/openid-connect/certs
    ```

    JWT 包含令牌中的所有信息，因此资源服务器需要验证令牌的签名，以确保数据未被修改。jwk-set-uri 属性包含服务器可用于此目的的公钥。

    issuer-uri 属性指向基本授权服务器 URI，也可用于验证 iss 声明，作为额外的安全措施。

    此外，如果未设置 jwk-set-uri 属性，资源服务器将尝试使用 issuer-uri 从授权服务器元数据[端点](https://tools.ietf.org/html/rfc8414#section-3)确定此密钥的位置。

    需要注意的是，添加 issuer-uri 属性后，我们必须先运行授权服务器，然后才能启动资源服务器应用程序。

    现在让我们看看如何使用 Java 配置来配置 JWT 支持：

    main/.jwt.spring/SecurityConfig.java

    在这里，我们覆盖了默认的 Http 安全配置；我们需要明确指定，我们希望它作为资源服务器运行，并且我们将分别使用 oauth2ResourceServer() 和 jwt() 方法来使用 JWT 格式的访问令牌。

    上述 JWT 配置是 Spring Boot 默认实例为我们提供的配置。这也可以自定义，我们很快就会看到。

4. 令牌中的自定义声明

    现在，让我们建立一些基础架构，以便在授权服务器返回的访问令牌中添加一些自定义声明。框架提供的标准声明都很好，但大多数情况下，我们需要在令牌中添加一些额外的信息，以便在客户端使用。

    让我们举一个自定义权利要求（组织）的例子，它将包含给定用户的组织名称。

    1. 授权服务器配置

        为此，我们需要在领域定义文件 baeldung-realm.json 中添加一些配置：

        - 在用户 <john@test.com> 中添加组织属性：

            ```json
            "attributes" : {
                "organization" : "baeldung"
            },
            ```

        - 在 jwtClient 配置中添加名为 organization 的协议映射器：

            ```json
            "protocolMappers": [{
                "id": "06e5fc8f-3553-4c75-aef4-5a4d7bb6c0d1",
                "name": "organization",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-attribute-mapper",
                "consentRequired": false,
                "config": {
                    "userinfo.token.claim": "true",
                    "user.attribute": "organization",
                    "id.token.claim": "true",
                    "access.token.claim": "true",
                    "claim.name": "organization",
                    "jsonType.label": "String"
                }
            }],
            ```

        对于独立的 Keycloak 设置，也可以使用管理控制台完成。

        请务必记住，上述 JSON 配置是 Keycloak 特有的，其他 OAuth 服务器可能会有所不同。

        使用新配置后，我们将在 <john@test.com> 的令牌有效载荷中获得一个额外的属性：organization = baeldung：

        ```json
        {
            jti: "989ce5b7-50b9-4cc6-bc71-8f04a639461e"
            exp: 1585242462
            nbf: 0
            iat: 1585242162
            iss: "http://localhost:8083/auth/realms/baeldung"
            sub: "a5461470-33eb-4b2d-82d4-b0484e96ad7f"
            typ: "Bearer"
            azp: "jwtClient"
            auth_time: 1585242162
            session_state: "384ca5cc-8342-429a-879c-c15329820006"
            acr: "1"
            scope: "profile write read"
            organization: "baeldung"
            preferred_username: "john@test.com"
        }
        ```

    2. 在 Angular 客户端中使用访问令牌

        接下来，我们要在 Angular 客户端程序中使用令牌信息。为此，我们将使用 angular2jwt 库。

        我们将在 AppService 中使用组织声明，并添加一个 getOrganization 函数：

        ```java
        getOrganization(){
            var token = Cookie.get("access_token");
            var payload = this.jwtHelper.decodeToken(token);
            this.organization = payload.organization; 
            return this.organization;
        }
        ```

        该函数使用 angular2-jwt 库中的 JwtHelperService 来解码访问令牌并获取我们的自定义声明。现在我们要做的就是在 AppComponent 中显示它：

        ```java
        @Component({
            selector: 'app-root',
            template: `<nav class="navbar navbar-default">
                <div class="container-fluid">
                    <div class="navbar-header">
                    <a class="navbar-brand" href="/">Spring Security Oauth - Authorization Code</a>
                    </div>
                </div>
                <div class="navbar-brand">
                    <p>{{organization}}</p>
                </div>
                </nav>
                <router-outlet></router-outlet>`
            })

            export class AppComponent implements OnInit {
            public organization = "";
            constructor(private service: AppService) { }  
            
            ngOnInit() {  
                this.organization = this.service.getOrganization();
            }  
        }
        ```

5. 在资源服务器中访问额外声明

    但我们如何在资源服务器端访问这些信息呢？

    5.1. 访问身份验证服务器声称

    这其实很简单，我们只需从 org.springframework.security.oauth2.jwt.Jwt 的 AuthenticationPrincipal 中提取它，就像在 UserInfoController 中提取其他属性一样：

    ```java
    @GetMapping("/user/info")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt principal) {
        Map<String, String> map = new Hashtable<String, String>();
        map.put("user_name", principal.getClaimAsString("preferred_username"));
        map.put("organization", principal.getClaimAsString("organization"));
        return Collections.unmodifiableMap(map);
    }
    ```

    5.2. 添加/删除/重命名声称的配置

    现在，如果我们想在资源服务器端添加更多索赔怎么办？或者删除或重命名某些索赔？

    比方说，我们要修改从身份验证服务器传入的组织声明，以获取大写值。但是，如果用户不存在组织声明，我们就需要将其值设置为未知。

    为此，我们需要添加一个实现转换器接口的类，并使用 MappedJwtClaimSetConverter 转换声称：

    main/.jwt.spring/OrganizationSubClaimAdapter.java

    然后，我们需要在 SecurityConfig 类中添加自己的 JwtDecoder 实例，覆盖 Spring Boot 提供的 JwtDecoder 实例，并将 OrganizationSubClaimAdapter 设置为其索赔转换器：

    ```java
    @Bean
    public JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(
        properties.getJwt().getJwkSetUri()).build();
        
        jwtDecoder.setClaimSetConverter(new OrganizationSubClaimAdapter());
        return jwtDecoder;
    }
    ```

    现在，当我们点击 /user/info API 获取用户 <mike@other.com> 时，我们将得到 UNKNOWN 组织。

    请注意，覆盖 Spring Boot 配置的默认 JwtDecoder Bean 时应小心谨慎，以确保仍然包含所有必要的配置。

6. 从 Java Keystore 加载密钥

    在之前的配置中，我们使用了授权服务器的默认公钥来验证令牌的完整性。

    我们也可以使用存储在 Java Keystore 文件中的密钥对和证书来完成签名过程。

    1. 生成 JKS Java 密钥存储文件

        让我们先使用命令行工具 keytool 生成密钥，更具体地说是 .jks 文件：

        ```shell
        keytool -genkeypair -alias mytest 
                            -keyalg RSA 
                            -keypass mypass 
                            -keystore mytest.jks 
                            -storepass mypass
        ```

        该命令将生成一个名为 mytest.jks 的文件，其中包含我们的密钥、公钥和私钥。

        同时确保 keypass 和 storepass 相同。

    2. 导出公钥

        接下来，我们需要从生成的 JKS 中导出公钥。我们可以使用以下命令来完成：

        `keytool -list -rfc --keystore mytest.jks | openssl x509 -inform pem -pubkey`

    3. Maven 配置

        我们不希望 maven 过滤过程选中 JKS 文件，因此要确保在 pom.xml 中排除它：

        ```xml
        <build>
            <resources>
                <resource>
                    <directory>src/main/resources</directory>
                    <filtering>true</filtering>
                    <excludes>
                        <exclude>*.jks</exclude>
                    </excludes>
                </resource>
            </resources>
        </build>
        ```

        如果我们使用的是 Spring Boot，我们需要确保通过 Spring Boot Maven 插件 addResources 将 JKS 文件添加到应用程序的类路径中：

        ```xml
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <addResources>true</addResources>
                    </configuration>
                </plugin>
            </plugins>
        </build>
        ```

    4. 授权服务器

        现在，我们将配置 Keycloak 以使用来自 mytest.jks 的 Keypair，方法是将其添加到 Realm 定义 JSON 文件的 KeyProvider 部分，如下所示：

        ```json
        {
        "id": "59412b8d-aad8-4ab8-84ec-e546900fc124",
        "name": "java-keystore",
        "providerId": "java-keystore",
        "subComponents": {},
        "config": {
            "keystorePassword": [ "mypass" ],
            "keyAlias": [ "mytest" ],
            "keyPassword": [ "mypass" ],
            "active": [ "true" ],
            "keystore": [
                    "src/main/resources/mytest.jks"
                ],
            "priority": [ "101" ],
            "enabled": [ "true" ],
            "algorithm": [ "RS256" ]
        }
        },
        ```

        在这里，我们将优先级设置为 101，高于授权服务器的任何其他 Keypair，并将 active 设置为 true。这样做是为了确保我们的资源服务器会从我们之前指定的 jwk-set-uri 属性中选择这个特定的 Keypair。

        再次说明，此配置是 Keycloak 特有的，其他 OAuth 服务器实现可能会有所不同。

7. 结论

    在这篇简短的文章中，我们重点介绍了如何设置 Spring Security OAuth2 项目以使用 JSON Web 标记。
