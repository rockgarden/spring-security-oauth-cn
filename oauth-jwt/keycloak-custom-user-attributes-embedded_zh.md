# [使用Keycloak自定义用户属性](https://www.baeldung.com/keycloak-custom-user-attributes)

1. 概述

    Keycloak 是第三方授权服务器，用于管理网络或移动应用程序的用户。

    它提供了一些默认属性，例如为任何给定用户存储的名字、姓氏和电子邮件。但很多时候，这些还不够，我们可能需要为应用程序添加一些额外的用户属性。

    在本教程中，我们将了解如何在 Keycloak 授权服务器中添加自定义用户属性，并在基于 Spring 的后端中访问它们。

    首先，我们将在嵌入式服务器上看到这一点。

2. 嵌入式服务器

    1. 添加自定义用户属性

        基本上，我们需要在这里执行相同的步骤，只是需要将它们作为[预配置](https://www.baeldung.com/keycloak-embedded-in-spring-boot-app#keycloak-config)保存到我们的领域定义文件 baeldung-realm.json。

        要在用户 <john@test.com> 中添加 DOB 属性，首先需要配置其属性：

        ```json
        "attributes" : {
            "DOB" : "1984-07-01"
        },
        ```

        然后为 DOB 添加协议映射器：

        ```json
        "protocolMappers": [
            {
            "id": "c5237a00-d3ea-4e87-9caf-5146b02d1a15",
            "name": "DOB",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-attribute-mapper",
            "consentRequired": false,
            "config": {
                "userinfo.token.claim": "true",
                "user.attribute": "DOB",
                "id.token.claim": "true",
                "access.token.claim": "true",
                "claim.name": "DOB",
                "jsonType.label": "String"
                }
            }
        ]
        ```

        这就是我们需要的全部内容。

        现在，我们已经看到了添加自定义用户属性的授权服务器部分，是时候看看[资源服务器](https://www.baeldung.com/spring-security-oauth-resource-server)如何访问用户的 DOB 了。

    2. 访问自定义用户属性

        在资源服务器端，自定义属性将作为 AuthenticationPrincipal 中的声称值提供给我们。

        让我们为此编写一个应用程序接口：

        ```java
        @RestController
        public class CustomUserAttrController {
            @GetMapping("/user/info/custom")
            public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt principal) {
                return Collections.singletonMap("DOB", principal.getClaimAsString("DOB"));
            }
        }
        ```

    3. 测试

        现在让我们使用 JUnit 进行测试。

        我们首先需要获取访问令牌，然后调用资源服务器上的 /user/info/custom API 端点：

        ```java
        @Test
        public void givenUserWithReadScope_whenGetUserInformationResource_thenSuccess() {
            String accessToken = obtainAccessToken("read");
            Response response = RestAssured.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .get(userInfoResourceUrl);

            assertThat(response.as(Map.class)).containsEntry("DOB", "1984-07-01");
        }
        ```

        正如我们所看到的，在这里我们验证了所获得的 DOB 值与我们在用户属性中添加的相同。

3. 总结

    在本教程中，我们学习了如何在 Keycloak 中为用户添加额外属性。

    我们在独立实例和嵌入实例中都看到了这一点。在这两种情况下，我们还了解了如何在后台通过 REST API 访问这些自定义声明。
