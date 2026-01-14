(ns ascolais.manse.placeholders
  "Placeholder definitions for Manse database effects."
  (:require [ascolais.manse :as-alias manse]
            [ascolais.sandestin :as-alias s]))

(def results
  "Placeholder returning vector of result maps from most recent ::manse/execute.
   Self-preserving for continuation patterns - returns itself if data not yet available.
   Accepts optional key argument to extract a specific field from each result."
  {::s/description
   "Access results from ::manse/execute in continuation effects.

   Returns: Vector of hash maps from the most recent execute effect.
   With key argument, returns vector of values at that key from each result.

   Self-preserving: Returns itself [::manse/results] or [::manse/results key] if data
   not yet available, allowing it to survive initial interpolation.

   Examples:
   [::manse/results]           ;; returns vector of result maps
   [::manse/results :users/id] ;; returns vector of id values"

   ::s/schema [:or
               [:tuple [:= ::manse/results]]
               [:tuple [:= ::manse/results] :keyword]]
   ::s/handler (fn [dispatch-data & [key]]
                 (if (contains? dispatch-data ::manse/results)
                   (let [res (::manse/results dispatch-data)]
                     (if key (mapv #(get % key) res) res))
                   (if key
                     [::manse/results key]
                     [::manse/results])))})

(def result
  "Placeholder returning single result map (or nil) from most recent ::manse/execute-one.
   Self-preserving for continuation patterns - returns itself if data not yet available.
   Accepts optional key argument to extract a specific field."
  {::s/description
   "Access result from ::manse/execute-one in continuation effects.

   Returns: Single hash map or nil from the most recent execute-one effect.
   With key argument, returns the value at that key.

   Self-preserving: Returns itself [::manse/result] or [::manse/result key] if data
   not yet available, allowing it to survive initial interpolation.

   Examples:
   [::manse/result]      ;; returns full result map
   [::manse/result :id]  ;; returns value at :id key"

   ::s/schema [:or
               [:tuple [:= ::manse/result]]
               [:tuple [:= ::manse/result] :keyword]]
   ::s/handler (fn [dispatch-data & [key]]
                 (if (contains? dispatch-data ::manse/result)
                   (let [res (::manse/result dispatch-data)]
                     (if key (get res key) res))
                   (if key
                     [::manse/result key]
                     [::manse/result])))})

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

   ::s/schema [:tuple [:= ::manse/reducible]]
   ::s/handler (fn [dispatch-data & _]
                 (if (contains? dispatch-data ::manse/reducible)
                   (::manse/reducible dispatch-data)
                   [::manse/reducible]))})
