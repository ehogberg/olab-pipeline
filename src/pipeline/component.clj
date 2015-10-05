(ns pipeline.component
  (:import java.util.concurrent.Executors)
  (:require [cheshire.core :refer [decode encode]]
            [clojure.tools.logging :as log ]
            [clojurewerkz.elastisch.rest :as esr ]
            [clojurewerkz.support.bytes :refer [ByteSource]]
            [com.stuartsierra.component :as component]
            [langohr.channel :as ch]
            [langohr.consumers :as lcons]
            [langohr.core :as rmq]
            [langohr.queue :as q ]))

(declare serialize)

;;
(extend-protocol ByteSource
  clojure.lang.PersistentArrayMap
  (to-byte-array [input] (serialize input)))


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
           (esr/connect uri {:executor (Executors/newCachedThreadPool)})))
  (stop [component]
    (log/debug "Disconnecting from Elasticsearch")
    (assoc component :connection nil)))

(defn new-Elasticsearch [configs] (map->Elasticsearch configs))

(defn deserialize [^bytes payload]
  (-> payload
      (String. "UTF-8")
      (decode true)))

(defn serialize [payload]
  (-> payload
      encode
      .getBytes))

(defn make-handler-with-deserialization [handler]
  (fn [ch md payload]
    (->> payload
         deserialize
         (handler ch md))))

(defrecord QueueProcessor [rmq channel queue-name exchange-name handler]
  component/Lifecycle
  (start [component]
    (log/debug "New queue processor: " queue-name)
    (let [ch (new-channel rmq)]
      (q/declare ch queue-name)
      (if exchange-name
        (q/bind ch queue-name exchange-name))
      (lcons/subscribe ch queue-name
                       (make-handler-with-deserialization handler)
                       {:auto-ack true})
      (assoc component :channel ch)))
  (stop [component]
    (log/debug "Closing queue processor: " queue-name)
    (ch/close channel)
    (assoc component :channel nil)))

(defn new-queue-processor [config] (map->QueueProcessor config))
