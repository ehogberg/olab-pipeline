(ns pipeline.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [pipeline
             [auditor :refer [new-auditor]]
             [component :refer [new-Elasticsearch new-queue-processor
                                new-Rabbit]]
             [handler :as handler]]))

(defn pipeline-system []
  (component/system-map
   :rmq (new-Rabbit {:uri nil})
   :es (new-Elasticsearch {:uri "http://localhost:9200"})
   :auditor (component/using
             (new-auditor {:queue-name "pipeline.auditor"
                           :exchange-name "pipeline-audit"
                           :pool-size 25})
             [:rmq :es])
   :capitalizer (component/using
                 (new-queue-processor {:queue-name "pipeline.capitalizer"
                                       :handler handler/capitalize-handler
                                       :auto-ack true})
                 [:rmq])
   :global-filter (component/using
                   (new-queue-processor {:queue-name "pipeline.global-filter"
                                         :exchange-name "pipeline-processing"
                                         :routing-key "global-filter"
                                         :handler handler/dumper-handler})
                   [:rmq])
   :intake (component/using
            (new-queue-processor {:queue-name "pipeline.intake"
                                  :exchange-name "pipeline-entry"
                                  :handler handler/intake-handler})
            [:rmq])))

(def ^:private system (pipeline-system))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system component/stop))

(defn restart []
  (stop)
  (start))

(defn -main [& args]
  (log/info "Starting pipeline...")
  (start)
  (log/info "Pipeline started."))
