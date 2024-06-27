# [在 Spring Boot 应用程序中嵌入 Keycloak](https://www.baeldung.com/keycloak-embedded-in-spring-boot-app)

1. 概述

    Keycloak 是由 RedHat 管理、JBoss 用 Java 开发的开源身份和访问管理解决方案。

    在本教程中，我们将学习如何在 Spring Boot 应用程序中嵌入 Keycloak 服务器。这样就能轻松启动预配置的 Keycloak 服务器。

    Keycloak 也可以作为独立服务器运行，但需要下载并通过管理控制台进行设置。

2. Keycloak 预配置

    首先，让我们了解一下如何预配置 Keycloak 服务器。

    服务器包含一组域，每个域都是用户管理的独立单元。要进行预配置，我们需要指定一个 JSON 格式的领域定义文件。

    可以使用 Keycloak [管理控制台](https://www.keycloak.org/docs/latest/server_admin/)配置的所有内容都将保存在该 JSON 文件中。

    我们的授权服务器将使用 baeldung-realm.json 进行预配置。让我们看看文件中的一些相关配置：

    - users：我们的默认用户是 <john@test.com> 和 <mike@other.com>；他们的凭据也在这里
    - clients：我们将定义一个 ID 为 newClient 的客户端
    - standardFlowEnabled：设置为 true 以激活 newClient 的授权代码流
    - redirectUris：此处列出了 newClient 在成功验证后服务器将重定向到的 URL
    - webOrigins：设置为 "+"，以允许为所有列为 redirectUris 的 URL 提供 CORS 支持

    Keycloak 服务器默认会发出 JWT 标记，因此无需为此进行单独配置。接下来让我们看看 Maven 配置。

3. Maven 配置

    由于我们将在 Spring Boot 应用程序中嵌入 Keycloak，因此无需单独下载。

    相反，我们将设置以下依赖项：

    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>        
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    ```

    请注意，这里我们使用的是 Spring Boot 3.1.1 版本。我们添加了 spring-boot-starter-data-jpa 和 H2 作为持久化依赖。其他 springframework.boot 依赖项用于网络支持，因为我们还需要将 Keycloak 授权服务器和管理控制台作为网络服务运行。

    我们还需要 Keycloak 和 RESTEasy 的以下依赖项：

    ```xml
    <dependency>
        <groupId>org.jboss.resteasy</groupId>
        <artifactId>resteasy-jackson2-provider</artifactId>
        <version>6.2.4.Final</version>
    </dependency>

    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-dependencies-server-all</artifactId>
        <version>24.0.4</version>
        <type>pom</type>
    </dependency> 

    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-crypto-default</artifactId>
        <version>24.0.4</version>
    </dependency>

    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-admin-ui</artifactId>
        <version>24.0.4</version>
    </dependency>

    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-services</artifactId>
        <version>24.0.4</version>
    </dependency>
    
    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-rest-admin-ui-ext</artifactId>
        <version>24.0.4</version>
    </dependency>
    ```

    查看 Maven 网站，获取 Keycloak 和 RESTEasy 的最新版本。

4. 嵌入式 Keycloak 配置

    现在让我们为授权服务器定义 Spring 配置：

    ![EmbeddedKeycloakConfig.java](./src/main/java/com/baeldung/auth/config/EmbeddedKeycloakConfig.java)

    注意：不用担心编译错误，我们稍后会定义 EmbeddedKeycloakRequestFilter 类。

    正如我们在这里看到的，我们首先将 Keycloak 配置为 JAX-RS 应用程序，并使用 KeycloakServerProperties 来持久化存储我们的领域定义文件中指定的 Keycloak 属性。然后，我们添加了一个会话管理过滤器，并模拟了一个 JNDI 环境，以使用 Spring/datasource 也就是我们的内存 H2 数据库。

5. KeycloakServerProperties

    现在让我们看看刚才提到的 KeycloakServerProperties：

    ```java
    @ConfigurationProperties(prefix = "keycloak.server")
    public class KeycloakServerProperties {
        String contextPath = "/auth";
        String realmImportFile = "baeldung-realm.json";
        AdminUser adminUser = new AdminUser();

        // getters and setters

        public static class AdminUser {
            String username = "admin";
            String password = "admin";

            // getters and setters        
        }
    }
    ```

    我们可以看到，这是一个简单的 POJO，用于设置 contextPath、adminUser 和 realm 定义文件。

6. 嵌入式 KeycloakApplication

    接下来，让我们看看使用我们之前设置的配置来创建境界的类：

    ![EmbeddedKeycloakApplication.java](./src/main/java/com/baeldung/auth/config/EmbeddedKeycloakApplication.java)

7. 自定义平台实现

    如前所述，Keycloak 由 RedHat/JBoss 开发。因此，它提供了在 Wildfly 服务器上部署应用程序或作为 Quarkus 解决方案的功能和扩展库。

    在本例中，我们将不再使用这些替代方案，因此，我们必须为某些特定平台的接口和类提供自定义实现。

    例如，在我们刚刚配置的 EmbeddedKeycloakApplication 中，我们首先加载了 Keycloak 的服务器配置 keycloak-server.json，并使用了抽象 JsonConfigProviderFactory 的空子类：

    `public class RegularJsonConfigProviderFactory extends JsonConfigProviderFactory { }`

    然后，我们扩展了 KeycloakApplication，创建了两个域：master 和 baeldung。它们是根据领域定义文件 baeldung-realm.json 中指定的属性创建的。

    如您所见，我们使用 KeycloakSession 来执行所有事务，为使其正常工作，我们必须创建一个自定义抽象请求过滤器 AbstractRequestFilter（[EmbeddedKeycloakRequestFilter](./src/main/java/com/baeldung/auth/config/EmbeddedKeycloakRequestFilter.java)），并在 EmbeddedKeycloakConfig 文件中使用 KeycloakSessionServletFilter 为此设置一个 Bean。

    此外，我们还需要几个自定义提供程序，这样我们就能拥有自己的 [org.keycloak.common.util.ResteasyProvider](./src/main/java/com/baeldung/auth/config/Resteasy3Provider.java) 和 [org.keycloak.platform.PlatformProvider](./src/main/java/com/baeldung/auth/config/SimplePlatformProvider.java) 实现，而无需依赖外部依赖。

    请注意，keycloack-server.json 文件中定义的值只是占位符。除非键名以"env.*"前缀开头，否则无法通过环境变量覆盖它们。

    要使用环境变量覆盖变量（无论前缀如何），必须实现自定义的 ProviderFactory 并覆盖 getProperties 方法。

    ```java
    public class RegularJsonConfigProviderFactory extends JsonConfigProviderFactory {
        @Override
        protected Properties getProperties() {
            return new SystemEnvProperties(System.getenv());
        }
    }
    ```

    重要的是，有关这些自定义提供程序的信息应包含在项目的 META-INF/services 文件夹中，以便在运行时获取。

8. 将所有内容整合在一起

    正如我们所看到的，Keycloak 大大简化了应用程序端所需的配置。无需以编程方式定义数据源或任何安全配置。

    要将这一切整合在一起，我们需要定义 Spring 和 Spring Boot 应用程序的配置。

    1. 应用程序设置

        我们将使用简单的 YAML 来定义 Spring 配置：

        ![application.yml](./src/main/resources/application.yml)

    2. Spring Boot 应用程序

        最后，这里是 Spring Boot 应用程序：

        ![AuthorizationServerApp](./src/main/java/com/baeldung/auth/AuthorizationServerApp.java)

        值得注意的是，这里我们启用了 KeycloakServerProperties 配置，以便将其注入 ApplicationListener Bean。

        运行该类后，我们可以访问授权服务器的欢迎页面 <http://localhost:8083/auth/>。

    3. 可执行 JAR

        我们还可以[创建一个可执行 jar 文件](https://www.baeldung.com/deployable-fat-jar-spring-boot)来打包和运行应用程序：

        ```xml
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <mainClass>com.baeldung.auth.AuthorizationServerApp</mainClass>
                <requiresUnpack>
                    <dependency>
                        <groupId>org.keycloak</groupId>
                        <artifactId>keycloak-model-jpa</artifactId>
                    </dependency>
                </requiresUnpack>
            </configuration>
        </plugin>
        ```

        在这里，我们指定了主类，还指示 Maven 解包 Keycloak 的一些依赖项。这将在运行时解压胖 jar 中的库，现在我们可以使用标准的 `java -jar <artifact-name>` 命令运行应用程序了。

        授权服务器的欢迎页面现在可以访问了，如前所述。

9. 总结

    在本快速教程中，我们了解了如何在 Spring Boot 应用程序中嵌入 Keycloak 服务器。

    该实现的最初想法由 Thomas Darimont 提出，可在 [embedded-spring-boot-keycloak-server](https://github.com/thomasdarimont/embedded-spring-boot-keycloak-server) 项目中找到。
