(ns pipeline.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [clojure.core.async :refer [>!! chan]]
            [pipeline
             [component :refer [map->Elasticsearch map->DynamoDB
                                map->WebServer map->ResponseIntake
                                map->DBWriter]]]))


(defn pipeline-system []
  (let [response-intake-chan (chan 1000)
        db-writer-chan (chan 1000)]
    (component/system-map
     :dynamodb        (map->DynamoDB {:access-key "FOO"
                                      :secret-key "BAR"
                                      :endpoint "http://localhost:8000"})
     :web             (component/using (map->WebServer {:port 3001})
                                       [:dynamodb :intake-channel :es])
     :es              (map->Elasticsearch {:uri "http://localhost:9200"})
     :intake-channel  response-intake-chan
     :response-intake (component/using
                       (map->ResponseIntake {:in response-intake-chan
                                             :out db-writer-chan})
                       [:es])
     :db-writer       (component/using
                       (map->DBWriter {:in db-writer-chan
                                       :out nil})
                       [:dynamodb :es]))))

(def ^:private system (pipeline-system))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system component/stop))

(defn restart []
  (stop)
  (start))

(defn reset []
  (stop)
  (alter-var-root #'system (constantly (pipeline-system)))
  (start))

(defn -main [& args]
  (log/info "Starting pipeline...")
  (start)
  (log/info "Pipeline started."))
