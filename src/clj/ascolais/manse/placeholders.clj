(ns ascolais.manse.placeholders
  "Placeholder definitions for Manse database effects."
  (:require [ascolais.manse :as-alias manse]
            [ascolais.sandestin :as-alias s]))

(def results
  "Placeholder returning vector of result maps from most recent ::manse/execute.
   Self-preserving for continuation patterns - returns itself if data not yet available."
  {::s/description
   "Access results from ::manse/execute in continuation effects.

   Returns: Vector of hash maps from the most recent execute effect

   Self-preserving: Returns itself [::manse/results] if data not yet available,
   allowing it to survive initial interpolation and resolve in continuation dispatch.

   Example:
   [::manse/execute [\"SELECT * FROM users\"] {}
    [[::process-users [::manse/results]]]]"

   ::s/schema [:vector :map]
   ::s/handler (fn [dispatch-data]
                 (or (::manse/results dispatch-data)
                     [::manse/results]))})

(def result
  "Placeholder returning single result map (or nil) from most recent ::manse/execute-one.
   Self-preserving for continuation patterns - returns itself if data not yet available."
  {::s/description
   "Access result from ::manse/execute-one in continuation effects.

   Returns: Single hash map or nil from the most recent execute-one effect

   Self-preserving: Returns itself [::manse/result] if data not yet available,
   allowing it to survive initial interpolation and resolve in continuation dispatch.

   Example:
   [::manse/execute-one [\"SELECT * FROM users WHERE id = ?\" 1] {}
    [[::send-email [::manse/result]]]]"

   ::s/schema [:maybe :map]
   ::s/handler (fn [dispatch-data]
                 (if (contains? dispatch-data ::manse/result)
                   (::manse/result dispatch-data)
                   [::manse/result]))})

(def reducible
  "Placeholder returning reducible from most recent ::manse/plan.
   Self-preserving for continuation patterns - returns itself if data not yet available."
  {::s/description
   "Access reducible from ::manse/plan in continuation effects.

   Returns: IReduceInit from the most recent plan effect

   Self-preserving: Returns itself [::manse/reducible] if data not yet available,
   allowing it to survive initial interpolation and resolve in continuation dispatch.

   Use with reduce/transduce for memory-efficient large result set processing.

   Example:
   [::manse/plan [\"SELECT * FROM large_table\"] {}
    [[::batch-process [::manse/reducible]]]]"

   ::s/schema :any
   ::s/handler (fn [dispatch-data]
                 (if (contains? dispatch-data ::manse/reducible)
                   (::manse/reducible dispatch-data)
                   [::manse/reducible]))})
