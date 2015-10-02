(ns pipeline.component
  (:require [clojure.tools.logging :as log ]
            [clojurewerkz.elastisch.rest :as esr ]
            [com.stuartsierra.component :as component]
            [langohr.channel :as ch]
            [langohr.consumers :as lcons]
            [langohr.core :as rmq]
            [langohr.queue :as q ]))


(defrecord RabbitMQ [uri]
  component/Lifecycle
  (start [component]
    (log/debug "Connecting to RabbitMQ cluster.")
    (assoc component :connection
           (rmq/connect {:uri uri })))
  (stop [component]
    (log/debug "Disconnecting from RabbitMQ.")
    (rmq/close (:connection component))
    (assoc component :connection nil)))

(defn new-Rabbit [configs] (map->RabbitMQ configs))

(defn new-channel [{:keys [connection]}]
  (ch/open connection))

(defrecord Elasticsearch [uri]
  component/Lifecycle
  (start [component]
    (log/debug "Connecting to Elasticsearch")
    (assoc component :connection
           (esr/connect uri)))
  (stop [component]
    (log/debug "Disconnecting from Elasticsearch")
    (assoc component :connection nil)))

(defn new-Elasticsearch [configs] (map->Elasticsearch configs))


(defrecord QueueProcessor [rmq channel queue-name handler]
  component/Lifecycle
  (start [component]
    (log/debug "New queue processor: " queue-name)
    (let [ch (new-channel rmq)]
      (q/declare ch queue-name)
      (lcons/subscribe ch queue-name handler {:auto-ack true})
      (assoc component :channel ch)))
  (stop [component]
    (log/debug "Closing queue processor: " queue-name)
    (ch/close channel)
    (assoc component :channel nil)))

(defn new-queue-processor [config] (map->QueueProcessor config))
