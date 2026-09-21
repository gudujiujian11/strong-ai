# MySQL Conversation Store Design

## Goal

Add a MySQL-backed implementation of `ConversationStore`. MySQL is the default store, while the existing in-memory implementation remains available only when `strong.conversation.store=memory` is configured explicitly.

## Storage Model

Use two normalized tables:

- `ai_conversation` stores conversation metadata keyed by the existing string conversation ID.
- `ai_conversation_message` stores messages keyed by message ID, linked to a conversation ID, with `message_order` preserving list order.

Flyway migration `V4` creates both tables and indexes conversations by `updated_at`. The migration does not modify existing tables or data.

## Components

- `MysqlConversationStore` implements `ConversationStore` using MyBatis-Plus mappers.
- Conversation and message persistence entities remain internal to the webapp module; public API models are unchanged.
- Mapper methods load conversations ordered by update time and messages ordered by `message_order`.
- `InMemoryConversationStore` is conditional on `strong.conversation.store=memory`.
- `MysqlConversationStore` is conditional on `strong.conversation.store=mysql` with `matchIfMissing=true`, making it the default.

## Save And Delete Semantics

`save` treats `Conversation` as one aggregate snapshot. In one transaction it upserts conversation metadata, deletes existing messages for that conversation, and inserts the supplied messages in list order. This preserves the current overwrite behavior without changing the interface.

`deleteById` deletes the conversation row first and then its messages in one transaction, returning `true` only when the conversation existed. Deleting the parent first serializes concurrent saves and deletes on the conversation ID without exposing an intermediate state outside the transaction. No silent fallback to memory occurs when MySQL operations fail.

## Compatibility

The `ConversationStore` interface, service/controller APIs, ordering of `findAll`, nullable token fields, message reasoning, and existing model types remain unchanged. Existing deployments can opt into the development-only memory store through configuration.

## Verification

Tests cover:

- Spring conditional selection defaults to MySQL and switches to memory explicitly.
- Conversation CRUD mapping.
- Descending conversation update order.
- Message order and nullable fields.
- Re-saving a conversation replaces its prior message snapshot.
- Deleting a missing or existing conversation returns the expected boolean.
- Flyway migration contains the required tables, keys, and indexes.
