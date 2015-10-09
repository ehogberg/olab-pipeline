(ns pipeline.handler
  (:require [joda-time.instant :refer [date-time]]
            [langohr.basic :refer [publish ]]
            [pipeline.auditor :refer [write-audit-entry]]))

(defn intake-handler [ch md payload]
  (let [m (assoc payload :tx-id (.nextInt (java.util.Random.))
                 :submitted-at (str (date-time)))]
    (publish ch "" "pipeline.capitalizer" m md)
    (write-audit-entry ch "Intake" md m)))

(defn capitalize-handler [ch md payload]
  (let [m (assoc payload :comment
                 (.toUpperCase (:comment payload)))]
    (write-audit-entry ch "Capitalize" md m)))

(defn dumper-handler [cd md payload]
  (println payload))
