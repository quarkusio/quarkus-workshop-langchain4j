## Purpose

Provides durable per-session chat memory backed by PostgreSQL so that conversation history
survives a full application restart, keyed by a caller-supplied `memoryId`.

## ADDED Requirements

### Requirement: Chat messages are persisted per memory ID
The system SHALL store each chat message as a distinct row containing a `memoryId`, an
integer `sequence` column, and a serialized message payload in a `TEXT` column. The store
SHALL use `ChatMessageSerializer` / `ChatMessageDeserializer` from `dev.langchain4j.data.message`
to handle the full message type hierarchy (including tool-execution request and result messages).

#### Scenario: Messages are stored and retrieved in order
- **WHEN** `updateMessages` is called with a list of chat messages for a given `memoryId`
- **THEN** `getMessages` for that `memoryId` returns the same messages in the original sequence order

#### Scenario: Two memory IDs do not share messages
- **WHEN** messages are stored under two different `memoryId` values
- **THEN** `getMessages` for each ID returns only its own messages

#### Scenario: Tool-execution messages survive the round trip
- **WHEN** the message list includes a tool-execution request or result message
- **THEN** `getMessages` returns that message deserialized to its correct type with no data loss

### Requirement: Update replaces the full conversation for a memory ID
The system SHALL implement `updateMessages` as a delete-then-reinsert of the complete message
list for the given `memoryId`. Partial updates or diffs SHALL NOT be used.

#### Scenario: Stale messages are not retained after update
- **WHEN** `updateMessages` is called for a `memoryId` that already has stored messages
- **THEN** the previous messages are deleted and only the new list is present

### Requirement: Delete removes all messages for a memory ID
The system SHALL remove all rows for the given `memoryId` when `deleteMessages` is called.

#### Scenario: Messages are gone after delete
- **WHEN** `deleteMessages` is called for a `memoryId`
- **THEN** `getMessages` for that `memoryId` returns an empty list

### Requirement: All store operations run inside a transaction
Every `ChatMemoryStore` method SHALL be annotated `@Transactional`. The store is invoked from
agent execution and Flow threads where no ambient JTA session exists, so each method must manage
its own transaction boundary.

#### Scenario: Store works when called outside a REST request
- **WHEN** a store method is invoked from a background thread (e.g. a Quarkus Flow task)
- **THEN** the operation completes without a `TransactionRequiredException` or similar error

### Requirement: Store bean is application-scoped and auto-discovered
The `DatabaseChatMemoryStore` bean SHALL be annotated `@ApplicationScoped`. Because
`ChatMemoryProcessor` registers `InMemoryChatMemoryStoreProducer` as a `@DefaultBean`,
an `@ApplicationScoped` bean implementing `ChatMemoryStore` SHALL automatically take precedence
with no additional wiring or supplier class needed.

#### Scenario: Default in-memory store is replaced at runtime
- **WHEN** an `@ApplicationScoped ChatMemoryStore` bean is present on the classpath
- **THEN** the application uses the custom store and the default in-memory store is not instantiated
