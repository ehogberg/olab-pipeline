(ns pipeline.auditor
  (:require [clojure.tools.logging :as log ]
            [clojurewerkz.elastisch.rest.document :as esrd]
            [com.stuartsierra.component :as component ]))

(defn make-auditor-write-handler [{:keys [connection]} audit-tag]
  (fn [_ _ payload]
    (as-> payload $
      (deserialize $)
      (assoc $ :audit-tag audit-tag)
      (esrd/create connection "pipeline-audit" "audit-entry" $))))
