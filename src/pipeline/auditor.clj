(ns pipeline.auditor
  (:require [clojurewerkz.elastisch.rest.document :as esrd]
            [com.climate.claypoole :as cp ]
            [com.stuartsierra.component :as component ]
            [langohr.consumers :as lcons ]
            [langohr.core :as rmq ]
            [langohr.queue :as q]
            [pipeline.component :refer [new-channel deserialize] ]))

(defn make-auditor-write-handler [{:keys [connection]} pool]
  (fn [_ _ payload]
    (cp/future pool
      (->> payload
           deserialize
           (esrd/create connection "pipeline-audit" "audit-entry")))))

(defrecord Auditor [rmq es queue-name exchange-name pool pool-size]
  component/Lifecycle
  (start [c]
    (let [ch (new-channel rmq)
          pool (cp/threadpool pool-size)]
      (q/declare ch queue-name)
      (q/bind ch queue-name exchange-name)
      (lcons/subscribe ch queue-name
                       (make-auditor-write-handler es pool)
                       {:auto-ack true})
      (assoc c :ch ch :pool pool)))
  (stop [c]
    (rmq/close (:ch c))
    (cp/shutdown (:pool c))
    (assoc c :ch nil)))

(defn new-auditor [config] (map->Auditor config))
