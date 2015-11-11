(ns pipeline.handler
  (:require [clojure.core.async :refer [chan go go-loop <! >! >!!]]
            [clojure.tools.logging :as log ]
            [clojurewerkz.elastisch.rest.document :as esrd ]
            [joda-time.convert :refer [to-sql-timestamp]]
            [joda-time.instant :refer [date-time]]))

(defmacro processing-step [[in out] body]
  `(let [in#  ~in
         out# ~out]
     (go-loop []
      (when-let [val# (<! in#)]
        (let [resp# (-> val#
                        ~body)]
          (if out#
            (>! out# resp#))
          (recur))))))

(defn add-company-id [resp] (assoc resp :company-id 1))

(defn auditor-entry [resp message {:keys [connection]}]
  (log/info resp " audit logged.")
  (esrd/create connection "pipeline-audit" "audit-entry"
               (assoc resp
                      :audited-at (.toString (date-time))
                      :audit-message message))
  resp)

(defn add-init-properties [resp]
  (assoc resp
         :response-id         (java.util.UUID/randomUUID)
         :resp-tx-id          (java.util.UUID/randomUUID)
         :added-at            (to-sql-timestamp (date-time))))

(defn add-global-filter-properties [resp]
  (assoc resp :global-filter-applied true))
