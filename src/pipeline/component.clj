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

;; Helper fxns for marshaling a map/vector in RMQ wire-transportable
;; format, and unmarshaling it back into Clojure.
(defn deserialize [^bytes payload]
  (-> payload
      (String. "UTF-8")
      (decode true)))

(defn serialize [payload]
  (-> payload
      encode
      (.getBytes "UTF-8")))

;; Let Langohr know how to auto-magically serialize
;; Clojure structures.
;;
;; Deserialization is still
;; the responsibility of the processing client;
;; (make-handler-with-deserialization) is presented
;; below as one possible approach to making this
;; less manual.
(extend-protocol ByteSource
  clojure.lang.PersistentArrayMap
  (to-byte-array [input] (serialize input)))


;; Component defining a RabbitMQ service.
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


;; Define an Elasticsearch service.
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



;; quasi-middleware which deserializes a payload, then
;; invokes a specified handler with it.  Returns a
;; function suitable for use as a RMQ handler.
(defn make-handler-with-deserialization [handler]
  (fn [ch md payload]
    (->> payload
         deserialize
         (handler ch md))))


;;
;; Define a generic RMQ message processor.  A minimum of two
;; config parameters are required; a queue name to listen for
;; messages on, and a handler function to process them once
;; they have been received.  An optional exchange name may be
;; specified; if defined, the queue will be bound to the named
;; exchange, if not, it will be bound to the AMQ default exchange.
;;
;; This component makes a significant
;; assumption that message payloads deserialize to JSON, and thence
;; to Clojure maps/vectors...the supplied handler is wrapped with
;; a deserializer using make-handler-with-deserialization.  Do not
;; use this component if the expected message payload departs from
;; the above assumptions; things will break badly.
;;
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
