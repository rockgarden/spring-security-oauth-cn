# [使用 Keycloak 的自定义协议映射器](https://www.baeldung.com/keycloak-custom-protocol-mapper)

1. 简介

    Keycloak 是一个开源身份和访问管理（IAM）解决方案，主要针对现代应用和服务。当用户通过 Keycloak 进行身份验证时，服务器发出的令牌包含关于已通过身份验证的用户和令牌所指向的客户端的重要信息。

    Keycloak 的令牌包含一些默认属性，如 iss（发行者）、exp（过期时间）、sub（主题）和 aud（受众）。但很多时候，这些属性还不够，我们可能需要在令牌中添加一些额外的信息。在这种情况下，我们需要使用协议映射器。

    在本教程中，我们将演示如何向 Keycloak 授权服务器添加自定义协议映射器。

2. 协议映射器

    Keycloak 令牌只是一个 JSON 对象，其中包含一组声明，通常经过数字签名。让我们来看一个令牌有效载荷及其标准化声明集合的示例：

    ```json
    {
    "exp": 1680679982,
    "iat": 1680679682,
    "jti": "bebf7b2c-f813-47be-ad63-0ca6323bba19",
    "iss": "http://192.168.198.128:8085/auth/realms/baeldung",
    "aud": "account",
    "sub": "648b0687-c002-441d-b797-0003b30168ed",
    "typ": "Bearer",
    "azp": "client-app",
    "acr": "1",
    ...
    "scope": "email profile",
    "clientId": "client-app",
    "clientHost": "192.168.198.1",
    "email_verified": false,
    "preferred_username": "service-account-client-app",
    "clientAddress": "192.168.198.1"
    }
    ```

    协议映射器会将电子邮件地址等项目映射到身份和访问令牌中的特定主张。我们可以通过向客户端添加协议映射器来定制令牌中的权利要求和具体细节。

3. 设置 Keycloak 服务器

    在本教程中，我们将使用 Keycloak 独立版本。

    让我们在 Keycloak 实例中添加一个名为 baeldung 的新领域和一个名为 client-app 的新客户端。

    除了客户端身份验证和服务账户角色字段，我们将保留所有默认值。

    服务帐户角色字段使该客户端支持客户端凭据授权。

4. 自定义协议映射器的实现

    现在我们已经设置好 Keycloak 服务器，我们将创建一个自定义协议映射器并在 Keycloak 服务器中进行配置。

4.1. 依赖关系

我们的自定义协议映射器是一个创建 JAR 文件的常规 Maven 项目。

首先，让我们在 pom.xml 中声明 keycloak-core、keycloak-server-spi、keycloak-server-spi-private 和 keycloak-services 依赖项：

```xml
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-core</artifactId>
    <scope>provided</scope>
    <version>${keycloak.version}</version>
</dependency>
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-server-spi</artifactId>
    <scope>provided</scope>
    <version>${keycloak.version}</version>
</dependency>
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-server-spi-private</artifactId>
    <scope>provided</scope>
    <version>${keycloak.version}</version>
</dependency>
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-services</artifactId>
    <scope>provided</scope>
    <version>${keycloak.version}</version>
</dependency>
```

4.2. 扩展 AbstractOIDCProtocolMapper 类

现在让我们创建协议映射器。为此，我们扩展 AbstractOIDCProtocolMapper 类并实现所有抽象方法：

![CustomProtocolMapper](/oauth-rest/keycloak-custom-providers/src/main/java/com/baeldung/auth/provider/mapper/CustomProtocolMapper.java)

我们选择"custom-protocol-mapper"作为提供者 ID，也就是令牌映射器的 ID。我们需要这个 ID 来配置 Keycloak 服务器中的协议映射器。

主要方法是 setClaim()。它将我们的数据添加到令牌中。我们的 setClaim() 实现只是将 Baeldung 文本添加到令牌中。

getDisplayType() 和 getHelpText() 方法用于管理控制台。getDisplayType() 方法定义了列出协议映射器时在管理控制台中显示的文本。getHelpText() 方法是我们选择协议映射器时显示的工具提示文本。

4.3. 将所有内容整合在一起

我们不能忘记创建一个服务定义文件并将其添加到项目中。该文件应命名为 org.keycloak.protocol.ProtocolMapper，并放置在最终 JAR 的 META-INF/services 目录中。此外，该文件的内容是自定义协议映射器实现的完全限定类名：

`com.baeldung.auth.provider.mapper.CustomProtocolMapper`

> 文件路径：src/resources/META-INF/services

现在，项目可以运行了。首先，我们使用 Maven 安装命令创建一个 JAR 文件：

`mvn clean install`

然后，将 JAR 文件添加到 Keycloak 的 providers 目录中，将其部署到 Keycloak 中。之后，我们必须重启服务器，用 JAR 文件中的实现更新服务器的提供程序注册表：

`$ bin/kc.sh start-dev`

从控制台输出中可以看到，Keycloak 注册了我们的自定义协议映射器：

```log
Updating the configuration and installing your custom providers, if any. Please wait.
2023-04-05 14:55:42,588 WARN  [org.keycloak.services] (build-108) KC-SERVICES0047: custom-protocol-mapper (com.baeldung.auth.provider.CustomProtocolMapper) is implementing the internal SPI protocol-mapper.
```

最后，如果我们进入 Keycloak 管理控制台的提供商信息(Provider info)页面，就会看到我们的自定义协议映射器。

> KC 25 位于 \Server info\Providers

现在，我们可以配置服务器使用自定义协议映射器了。

4.4. 配置客户端

在 Keycloak 中，我们可以使用管理面板添加自定义申明(add custom claims using the admin panel)。为此，我们需要进入管理控制台的客户端。回想一下，我们之前创建了客户端 client-app。然后，我们导航到客户端作用域(Client scopes)选项卡。

现在，让我们点击 client-app-dedicated，进入其 Add mapper By 配置，创建一个新映射。

在这里，我们需要为自定义映射器输入映射器类型。我们将输入 "Custom Token Mapper"（自定义令牌映射器），这是我们在 CustomProtocolMapper 类中的 getDisplayType() 方法中使用的值：

自定义协议映射器
接下来，我们给映射器命名，并输入 