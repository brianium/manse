# Manse

A [Sandestin](https://github.com/brianium/sandestin) library wrapping [next.jdbc](https://github.com/seancorfield/next-jdbc), exposing database operations as effect vectors.

## Installation

```clojure
;; deps.edn
{:deps
 {io.github.brianium/manse {:git/tag "v0.1.0" :git/sha "..."}}}
```

## Quick Start

```clojure
(require '[ascolais.manse :as manse]
         '[ascolais.sandestin :as s]
         '[next.jdbc :as jdbc])

;; Create a datasource
(def ds (jdbc/get-datasource {:dbtype "sqlite" :dbname "mydb.db"}))

;; Create a Sandestin dispatch function with Manse effects
(def dispatch
  (s/create-dispatch [(manse/registry {:datasource ds})]))

;; Execute queries
(dispatch {} [[::manse/execute ["SELECT * FROM users"]]])
;; => {:results [{:effect [...] :res [{:users/id 1 :users/name "alice"} ...]}]
;;     :errors []}
```

## Effects

### `::manse/execute`

Execute SQL returning a vector of result maps.

```clojure
;; Basic query
[[::manse/execute ["SELECT * FROM users WHERE active = ?" true]]]

;; With options
[[::manse/execute ["SELECT * FROM users"]
  {:builder-fn next.jdbc.result-set/as-unqualified-maps}]]

;; With continuation effects
[[::manse/execute ["SELECT * FROM users"] {}
  [[::my-effect [::manse/results]]]]]
```

### `::manse/execute-one`

Execute SQL returning a single result map or nil.

```clojure
[[::manse/execute-one ["SELECT * FROM users WHERE id = ?" 42]]]

;; With continuation using key extraction
[[::manse/execute-one ["SELECT * FROM users WHERE id = ?" 42] {}
  [[::send-email [::manse/result :users/email]]]]]
```

### `::manse/plan`

Create a reducible for memory-efficient processing of large result sets.

```clojure
[[::manse/plan ["SELECT * FROM large_table"] {}
  [[::process-batch [::manse/reducible]]]]]

;; The reducible can be used with reduce/transduce
(let [{:keys [results]} (dispatch {} [[::manse/plan ["SELECT * FROM users"]]])]
  (into [] (map :users/name) (-> results first :res)))
```

### `::manse/with-transaction`

Execute nested effects within a database transaction.

```clojure
[[::manse/with-transaction
  [[::manse/execute-one ["INSERT INTO users(name) VALUES(?)" "alice"]
    {:return-keys true}
    [[::manse/execute ["INSERT INTO audit(user_id, action) VALUES(?, ?)"
                       [::manse/result :last_insert_rowid()] "created"]]]]]]]

;; With transaction options
[[::manse/with-transaction
  [[::manse/execute ["UPDATE accounts SET balance = balance - 100 WHERE id = ?" 1]]
   [::manse/execute ["UPDATE accounts SET balance = balance + 100 WHERE id = ?" 2]]]
  {:isolation :serializable}]]
```

Transaction options:
- `:isolation` - `:read-uncommitted`, `:read-committed`, `:repeatable-read`, `:serializable`
- `:read-only` - boolean hint for read-only transactions
- `:rollback-only` - always rollback (useful for testing)

## Placeholders

Placeholders provide access to effect results in continuation effects.

### `::manse/results`

Access results from the most recent `::manse/execute`.

```clojure
[::manse/results]           ;; vector of result maps
[::manse/results :users/id] ;; vector of id values (key extraction)
```

### `::manse/result`

Access result from the most recent `::manse/execute-one`.

```clojure
[::manse/result]      ;; single result map or nil
[::manse/result :id]  ;; value at :id key
```

### `::manse/reducible`

Access reducible from the most recent `::manse/plan`.

```clojure
[::manse/reducible]
```

## Discoverability

Manse provides comprehensive descriptions for Sandestin's exploration API:

```clojure
;; Describe all effects and placeholders
(s/describe dispatch)

;; Describe a specific effect
(s/describe dispatch ::manse/execute)

;; Search by text
(s/grep dispatch "transaction")

;; Generate sample effect vectors
(s/sample dispatch ::manse/execute-one 3)
```

## Execute Options

All execute effects accept next.jdbc options:

- `:builder-fn` - Result set builder (e.g., `as-unqualified-maps`)
- `:return-keys` - Return generated keys on insert
- `:column-fn` - Transform column names
- `:table-fn` - Transform table names
- `:label-fn` - Transform column labels
- `:qualifier-fn` - Transform namespace qualifiers
- `:connectable` - Override the datasource/connection

## Connection Pooling

Manse works with any `javax.sql.DataSource`, including connection pools:

```clojure
(require '[hikari-cp.core :as hikari])

(def pool
  (hikari/make-datasource
    {:jdbc-url "jdbc:postgresql://localhost/mydb"
     :username "user"
     :password "pass"}))

(def dispatch
  (s/create-dispatch [(manse/registry {:datasource pool})]))
```

## License

Copyright 2024 Brian Scaturro

Distributed under the Eclipse Public License version 2.0.
