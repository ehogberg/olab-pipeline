(ns pipeline.core
  (:gen-class)
  (:require [clojure.tools.logging :as log ]
            [com.stuartsierra.component :as component]
            [pipeline.component :refer [new-Foo]]))


(defn pipeline-system []
  (component/system-map
   :foo (new-Foo)))

(def system (pipeline-system))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system component/stop))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (log/info "Starting pipeline...")
  (start))
