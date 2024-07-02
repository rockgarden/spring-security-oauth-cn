# [使用 Keycloak 的自定义用户提供程序](https://www.baeldung.com/java-keycloak-custom-user-providers)

1. 简介

    在本教程中，我们将介绍如何在 Keycloak（一种流行的开源身份管理解决方案）中添加自定义提供程序，以便与现有和/或非标准用户存储库一起使用。

2. 使用 Keycloak 的自定义提供程序概述

    开箱即用的 Keycloak 提供了一系列基于 SAML、OpenID Connect 和 OAuth2 等协议的标准集成。虽然这些内置功能相当强大，但有时还不够。一个常见的需求，尤其是当涉及到传统系统时，就是将这些系统中的用户集成到 Keycloak 中。为了适应这种情况和类似的集成场景，Keycloak 支持自定义提供程序的概念。

    自定义提供程序在 Keycloak 的架构中起着关键作用。对于登录流程、身份验证、授权等每个主要功能，都有相应的服务提供商接口。这种方法允许我们为任何这些服务插入自定义实现，然后 Keycloak 就会像使用自己的服务一样使用这些实现。

    1. 自定义提供程序的部署和发现

        自定义提供程序最简单的形式就是一个标准 jar 文件，其中包含一个或多个服务实现。启动时，Keycloak 会扫描其 classpath，并使用标准的 [java.util.ServiceLoader](https://www.baeldung.com/java-spi) 机制选择所有可用的提供程序。这意味着我们要做的就是在 jar 的 META-INF/services 文件夹中创建一个以我们要提供的特定服务接口命名的文件，并在其中写入我们的实现的完全匹配的名称。

        但是，我们能为 Keycloak 添加什么样的服务呢？如果我们进入 Keycloak 管理控制台的服务器信息(server info)页面，就会看到很多服务。

        在这个页面中，左边一栏对应的是某个服务提供商接口（Service Provider Interface，简称 SPI），右边一栏显示的是该 SPI 的可用提供商。

    2. 可用的 SPI

        Keycloak 的主要文档列出了以下 SPI：

        - org.keycloak.authentication.AuthenticatorFactory： 定义验证用户或客户端应用程序所需的操作和交互流
        - org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory： 允许我们创建自定义动作，以便 Keycloak 在到达 /auth/realms/master/login-actions/action-token 端点时执行这些动作。举例来说，标准密码重置流程背后就是这种机制。电子邮件中的链接包含这样一个动作令牌
        - org.keycloak.events.EventListenerProviderFactory： 创建监听 Keycloak 事件的提供程序。[EventType Javadoc](https://www.keycloak.org/docs-api/21.1.2/javadocs/org/keycloak/events/EventType.html) 页面包含提供程序可处理的可用事件自定义列表。使用此 SPI 的典型用途是创建审计数据库
        - org.keycloak.adapters.saml.RoleMappingsProvider： 将从外部身份提供程序接收的 SAML 角色映射到 Keycloak 的角色。这种映射非常灵活，允许我们在给定 "Realm" 的上下文中重命名、删除和/或添加角色。
        - org.keycloak.storage.UserStorageProviderFactory： 允许 Keycloak 访问自定义用户存储空间
        - org.keycloak.vault.VaultProviderFactory： 允许我们使用自定义保险库来存储特定领域的机密。其中包括加密密钥、数据库凭证等信息。

        现在，这个列表并没有涵盖所有可用的 SPI：它们只是最完善的记录，而且在实践中最有可能需要定制。

3. 自定义提供程序实现

    正如我们在本文介绍中提到的，我们的提供程序示例允许我们将 Keycloak 与只读的自定义用户存储库一起使用。例如，在我们的例子中，这个用户存储库只是一个带有少量属性的普通 SQL 表：

    ```sql
    create table if not exists users(
        username varchar(64) not null primary key,
        password varchar(64) not null,
        email varchar(128),
        firstName varchar(128) not null,
        lastName varchar(128) not null,
        birthDate DATE not null
    );
    ```

    为了支持自定义用户存储，我们必须实现 UserStorageProviderFactory SPI 并将其部署到现有的 Keycloak 实例中。

    这里的关键点是只读部分。我们的意思是，用户可以使用他们的凭据登录 Keycloak，但不能更改自定义存储中的任何信息，包括密码。但这并不是 Keycloak 的限制，因为它实际上支持双向更新。内置 LDAP 提供商就是支持此功能的一个很好的例子。

    1. 项目设置

        我们的自定义提供程序项目只是一个创建 jar 文件的普通 Maven 项目。为了避免将我们的提供程序编译-部署-重启(compile-deploy-restart)循环到普通 Keycloak 实例的耗时过程，我们将使用一个不错的技巧：将 Keycloak 作为测试时依赖项嵌入我们的项目。

        我们已经介绍过如何在 SpringBoot 应用程序中嵌入 Keycloack，因此在此不再赘述。通过采用这种技术，我们将获得更快的启动时间和热重载功能，为开发人员提供更流畅的体验。在此，我们将重用示例 SpringBoot 应用程序，直接从自定义提供程序运行测试，因此我们将把它添加为测试依赖：

        ```xml
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-model-legacy</artifactId>
            <version>22.0.0</version>
        </dependency>
        <dependency>
            <groupId>com.baeldung</groupId>
            <artifactId>oauth-authorization-server</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <scope>test</scope>
        </dependency>
        ```

        我们使用 22.0.0 版本的 keycloak-model-legacy Keycloak 依赖关系。

        但是，auth-authorization-server 依赖项必须从 Baeldung 的 Spring Security OAuth 资源库中本地构建。

    2. 实现 UserStorageProviderFactory

        让我们通过创建 UserStorageProviderFactory 实现来开始我们的提供程序，并让 Keycloak 可以发现它。

        该接口包含 11 个方法，但我们只需实现其中的两个：

        - getId()： 返回该提供程序的唯一标识符，Keycloak 将在其管理页面上显示该标识符。
        - create()： 返回实际的提供程序实现。

        Keycloak 会为每个事务调用 create() 方法，并将 KeycloakSession 和 ComponentModel 作为参数传递。在这里，事务指的是任何需要访问用户存储空间的操作。最典型的例子就是登录流程：在某个时刻，Keycloak 会调用给定 "Realm" 的每个已配置用户存储来验证凭证。因此，我们应该避免在此时执行任何代价高昂的初始化操作，因为 create() 方法会一直被调用。

        尽管如此，实现起来还是很简单的：

        ```java
        public class CustomUserStorageProviderFactory
        implements UserStorageProviderFactory<CustomUserStorageProvider> {
            @Override
            public String getId() {
                return "custom-user-provider";
            }

            @Override
            public CustomUserStorageProvider create(KeycloakSession ksession, ComponentModel model) {
                return new CustomUserStorageProvider(ksession,model);
            }
        }
        ```

        我们选择 "custom-user-provider" 作为我们的提供程序 ID，而我们的 create() 实现只是返回一个 UserStorageProvider 实现的新实例。现在，我们不能忘记创建一个服务定义文件并将其添加到项目中。该文件应命名为 org.keycloak.storage.UserStorageProviderFactory，并放置在最终 jar 的 META-INF/services 文件夹中。

        由于我们使用的是标准 Maven 项目，这意味着我们将把它添加到 src/main/resources/META-INF/services 文件夹中。

        该文件的内容只是 SPI 实现的全称：

        ```inf
        # SPI class implementation
        com.baeldung.auth.provider.user.CustomUserStorageProviderFactory
        ```

    3. UserStorageProvider 实现

        乍一看，UserStorageProvider 的实现并不像我们想象的那样。它只包含几个回调方法，其中没有一个与实际用户有关。这是因为 Keycloak 希望我们的提供程序也能实现其他支持特定用户管理方面的混合接口。

        Keycloak [文档](https://www.keycloak.org/docs/latest/server_development/index.html#provider-capability-interfaces)中提供了可用接口的完整列表，这些接口被称为 "提供程序能力"（Provider Capabilities）。对于一个简单的只读提供程序，我们需要实现的唯一接口就是 UserLookupProvider。它只提供查找功能，这意味着 Keycloak 会在需要时自动将用户导入其内部数据库。不过，原始用户的密码不会用于身份验证。为此，我们还需要实现 CredentialInputValidator。

        最后，一个常见的需求是在 Keycloak 的管理界面中显示自定义存储中的现有用户。这就需要我们实现另一个接口： UserQueryProvider 接口。这个接口添加了一些查询方法，并充当我们商店的 DAO。

        因此，考虑到这些要求，我们的实现应该是这样的：

        ```java
        public class CustomUserStorageProvider implements UserStorageProvider, 
        UserLookupProvider,
        CredentialInputValidator, 
        UserQueryProvider {
        
            // ... private members omitted
            
            public CustomUserStorageProvider(KeycloakSession ksession, ComponentModel model) {
            this.ksession = ksession;
            this.model = model;
            }

            // ... implementation methods for each supported capability
        }
        ```

        请注意，我们正在保存传递给构造函数的值。稍后我们将看到它们如何在我们的实现中发挥重要作用。

    4. 用户查找提供程序的实现

        Keycloak 使用此接口中的方法来恢复用户模型实例的 id、用户名或电子邮件。在本例中，id 是该用户的唯一标识符，格式如下： `‘f:’ unique_id ‘:’ external_id`

        - ‘f:’只是一个固定的前缀，表示这是一个联合用户
        - unique_id 是 Keycloak 的用户 ID
        - external_id 是指定用户商店使用的用户标识符。在我们的例子中，就是用户名列的值

        让我们继续实现该接口的方法，首先是 getUserByUsername()：

        ```java
        @Override
        public UserModel getUserByUsername(String username, RealmModel realm) {
            try ( Connection c = DbUtil.getConnection(this.model)) {
                PreparedStatement st = c.prepareStatement(
                "select " +
                " username, firstName, lastName, email, birthDate " + 
                "from users " + 
                "where username = ?");
                st.setString(1, username);
                st.execute();
                ResultSet rs = st.getResultSet();
                if ( rs.next()) {
                    return mapUser(realm,rs);
                } else {
                    return null;
                }
            }
            catch(SQLException ex) {
                throw new RuntimeException("Database error:" + ex.getMessage(),ex);
            }
        }
        ```

        不出所料，这是一个简单的数据库查询，使用提供的用户名来查找其信息。有两个有趣的地方需要解释一下： DbUtil.getConnection() 和 mapUser()。

        DbUtil 是一个辅助类，它以某种方式根据我们在构造函数中获取的组件模型中包含的信息返回 JDBC 连接。稍后我们将详细介绍它。

        至于 mapUser()，它的工作是将包含用户数据的数据库记录映射到 UserModel 实例。正如 Keycloak 所见，UserModel 代表一个用户实体，并有读取其属性的方法。我们对该接口的实现（在此提供）扩展了 Keycloak 提供的 AbstractUserAdapter 类。我们还在实现中添加了一个 Builder 内部类，这样 mapUser() 就能轻松创建 UserModel 实例：

        ```java
        private UserModel mapUser(RealmModel realm, ResultSet rs) throws SQLException {
            CustomUser user = new CustomUser.Builder(ksession, realm, model, rs.getString("username"))
            .email(rs.getString("email"))
            .firstName(rs.getString("firstName"))
            .lastName(rs.getString("lastName"))
            .birthDate(rs.getDate("birthDate"))
            .build();
            return user;
        }
        ```

        类似地，其他方法基本上与上述模式相同，因此我们不再详细介绍。请参考提供程序的代码，查看所有 getUserByXXX 和 searchForUser 方法。

    5. 获取连接

        现在，让我们来看看 DbUtil.getConnection() 方法：

        ```java
        public class DbUtil {

            public static Connection getConnection(ComponentModel config) throws SQLException{
                String driverClass = config.get(CONFIG_KEY_JDBC_DRIVER);
                try {
                    Class.forName(driverClass);
                } catch(ClassNotFoundException nfe) {
                // ... error handling omitted
                }
                return DriverManager.getConnection(
                config.get(CONFIG_KEY_JDBC_URL),
                config.get(CONFIG_KEY_DB_USERNAME),
                config.get(CONFIG_KEY_DB_PASSWORD));
            }
        }
        ```

        我们可以看到 ComponentModel 包含了创建所需的所有参数。但是，Keycloak 如何知道我们的自定义提供程序需要哪些参数呢？要回答这个问题，我们需要回到 CustomUserStorageProviderFactory。

    6. 配置元数据

        CustomUserStorageProviderFactory 的基本契约 UserStorageProviderFactory 包含一些方法，允许 Keycloak 查询配置属性元数据，同样重要的是，这些方法还可以验证分配的值。在我们的例子中，我们将定义一些建立 JDBC 连接所需的配置参数。由于这些元数据是静态的，我们将在构造函数中创建它，然后 getConfigProperties() 将简单地返回它。

        ```java
        public class CustomUserStorageProviderFactory
        implements UserStorageProviderFactory<CustomUserStorageProvider> {
            protected final List<ProviderConfigProperty> configMetadata;
            
            public CustomUserStorageProviderFactory() {
                configMetadata = ProviderConfigurationBuilder.create()
                .property()
                    .name(CONFIG_KEY_JDBC_DRIVER)
                    .label("JDBC Driver Class")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("org.h2.Driver")
                    .helpText("Fully qualified class name of the JDBC driver")
                    .add()
                // ... repeat this for every property (omitted)
                .build();
            }
            // ... other methods omitted
            
            @Override
            public List<ProviderConfigProperty> getConfigProperties() {
                return configMetadata;
            }

            @Override
            public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
            try (Connection c = DbUtil.getConnection(config)) {
                c.createStatement().execute(config.get(CONFIG_KEY_VALIDATION_QUERY));
            }
            catch(Exception ex) {
                throw new ComponentValidationException("Unable to validate database connection",ex);
            }
            }
        }
        ```

        在 validateConfiguration() 中，我们将获得所有需要验证的信息，以便在将我们提供的内容添加到 Realm 时验证所传递的参数。在我们的例子中，我们使用这些信息建立数据库连接并执行验证查询。如果出错，我们就会抛出 ComponentValidationException（组件验证异常），提示 Keycloak 参数无效。

        此外，虽然这里没有显示，但我们还可以使用 onCreated() 方法附加逻辑，每次管理员将我们的提供程序添加到 "Realm" 时都会执行该逻辑。这样，我们就可以执行一次性初始化逻辑，为使用存储做好准备，这在某些情况下可能是必要的。例如，我们可以使用此方法修改数据库，添加一列来记录特定用户是否已经使用过 Keycloak。

    7. 凭证输入验证器实现

        该接口包含验证用户凭证的方法。由于 Keycloak 支持不同类型的凭据（密码、OTP 令牌、X.509 证书等），我们的提供程序必须在 supportsCredentialType() 中告知是否支持给定类型，并在 isConfiguredFor() 中告知是否在给定 Realm 的上下文中为其配置。

        在我们的例子中，我们只支持密码，由于密码不需要任何额外配置，我们可以将后面的方法委托给前面的方法：

        ```java
        @Override
        public boolean supportsCredentialType(String credentialType) {
            return PasswordCredentialModel.TYPE.endsWith(credentialType);
        }

        @Override
        public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
            return supportsCredentialType(credentialType);
        }
        ```

        实际的密码验证发生在 isValid() 方法中：

        ```java
        @Override
        public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
            if(!this.supportsCredentialType(credentialInput.getType())) {
                return false;
            }
            StorageId sid = new StorageId(user.getId());
            String username = sid.getExternalId();
            
            try (Connection c = DbUtil.getConnection(this.model)) {
                PreparedStatement st = c.prepareStatement("select password from users where username = ?");
                st.setString(1, username);
                st.execute();
                ResultSet rs = st.getResultSet();
                if ( rs.next()) {
                    String pwd = rs.getString(1);
                    return pwd.equals(credentialInput.getChallengeResponse());
                }
                else {
                    return false;
                }
            }
            catch(SQLException ex) {
                throw new RuntimeException("Database error:" + ex.getMessage(),ex);
            }
        }
        ```

        这里有几点值得讨论。首先，请注意我们是如何从 UserModel 中提取外部 id 的，使用的是根据 Keycloak id 初始化的 StorageId 对象。我们可以利用这个 id 具有众所周知的格式这一事实，从中提取用户名，但最好还是稳妥起见，将这些知识封装在 Keycloak 提供的类中。

        接下来是实际的密码验证。我们的数据库非常简单，当然也非常不安全，因此密码检查非常简单：只需将数据库值与用户提供的值进行比较（可通过 getChallengeResponse() 获取），然后就完成了。当然，现实世界中的提供商还需要更多步骤，例如从数据库生成哈希值（hash informed password）和盐值（salt value），然后比较哈希值。

        最后，用户存储通常会有一些与密码相关的生命周期：最大年龄、阻止和/或不活动状态等。无论如何，在实现提供程序时，isValid() 方法都是添加这一逻辑的地方。

    8. 用户查询提供程序的实现

        UserQueryProvider 能力接口告诉 Keycloak，我们的提供程序可以在其商店中搜索用户。这就派上用场了，因为通过支持这一功能，我们就能在管理控制台中看到用户。

        该接口的方法包括 getUsersCount()（用于获取商店中的用户总数），以及多个 getXXX() 和 searchXXX() 方法。这个查询接口不仅支持查询用户，还支持查询组，但我们这次不讨论这个问题。

        由于这些方法的实现非常相似，我们只看其中的 searchForUser()：

        ```java
        @Override
        public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
            try (Connection c = DbUtil.getConnection(this.model)) {
                PreparedStatement st = c.prepareStatement(
                "select username, firstName, lastName, email, birthDate from users where username like ? order by username limit ? offset ?");
                st.setString(1, search);
                st.setInt(2, maxResults);
                st.setInt(3, firstResult);
                st.execute();
                ResultSet rs = st.getResultSet();
                List<UserModel> users = new ArrayList<>();
                while(rs.next()) {
                    users.add(mapUser(realm,rs));
                }
                return users.stream();
            } catch(SQLException ex) {
                throw new RuntimeException("Database error:" + ex.getMessage(),ex);
            }
        }
        ```

        正如我们所见，这里没有什么特别之处：只是普通的 JDBC 代码。值得一提的实现注意事项： UserQueryProvider 方法通常有分页和非分页两种版本。由于用户存储可能会有大量记录，因此非分页版本应使用合理的默认值，简单地委托给分页版本。更妙的是，我们可以添加一个配置参数来定义什么是 "合理的默认值(sensible default)"。

4. 测试

    既然我们已经实现了提供程序，现在就该使用嵌入式 Keycloak 实例对其进行本地测试了。项目代码中包含一个实时测试类，我们用它来引导 Keycloak 和自定义用户数据库，然后在控制台上打印访问 URL，最后再睡上一个小时。

    通过这种设置，我们只需在浏览器中打开打印的 URL，就能验证自定义提供程序是否按预期运行。

    要访问管理控制台，我们需要使用管理员凭证，可以通过查看 application-test.yml 文件获得。登录后，让我们进入 "Server Info" 页面。

    在 "Providers" 选项卡上，我们可以看到自定义提供商与其他内置存储提供商一起显示。

    我们还可以检查 Baeldung realm 是否已在使用此提供程序。为此，我们可以在左上角的下拉菜单中选择它，然后导航到用户联盟(User Federation)页面。

    接下来，让我们测试一下实际登录该领域的情况。我们将使用领域的账户管理页面，用户可以在这里管理其数据。我们的实时测试会在进入休眠状态前打印这个 URL，因此我们只需从控制台复制并粘贴到浏览器的地址栏即可。

    测试数据包含三个用户：user1、user2 和 user3。他们的密码都一样："changeit"。登录成功后，我们将看到账户管理页面显示导入的用户数据。

    但是，如果我们试图修改任何数据，就会出现错误。这是意料之中的，因为我们的提供者是只读的，所以 Keycloak 不允许修改它。由于支持双向同步超出了本文的讨论范围，所以我们暂且保持原样。

5. 结论

    在本文中，我们以用户存储提供程序为例，介绍了如何为 Keycloak 创建自定义提供程序。
