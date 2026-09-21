# MySQL Conversation Store Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Subagent delegation is not authorized for this task.

**Goal:** Persist conversations and their ordered messages in MySQL by default, while retaining an explicitly selectable in-memory store.

**Architecture:** Add two normalized MySQL tables and a transactional `MysqlConversationStore` backed by focused MyBatis-Plus mappers. Use mutually exclusive Spring Boot property conditions so MySQL is selected when `strong.conversation.store` is absent or equals `mysql`, and memory is selected only when it equals `memory`.

**Tech Stack:** Java 8, Spring Boot 2.4, Spring conditional configuration, MyBatis-Plus, Flyway 7.10, JUnit 5, Mockito.

## Global Constraints

- Preserve the existing `ConversationStore` interface and all controller/service APIs.
- Preserve descending `updatedAt` ordering and message list order.
- `save` replaces the complete message snapshot transactionally.
- Database failures must propagate; there is no runtime fallback to memory.
- Do not modify the user's existing `bootstrap.yml` change.
- Do not create commits.

## File Map

- Modify `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/InMemoryConversationStore.java`: enable only for the explicit `memory` store selection.
- Create `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationEntity.java`: database representation of conversation metadata.
- Create `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationMessageEntity.java`: database representation of ordered messages.
- Create `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationMapper.java`: conversation upsert and ordered query operations.
- Create `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationMessageMapper.java`: message batch insert, ordered query, and delete operations.
- Create `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/MysqlConversationStore.java`: aggregate mapping and transactional persistence.
- Create `strong-ai-webapp/src/main/resources/db/migration/V4__conversation_store.sql`: MySQL schema.
- Create `strong-ai-webapp/src/test/java/cn/strong/webapp/user/conversation/ConversationStoreConfigurationTest.java`: conditional bean selection tests.
- Create `strong-ai-webapp/src/test/java/cn/strong/webapp/user/conversation/mysql/MysqlConversationStoreTest.java`: store behavior tests with mapper boundaries mocked.
- Create `strong-ai-webapp/src/test/java/cn/strong/webapp/user/conversation/mysql/ConversationStoreMigrationTest.java`: migration contract test.

---

### Task 1: Conditional Store Selection

**Files:**
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/InMemoryConversationStore.java`
- Create: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/conversation/ConversationStoreConfigurationTest.java`
- Create initially for compilation: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/MysqlConversationStore.java`
- Create initially for constructor injection: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationMapper.java`
- Create initially for constructor injection: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationMessageMapper.java`

**Interfaces:**
- Consumes: `ConversationStore`.
- Produces: exactly one `ConversationStore` bean selected by `strong.conversation.store`.

- [x] **Step 1: Write the failing conditional-selection test**

Use `ApplicationContextRunner` with a nested `@Configuration` importing both implementations. Assert that no property yields `MysqlConversationStore`, `strong.conversation.store=memory` yields `InMemoryConversationStore`, and `strong.conversation.store=mysql` yields `MysqlConversationStore`.

```java
@Test
void shouldUseMysqlByDefault() {
    contextRunner.run(context -> {
        assertThat(context).hasSingleBean(ConversationStore.class);
        assertThat(context.getBean(ConversationStore.class)).isInstanceOf(MysqlConversationStore.class);
    });
}

@Test
void shouldUseMemoryOnlyWhenExplicitlyConfigured() {
    contextRunner.withPropertyValues("strong.conversation.store=memory").run(context -> {
        assertThat(context).hasSingleBean(ConversationStore.class);
        assertThat(context.getBean(ConversationStore.class)).isInstanceOf(InMemoryConversationStore.class);
    });
}
```

- [x] **Step 2: Run the test and verify RED**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=ConversationStoreConfigurationTest test
```

Expected: failure because conditional selection is absent and/or `MysqlConversationStore` does not yet exist as a valid bean.

- [x] **Step 3: Add mutually exclusive conditions**

Annotate the memory implementation:

```java
@Repository
@ConditionalOnProperty(prefix = "strong.conversation", name = "store", havingValue = "memory")
public class InMemoryConversationStore implements ConversationStore {
```

Create minimal mapper dependency interfaces; Task 2 adds their database operations:

```java
@Mapper
public interface ConversationMapper {
}

@Mapper
public interface ConversationMessageMapper {
}
```

Create the MySQL implementation with its final constructor dependencies and default condition. Its CRUD methods initially throw `UnsupportedOperationException` until Task 2:

```java
@Repository
@ConditionalOnProperty(prefix = "strong.conversation", name = "store", havingValue = "mysql", matchIfMissing = true)
public class MysqlConversationStore implements ConversationStore {
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;

    public MysqlConversationStore(ConversationMapper conversationMapper,
                                  ConversationMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }
}
```

- [x] **Step 4: Run the selection test and verify GREEN**

Run the command from Step 2. Expected: all selection tests pass with exactly one store bean.

---

### Task 2: Transactional MySQL Aggregate Store

**Files:**
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationMessageEntity.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationMapper.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/ConversationMessageMapper.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/conversation/mysql/MysqlConversationStore.java`
- Create: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/conversation/mysql/MysqlConversationStoreTest.java`

**Interfaces:**
- Consumes: `Conversation`, `ConversationMessage`, `ConversationStore`.
- Produces: `MysqlConversationStore.findAll()`, `findById(String)`, `save(Conversation)`, and `deleteById(String)` with existing interface semantics.

- [x] **Step 1: Write failing mapping and query tests**

Mock both mapper interfaces and test these behaviors independently:

```java
@Test
void shouldLoadAllConversationsWithMessagesInDatabaseOrder() {
    when(conversationMapper.findAllOrderByUpdatedAtDesc()).thenReturn(Arrays.asList(newer, older));
    when(messageMapper.findByConversationIds(Arrays.asList("newer", "older")))
            .thenReturn(Arrays.asList(firstMessage, secondMessage));

    List<Conversation> result = store.findAll();

    assertEquals(Arrays.asList("newer", "older"),
            result.stream().map(Conversation::getId).collect(Collectors.toList()));
    assertEquals(Arrays.asList("message-1", "message-2"),
            result.get(0).getMessages().stream().map(ConversationMessage::getId).collect(Collectors.toList()));
}

@Test
void shouldReturnEmptyWhenConversationDoesNotExist() {
    when(conversationMapper.selectById("missing")).thenReturn(null);
    assertFalse(store.findById("missing").isPresent());
    verifyNoInteractions(messageMapper);
}
```

Also assert every conversation/message field, including `deepThinking`, `reasoning`, nullable token counts, and timestamps.

- [x] **Step 2: Run the focused test and verify RED**

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=MysqlConversationStoreTest test
```

Expected: failure because mapper/entity behavior and store mapping are not implemented.

- [x] **Step 3: Implement entities and read operations**

Use string input IDs and direct field mappings:

```java
@Data
@TableName("ai_conversation")
public class ConversationEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String title;
    private String provider;
    private String model;
    private Boolean deepThinking;
    private Long createdAt;
    private Long updatedAt;
}
```

```java
@Data
@TableName("ai_conversation_message")
public class ConversationMessageEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String conversationId;
    private Integer messageOrder;
    private String role;
    private String content;
    private String reasoning;
    private String provider;
    private String model;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long createdAt;
}
```

`ConversationMapper.findAllOrderByUpdatedAtDesc()` selects explicit columns ordered by `updated_at DESC`. `ConversationMessageMapper.findByConversationId` and `findByConversationIds` select explicit columns ordered by `conversation_id, message_order` (single-conversation query only needs `message_order`). The store groups batch-loaded messages without changing mapper order and always returns fresh model objects.

- [x] **Step 4: Run read tests and verify GREEN**

Run the command from Step 2. Expected: read/mapping tests pass.

- [x] **Step 5: Write failing snapshot save and delete tests**

```java
@Test
void shouldReplaceMessageSnapshotWhenSaving() {
    Conversation input = conversationWithTwoMessages();

    Conversation saved = store.save(input);

    InOrder writes = inOrder(conversationMapper, messageMapper);
    writes.verify(conversationMapper).upsert(any(ConversationEntity.class));
    writes.verify(messageMapper).deleteByConversationId(input.getId());
    writes.verify(messageMapper).insertBatch(messageCaptor.capture());
    assertEquals(Arrays.asList(0, 1), messageCaptor.getValue().stream()
            .map(ConversationMessageEntity::getMessageOrder).collect(Collectors.toList()));
    assertNotSame(input, saved);
    assertNotSame(input.getMessages(), saved.getMessages());
}

@Test
void shouldReportWhetherConversationWasDeleted() {
    when(conversationMapper.deleteById("existing")).thenReturn(1);
    when(conversationMapper.deleteById("missing")).thenReturn(0);
    assertTrue(store.deleteById("existing"));
    assertFalse(store.deleteById("missing"));
}
```

Add a zero-message test asserting `insertBatch` is not called.

- [x] **Step 6: Run write tests and verify RED**

Run the command from Step 2. Expected: write tests fail because transactional snapshot persistence is not implemented.

- [x] **Step 7: Implement mapper writes and transactional store methods**

Add `ConversationMapper.upsert(ConversationEntity)` using MySQL `INSERT ... ON DUPLICATE KEY UPDATE`. Add `ConversationMessageMapper.deleteByConversationId(String)` and a MyBatis `<script>` batch insert accepting `List<ConversationMessageEntity>`.

Implement save and delete with rollback on any exception:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Conversation save(Conversation conversation) {
    ConversationEntity entity = toEntity(conversation);
    List<ConversationMessageEntity> messages = toMessageEntities(conversation);
    conversationMapper.upsert(entity);
    messageMapper.deleteByConversationId(conversation.getId());
    if (!messages.isEmpty()) {
        messageMapper.insertBatch(messages);
    }
    return toModel(entity, messages);
}

@Override
@Transactional(rollbackFor = Exception.class)
public boolean deleteById(String id) {
    if (conversationMapper.deleteById(id) == 0) {
        return false;
    }
    messageMapper.deleteByConversationId(id);
    return true;
}
```

- [x] **Step 8: Run all conversation store tests and verify GREEN**

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest='ConversationStoreConfigurationTest,MysqlConversationStoreTest,ConversationServiceTest' test
```

Expected: all selected tests pass.

---

### Task 3: Flyway Schema And Regression Verification

**Files:**
- Create: `strong-ai-webapp/src/main/resources/db/migration/V4__conversation_store.sql`
- Create: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/conversation/mysql/ConversationStoreMigrationTest.java`

**Interfaces:**
- Consumes: entity and mapper table/column names from Task 2.
- Produces: `ai_conversation` and `ai_conversation_message` tables compatible with all mapper SQL.

- [x] **Step 1: Write the failing migration contract test**

Read `db/migration/V4__conversation_store.sql` as UTF-8 and assert it contains both `CREATE TABLE` statements, primary keys, unique message-order key, and update-time index:

```java
assertTrue(sql.contains("CREATE TABLE ai_conversation ("));
assertTrue(sql.contains("CREATE TABLE ai_conversation_message ("));
assertTrue(sql.contains("KEY idx_ai_conversation_updated_at (updated_at)"));
assertTrue(sql.contains("UNIQUE KEY uk_ai_conversation_message_order (conversation_id, message_order)"));
```

- [x] **Step 2: Run the migration test and verify RED**

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=ConversationStoreMigrationTest test
```

Expected: failure because the migration resource does not exist.

- [x] **Step 3: Add the MySQL migration**

Create `ai_conversation` with `VARCHAR(64)` ID, metadata fields, `TINYINT(1)` deep-thinking flag, millisecond timestamps, primary key, and `updated_at` index. Create `ai_conversation_message` with `VARCHAR(64)` ID/conversation ID, `INT` message order, nullable text/model/token fields, millisecond timestamp, primary key, unique `(conversation_id, message_order)`, and conversation lookup index. Use InnoDB and utf8mb4; do not add foreign keys because the existing schema manages aggregate deletion in application transactions.

- [x] **Step 4: Run the migration test and verify GREEN**

Run the command from Step 2. Expected: migration contract test passes.

- [x] **Step 5: Run affected-module verification**

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false test
```

Expected: reactor build succeeds and all module tests pass. Integration against a live MySQL instance is not claimed unless a disposable test database is available.

Actual: 88 of 89 full-module tests passed with `spring.arthas.enabled=false`. The remaining pre-existing `AiChatControllerTest` makes a real DeepSeek request with `maxTokens=393216`, which the external model rejects because its declared limit is `128000`. The focused 16 conversation tests and the Maven package build both passed.

- [x] **Step 6: Inspect final scope**

Run `git status --short` and `git diff --check`. Confirm only the design/plan documents, conversation persistence code/tests, and V4 migration are new or modified; preserve the pre-existing `bootstrap.yml` change.
