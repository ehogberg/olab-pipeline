(ns pipeline.component
  (:require [clojure.core.async :refer [chan mult tap]]
            [clojure.tools.logging :as log ]
            [clojurewerkz.elastisch.rest :as esr ]
            [com.stuartsierra.component :as component]
            [pipeline.handler :as handler]
            [taoensso.faraday :as far ]))

;; Dynamodb
(defrecord DynamoDB [access-key secret-key endpoint])

(defn new-DynamoDB [config]
  (map->DynamoDB config))

;; Define an Elasticsearch service.
(defrecord Elasticsearch [uri]
  component/Lifecycle
  (start [component]
    (log/debug "Connecting to Elasticsearch")
    (assoc component :connection (esr/connect uri )))
  (stop [component]
    (log/debug "Disconnecting from Elasticsearch")
    (assoc component :connection nil)))

(defn new-Elasticsearch [configs] (map->Elasticsearch configs))

(defn init-channels [c]
  (assoc c
         :pipeline-entry (chan 1000)
         :pipeline-init  (chan 1000)
         :global-filter  (chan 1000)
         :pipeline-finalize (chan 1000)))

(defrecord Channels [pipeline-entry pipeline-init global-filter
                     pipeline-finalize]
  component/Lifecycle
  (start [c]
    (init-channels c))
  (stop [c]
    c))

(defn new-Channels [config]
  (map->Channels config))

(defrecord Handlers [channels in]
  component/Lifecycle
  (start [c]
    (let [channels (:channels c)
          finalize-mult (mult (:pipeline-finalize channels))]
      (handler/processing-step [(:pipeline-init channels)
                                (:global-filter channels)]
                               handler/add-init-properties)
      (handler/processing-step [(:global-filter channels)
                                (:pipeline-finalize channels)]
                               handler/add-global-filter-properties)
      (handler/processing-step [(tap finalize-mult (chan)) nil]
                               log/debug)
      (handler/processing-step [(tap finalize-mult (chan)) nil]
                               (log/debug "Written to db."))
      (assoc c :in (:pipeline-init channels))))
  (stop [c]
    (assoc c :in nil)))

(defn new-Handlers [config]
  (map->Handlers config))
