# [为Keycloak定制主题](https://www.baeldung.com/spring-keycloak-custom-themes)

1. 概述

    Keycloak 是一个开源身份和访问管理（IAM）解决方案，可用作第三方授权服务器来管理网络或移动应用程序的身份验证和授权要求。

    在本教程中，我们将重点介绍如何自定义 Keycloak 服务器的主题，以便为面向最终用户的网页提供不同的外观和感觉。

2. 自定义嵌入式 Keycloak 服务器

    根据定义，嵌入式 Keycloak 服务器意味着我们的机器上没有安装 IAM 提供程序。因此，我们需要在源代码中保留所有必要的工件，如 themes.properties 和 CSS 文件。

    我们可以将它们放在 Spring Boot 项目的 src/main/resources/themes 文件夹中。

    当然，由于主题结构的文件是相同的，我们自定义它们的方式也与独立服务器相同。

    不过，我们需要配置一些东西，以指示 Keycloak 服务器从我们的自定义主题中获取内容。

    1. 修改领域定义文件

        首先，让我们看看如何为给定的主题类型指定自定义主题。

        回想一下，在独立服务器的情况下，我们在管理控制台的 "Themes" 页面上，从 "Account Theme" 下拉菜单中添加了自定义主题。

        为了达到同样的效果，我们需要在我们的领域定义文件 baeldung-realm.json 中添加一行：

        `"accountTheme": "custom",`

        这就是我们需要的全部内容；所有其他类型（如登录和电子邮件）仍将遵循标准主题。

    2. 重定向到自定义主题目录

        接下来，让我们看看如何告诉服务器自定义主题的位置。

        我们可以通过以下几种方式来实现。

        在启动嵌入式服务器的 Boot App 时，我们可以将主题目录指定为 VM 参数：

        `mvn spring-boot:run -Dkeycloak.theme.dir=/src/main/resources/themes`

        为了以编程方式实现同样的目的，我们可以在 @SpringBootApplication 类中将其设置为系统属性：

        ```java
        public static void main(String[] args) throws Exception {
            System.setProperty("keycloak.theme.dir","src/main/resources/themes");
            SpringApplication.run(JWTAuthorizationServerApp.class, args);
        }
        ```

        或者，我们可以在 keycloak-server.json 中更改服务器配置：

        ```json
        "theme": {
            ....
            "welcomeTheme": "custom",
            "folder": {
                "dir": "src/main/resources/themes"
            }
        },
        ```

        值得注意的是，在这里我们还添加了 welcomeTheme 属性，以便对欢迎页面进行自定义。

        如前所述，CSS 文件和图片的所有其他更改均保持不变。

        要查看欢迎页面的更改，我们需要启动嵌入式服务器并导航至 <http://localhost:8083/auth>。

        账户管理页面位于 <http://localhost:8083/auth/realms/baeldung/account>，我们可以使用以下凭据访问：`john@test.com/123`。

3. 总结

    在本教程中，我们了解了如何在嵌入式 Keycloak 服务器中实现同样的功能。
