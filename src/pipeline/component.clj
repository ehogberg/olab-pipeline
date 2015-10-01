(ns pipeline.component
  (:require [clojure.tools.logging :as log ]
            [com.stuartsierra.component :as component]))


(defrecord Foo []
  component/Lifecycle
  (start [component]
    (log/debug "Starting Foo")
    component)
  (stop [component]
    (log/debug "Stopping Foo")
    component))

(defn new-Foo [] (Foo.))
