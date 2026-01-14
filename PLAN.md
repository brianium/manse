# Manse Implementation Plan

Manse is a Sandestin library wrapping next.jdbc, exposing database operations as effect vectors.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Datasource ownership | Passed to registry, closed over | Lifecycle managed externally; clean separation of concerns |
| Transactions | Nested effect approach | Explicit, composable, matches next.jdbc semantics |
| JSON support | Deferred | Easy to implement in userland; revisit later |
| Actions | None initially | No clear composition pattern needed yet |
| Error handling | Bubble up through Sandestin | Start simple; interceptors available for customization |

## Dependencies

```clojure
;; deps.edn
{:deps
 {com.github.seancorfield/next.jdbc {:mvn/version "1.3.1086"}
  metosin/malli {:mvn/version "0.16.4"}
  io.github.brianium/sandestin {:git/tag "v0.1.0" :git/sha "cfe9c24"}}

 :aliases
 {:dev
  {:extra-paths ["dev/src/clj"]
   :extra-deps {org.xerial/sqlite-jdbc {:mvn/version "3.47.0.0"}
                org.postgresql/postgresql {:mvn/version "42.7.4"}}}

  :test
  {:extra-paths ["test/src/clj"]
   :extra-deps {org.xerial/sqlite-jdbc {:mvn/version "3.47.0.0"}
                io.github.cognitect-labs/test-runner
                {:git/tag "v0.5.1" :git/sha "dfb30dd"}}}}}
```

## Namespace Structure

```
src/clj/ascolais/
├── manse.clj              ;; Public API: registry fn, effect/placeholder keywords
└── manse/
    ├── effects.clj        ;; Effect handler implementations
    ├── placeholders.clj   ;; Placeholder handler implementations
    └── schema.clj         ;; Malli schemas for effects and options
```

## Registry Function

```clojure
(ns ascolais.manse)

(defn registry
  "Returns a Sandestin registry for database effects.

   Options:
   - :datasource - javax.sql.DataSource (required)

   Example:
   (registry {:datasource my-ds})"
  [{:keys [datasource]}]
  ;; Return Sandestin registry map
  )
```

## Effects

### `::manse/execute`

Wraps `next.jdbc/execute!` - executes SQL returning vector of result maps.

```clojure
;; Schema
[:tuple
 [:= ::execute]
 ::sql-params                              ;; [sql & params]
 [:? ::execute-opts]]                      ;; optional next.jdbc opts

;; Handler dispatches continuation with results in dispatch-data
```

**Usage:**
```clojure
[[::manse/execute ["SELECT * FROM users WHERE active = ?" true]]
 [::some-effect [::manse/results]]]

[[::manse/execute ["SELECT * FROM users"] {:builder-fn rs/as-unqualified-maps}]
 [::some-effect [::manse/results]]]
```

### `::manse/execute-one`

Wraps `next.jdbc/execute-one!` - executes SQL returning single result map or nil.

```clojure
;; Schema
[:tuple
 [:= ::execute-one]
 ::sql-params
 [:? ::execute-opts]]
```

**Usage:**
```clojure
[[::manse/execute-one ["SELECT * FROM users WHERE id = ?" 42]]
 [::send-welcome-email [::manse/result]]]
```

### `::manse/plan`

Wraps `next.jdbc/plan` - returns reducible for efficient large result set processing.

```clojure
;; Schema
[:tuple
 [:= ::plan]
 ::sql-params
 [:? ::plan-opts]]
```

**Usage:**
```clojure
[[::manse/plan ["SELECT * FROM large_table"]]
 [::process-in-batches [::manse/reducible]]]
```

### `::manse/with-transaction`

Wraps nested effects in a database transaction. Commits on success, rolls back on any error.

```clojure
;; Schema
[:tuple
 [:= ::with-transaction]
 [:vector :any]           ;; nested effects
 [:? ::transaction-opts]] ;; :isolation, :read-only, :rollback-only
```

**Usage:**
```clojure
[[::manse/with-transaction
  [[::manse/execute-one ["INSERT INTO users(name) VALUES(?)" "alice"]]
   [::manse/execute ["INSERT INTO audit(user_id, action) VALUES(?, ?)"
                     [::manse/result :id] "created"]]]]]
```

**Implementation approach:**
1. Get connection from datasource
2. Start transaction on connection
3. Recursively dispatch nested effects with connection as connectable
4. Commit if no errors, rollback otherwise
5. Close connection

## Placeholders

Placeholder schemas describe the input signature (like effects) for consistent validation and sample generation.

### `::manse/results`

Returns vector of result maps from most recent `::manse/execute`. Supports optional key extraction.

```clojure
;; Schema (input signature)
[:or
 [:tuple [:= ::manse/results]]
 [:tuple [:= ::manse/results] :keyword]]

;; Usage
[::manse/results]           ;; returns vector of result maps
[::manse/results :users/id] ;; returns vector of id values
```

### `::manse/result`

Returns single result map (or nil) from most recent `::manse/execute-one`. Supports optional key extraction.

```clojure
;; Schema (input signature)
[:or
 [:tuple [:= ::manse/result]]
 [:tuple [:= ::manse/result] :keyword]]

;; Usage
[::manse/result]      ;; returns full result map
[::manse/result :id]  ;; returns value at :id key
```

### `::manse/reducible`

Returns reducible from most recent `::manse/plan`.

```clojure
;; Schema (input signature)
[:tuple [:= ::manse/reducible]]

;; Usage
[::manse/reducible]
```

## Connectable Override

All effects accept an optional `:connectable` in the opts map to override the registry's datasource. Used internally for transactions but also available to users.

```clojure
;; Inside a transaction, nested effects receive tx connection
[[::manse/execute ["SELECT 1"] {:connectable tx-connection}]]
```

## Schemas

```clojure
(ns ascolais.manse.schema
  (:require [malli.core :as m]))

(def sql-params
  "SQL params vector: [sql-string & params]"
  [:cat :string [:* :any]])

(def execute-opts
  "Options for execute/execute-one"
  [:map
   [:builder-fn {:optional true} :any]
   [:return-keys {:optional true} [:or :boolean [:vector :keyword]]]
   [:connectable {:optional true} :any]])

(def transaction-opts
  "Options for with-transaction"
  [:map
   [:isolation {:optional true} [:enum :read-uncommitted :read-committed
                                       :repeatable-read :serializable]]
   [:read-only {:optional true} :boolean]
   [:rollback-only {:optional true} :boolean]])
```

## Testing Strategy

### Unit Tests
- Schema validation for all effects
- Placeholder resolution
- Registry creation

### Integration Tests (SQLite)
- Execute/execute-one with real queries
- Transaction commit/rollback behavior
- Plan with reduce/transduce
- Error scenarios

**Rationale for SQLite-focused integration tests:** next.jdbc is well-tested against various drivers. Manse's value is in the Sandestin integration, which can be verified with SQLite.

### Test Utilities
- In-memory SQLite datasource factory
- Test table setup/teardown helpers

## Implementation Phases

### Phase 1: Core Foundation [COMPLETE]
1. Set up project structure and dependencies
2. Implement `::manse/execute` effect with schema
3. Implement `::manse/results` placeholder
4. Basic registry function
5. Unit tests for schema validation
6. Integration test with SQLite

### Phase 2: Complete Core Effects [COMPLETE]
1. Implement `::manse/execute-one` and `::manse/result`
2. Implement `::manse/plan` and `::manse/reducible`
3. Integration tests for all core effects

### Phase 3: Transactions [COMPLETE]
1. Implement `::manse/with-transaction`
2. Connectable override via `::manse/connectable` in dispatch-data
3. Transaction isolation/rollback options
4. Integration tests for commit/rollback behavior

### Phase 4: Polish [COMPLETE]
1. Comprehensive schema descriptions for discoverability
2. Edge case handling and improved error messages
3. Schema tests for transaction-opts

## Future Considerations (Out of Scope for Now)

- **JSON support:** Protocol extensions for automatic JSON/JSONB handling (Charred)
- **Result coercion:** Placeholder arguments for transforming results (sqlite vs postgres differences)
- **Friendly SQL functions:** `insert!`, `query`, `update!`, `delete!`, etc. from `next.jdbc.sql`
- **Batch operations:** `execute-batch!` support

## Open Questions

1. **Nested transaction semantics:** Should nested `::with-transaction` create savepoints, or reuse the outer transaction? Current implementation reuses the outer connection via `::manse/connectable` in dispatch-data.

2. **Plan consumption:** Currently `::manse/plan` dispatches continuation immediately with the reducible. Users should be aware that the reducible holds a connection open until fully reduced.
