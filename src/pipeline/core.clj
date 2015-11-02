(ns pipeline.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [clojure.core.async :refer [>!!]]
            [pipeline
             [component :refer [new-Elasticsearch new-Channels
                                new-Handlers new-DynamoDB]]
             [handler :as handler]]))


;;
;; Setup all the messaging queues we'll need for our pipeline.
;;
(defn pipeline-system []
  (component/system-map
   :dynamodb (new-DynamoDB {:access-key "FOO"
                            :secret-key "BAR"})
   :es (new-Elasticsearch {:uri "http://localhost:9200"})
   :channels (component/using (new-Channels {}) [])
   :handlers  (component/using (new-Handlers {}) [:channels])))

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

(defn send-something [something]
  (let [in (-> system
               :handlers
               :in)]
    (>!! in something )))
