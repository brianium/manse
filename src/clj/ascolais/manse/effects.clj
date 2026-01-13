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
   (fn [{:keys [dispatch]} _ sql-params & [opts continuation-fx]]
     (let [connectable (or (:connectable opts) datasource)
           jdbc-opts   (dissoc opts :connectable)
           results     (jdbc/execute! connectable sql-params jdbc-opts)]
       (when (seq continuation-fx)
         (dispatch {::manse/results results} continuation-fx))
       results))})
