(ns ascolais.manse.effects
  "Effect definitions for Manse database operations."
  (:require [ascolais.manse :as-alias manse]
            [ascolais.manse.schema :as schema]
            [ascolais.sandestin :as-alias s]
            [next.jdbc :as jdbc]))

(defn create-execute
  "Creates the execute effect handler with the given datasource."
  [datasource]
  {::s/description "Execute SQL and return vector of result maps. Dispatches continuation effects with results available via ::manse/results placeholder."
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
  {::s/description "Execute SQL and return a single result map (or nil). Dispatches continuation effects with result available via ::manse/result placeholder."
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
  {::s/description "Create a reducible query plan for efficient large result set processing. Dispatches continuation effects with reducible available via ::manse/reducible placeholder."
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
  {::s/description "Execute nested effects within a database transaction. Commits on success, rolls back on any error."
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
