(ns ascolais.manse.effects
  "Effect definitions for Manse database operations."
  (:require [ascolais.manse :as-alias manse]
            [ascolais.manse.schema :as schema]
            [ascolais.sandestin :as-alias s]
            [next.jdbc :as jdbc]))

(defn create-execute
  "Creates the execute effect handler with the given datasource."
  [datasource]
  {::s/description
   "Execute SQL and return a vector of result maps.

   Arguments:
   - sql-params: Vector of [sql-string & params] for parameterized queries
   - opts: Optional map with :builder-fn, :return-keys, :connectable, etc.
   - continuation-fx: Optional effects to dispatch with results

   Returns: Vector of hash maps representing rows

   Example: [::manse/execute [\"SELECT * FROM users WHERE active = ?\" true]]

   Use ::manse/results placeholder in continuations to access results."

   ::s/schema [:tuple
               [:= ::manse/execute]
               schema/sql-params
               [:? schema/execute-opts]
               [:? [:vector :any]]]
   ::s/handler
   (fn [{:keys [dispatch dispatch-data]} _ sql-params & [opts continuation-fx]]
     (let [connectable (or (:connectable opts)
                           (::manse/connectable dispatch-data)
                           datasource)
           jdbc-opts   (dissoc opts :connectable)
           results     (jdbc/execute! connectable sql-params jdbc-opts)]
       (when (seq continuation-fx)
         (dispatch {::manse/results results} continuation-fx))
       results))})

(defn create-execute-one
  "Creates the execute-one effect handler with the given datasource."
  [datasource]
  {::s/description
   "Execute SQL and return a single result map or nil.

   Arguments:
   - sql-params: Vector of [sql-string & params] for parameterized queries
   - opts: Optional map with :builder-fn, :return-keys, :connectable, etc.
   - continuation-fx: Optional effects to dispatch with result

   Returns: Single hash map (first row) or nil if no results

   Example: [::manse/execute-one [\"SELECT * FROM users WHERE id = ?\" 42]]

   Use ::manse/result placeholder in continuations to access the result."

   ::s/schema [:tuple
               [:= ::manse/execute-one]
               schema/sql-params
               [:? schema/execute-opts]
               [:? [:vector :any]]]
   ::s/handler
   (fn [{:keys [dispatch dispatch-data]} _ sql-params & [opts continuation-fx]]
     (let [connectable (or (:connectable opts)
                           (::manse/connectable dispatch-data)
                           datasource)
           jdbc-opts   (dissoc opts :connectable)
           result      (jdbc/execute-one! connectable sql-params jdbc-opts)]
       (when (seq continuation-fx)
         (dispatch {::manse/result result} continuation-fx))
       result))})

(defn create-plan
  "Creates the plan effect handler with the given datasource."
  [datasource]
  {::s/description
   "Create a reducible query plan for efficient large result set processing.

   Arguments:
   - sql-params: Vector of [sql-string & params] for parameterized queries
   - opts: Optional map with :builder-fn, :connectable, etc.
   - continuation-fx: Optional effects to dispatch with reducible

   Returns: IReduceInit - a reducible that processes rows without full materialization

   Example: [::manse/plan [\"SELECT * FROM large_table\"]]

   Use with reduce/transduce for memory-efficient processing of large result sets.
   Use ::manse/reducible placeholder in continuations to access the reducible."

   ::s/schema [:tuple
               [:= ::manse/plan]
               schema/sql-params
               [:? schema/execute-opts]
               [:? [:vector :any]]]
   ::s/handler
   (fn [{:keys [dispatch dispatch-data]} _ sql-params & [opts continuation-fx]]
     (let [connectable (or (:connectable opts)
                           (::manse/connectable dispatch-data)
                           datasource)
           jdbc-opts   (dissoc opts :connectable)
           reducible   (jdbc/plan connectable sql-params jdbc-opts)]
       (when (seq continuation-fx)
         (dispatch {::manse/reducible reducible} continuation-fx))
       reducible))})

(defn create-with-transaction
  "Creates the with-transaction effect handler with the given datasource."
  [datasource]
  {::s/description
   "Execute nested effects within a database transaction.

   Arguments:
   - nested-fx: Vector of effects to execute within the transaction
   - opts: Optional map with :isolation, :read-only, :rollback-only

   Behavior:
   - Commits automatically if all nested effects succeed
   - Rolls back if any nested effect throws an error
   - Nested effects share the same connection via ::manse/connectable

   Options:
   - :isolation - :read-uncommitted, :read-committed, :repeatable-read, :serializable
   - :read-only - boolean, hint that transaction is read-only
   - :rollback-only - boolean, always rollback (useful for testing)

   Example:
   [::manse/with-transaction
    [[::manse/execute-one [\"INSERT INTO users(name) VALUES(?)\" \"alice\"]]
     [::manse/execute [\"INSERT INTO audit(action) VALUES(?)\" \"user-created\"]]]]"

   ::s/schema [:tuple
               [:= ::manse/with-transaction]
               [:vector :any]
               [:? schema/transaction-opts]]
   ::s/handler
   (fn [{:keys [dispatch dispatch-data]} _ nested-fx & [opts]]
     (let [connectable (or (::manse/connectable dispatch-data) datasource)]
       (jdbc/transact
        connectable
        (fn [tx]
          (let [{:keys [errors] :as result}
                (dispatch {::manse/connectable tx} nested-fx)]
            (when (seq errors)
              (throw (ex-info "Transaction rolled back due to errors"
                              {:errors errors})))
            result))
        opts)))})
