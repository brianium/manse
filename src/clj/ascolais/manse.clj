(ns ascolais.manse
  "Sandestin registry for next.jdbc database effects."
  (:require [ascolais.manse.effects :as effects]
            [ascolais.manse.placeholders :as placeholders]
            [ascolais.sandestin :as-alias s]))

(defn registry
  "Returns a Sandestin registry for database effects.

   Options:
   - :datasource - javax.sql.DataSource (required)

   Example:
   (registry {:datasource my-ds})"
  [{:keys [datasource]}]
  (when-not datasource
    (throw (ex-info "Manse registry requires a :datasource" {})))
  {::s/effects
   {::execute (effects/create-execute datasource)}

   ::s/placeholders
   {::results placeholders/results}})
