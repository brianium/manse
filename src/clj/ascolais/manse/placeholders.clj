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
