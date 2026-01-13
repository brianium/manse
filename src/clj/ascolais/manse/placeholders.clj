(ns ascolais.manse.placeholders
  "Placeholder definitions for Manse database effects."
  (:require [ascolais.manse :as-alias manse]
            [ascolais.sandestin :as-alias s]))

(def results
  "Placeholder returning vector of result maps from most recent ::manse/execute.
   Self-preserving for continuation patterns - returns itself if data not yet available."
  {::s/description "Returns results from the most recent execute effect (self-preserving)"
   ::s/schema [:vector :map]
   ::s/handler (fn [dispatch-data]
                 (or (::manse/results dispatch-data)
                     [::manse/results]))})

(def result
  "Placeholder returning single result map (or nil) from most recent ::manse/execute-one.
   Self-preserving for continuation patterns - returns itself if data not yet available."
  {::s/description "Returns result from the most recent execute-one effect (self-preserving)"
   ::s/schema [:maybe :map]
   ::s/handler (fn [dispatch-data]
                 (if (contains? dispatch-data ::manse/result)
                   (::manse/result dispatch-data)
                   [::manse/result]))})

(def reducible
  "Placeholder returning reducible from most recent ::manse/plan.
   Self-preserving for continuation patterns - returns itself if data not yet available."
  {::s/description "Returns reducible from the most recent plan effect (self-preserving)"
   ::s/schema :any
   ::s/handler (fn [dispatch-data]
                 (if (contains? dispatch-data ::manse/reducible)
                   (::manse/reducible dispatch-data)
                   [::manse/reducible]))})
