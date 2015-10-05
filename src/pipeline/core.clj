(ns pipeline.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [langohr.basic :refer [publish]]
            [pipeline.auditor :refer [new-auditor]]
            [pipeline.component :refer [new-Rabbit new-Elasticsearch
                                        new-queue-processor]]))


(defn pipeline-system []
  (component/system-map
   :rmq (new-Rabbit {:uri nil})
   :es (new-Elasticsearch {:uri "http://localhost:9200"})
   :auditor (component/using
             (new-auditor {:queue-name "pipeline.auditor"
                           :exchange-name "pipeline-audit"
                           :pool-size 25})
             [:rmq :es])
   :intake (component/using
            (new-queue-processor {:queue-name "pipeline.intake"
                                  :exchange-name "pipeline-entry"
                                  :handler (fn [ch _ payload]
                                             (publish ch "pipeline-audit" ""
                                                      payload))})
            [:rmq])))

(def ^:private system (pipeline-system))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system component/stop))

(defn -main [& args]
  (log/info "Starting pipeline...")
  (start)
  (log/info "Pipeline started."))
