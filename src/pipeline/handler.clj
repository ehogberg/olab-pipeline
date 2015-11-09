(ns pipeline.handler
  (:require [clojure.core.async :refer [chan go go-loop <! >! >!!]]
            [clojure.tools.logging :as log ]
            [clojurewerkz.elastisch.rest.document :as esrd ]
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

(defn auditor-entry [resp message {:keys [connection]}]
  (log/info resp " audit logged.")
  (esrd/create connection "pipeline-audit" "audit-entry"
               (assoc resp
                      :audited-at (.toString (date-time))
                      :audit-message message))
  resp)

(defn add-init-properties [resp]
  (assoc resp
         :response-id         (.toString (java.util.UUID/randomUUID))
         :checkpoint-id       (.toString (java.util.UUID/randomUUID))
         :resp-tx-id          (.toString (java.util.UUID/randomUUID))
         :added-at            (.toString (date-time))))

(defn add-global-filter-properties [resp]
  (assoc resp :global-filter-applied true))
