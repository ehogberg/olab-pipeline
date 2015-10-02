(ns pipeline.core
  (:gen-class)
  (:require [clojure.tools.logging :as log ]
            [com.stuartsierra.component :as component]
            [pipeline.component :refer [new-Rabbit new-Elasticsearch
                                        new-queue-processor]]))


(defn pipeline-system []
  (component/system-map
   :rmq (new-Rabbit {:uri nil})
   :es (new-Elasticsearch {:uri "http://localhost:9250"})
   :qp (component/using
        (new-queue-processor {:queue-name "foo"
                              :handler (fn [& args] (println args))})
        [:rmq])))

(def ^:private system (pipeline-system))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system component/stop))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (log/info "Starting pipeline...")
  (start))
