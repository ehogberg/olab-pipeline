(ns pipeline.component
  (:require [clojure.core.async :refer [chan close! mult tap]]
            [clojure.tools.logging :as log ]
            [clojurewerkz.elastisch.rest :as esr ]
            [compojure.api.middleware :refer [wrap-components]]
            [com.stuartsierra.component :as component]
            [pipeline.api :as api]
            [pipeline.dydb :refer [write-to-checkpoint]]
            [pipeline.handler :as handler]
            [ring.adapter.jetty :refer [run-jetty]]
            [taoensso.faraday :as far ]))

(defrecord WebServer [server port]
  component/Lifecycle
  (start [{:keys [port] :as this}]
    (assoc this
           :server (run-jetty (wrap-components #'api/api-routes
                                               (select-keys this
                                                            [:dynamodb
                                                             :es
                                                             :intake-channel]))
                              {:port port :join? false})))
  (stop [{:keys [server] :as this}]
    (if server
      (.stop server))
    (assoc this
           :server nil)))


;; Dynamodb
(defrecord DynamoDB [access-key secret-key endpoint])


;; Define an Elasticsearch service.
(defrecord Elasticsearch [uri]
  component/Lifecycle
  (start [component]
    (log/info "Connecting to Elasticsearch")
    (assoc component :connection (esr/connect uri )))
  (stop [component]
    (log/info "Disconnecting from Elasticsearch")
    (assoc component :connection nil)))


(defrecord ResponseIntake [in out]
  component/Lifecycle
  (start [{:keys [in out es] :as c}]
    (handler/processing-step [in out]
                             (handler/auditor-entry "Intake" es))
    c)
  (stop [c]
    (assoc c :in nil :out nil)))


(defrecord DBWriter [in out]
  component/Lifecycle
  (start [{:keys [in out dynamodb es] :as c}]
    (let [in-mult (mult in)]
      (handler/processing-step [(tap in-mult (chan)) nil]
                               (handler/auditor-entry "Written to db" es))
      (handler/processing-step [(tap in-mult (chan)) nil]
                               log/debug))
    c)
  (stop [c]
    (assoc c :in nil :out nil :dynamodb nil)))
