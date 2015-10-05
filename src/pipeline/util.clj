(ns pipeline.util
  (:require [cheshire.core :refer [generate-string]]
            [com.stuartsierra.component :as component]
            [langohr.basic :refer [publish]]
            [pipeline.component :refer [new-Rabbit new-channel]]
            [pipeline.factory :refer [random-response]]))

(defn publish-some-responses
  ([] (publish-some-responses 10))
  ([n]
   (let [ch (-> {:uri nil}
                new-Rabbit
                component/start
                new-channel)]
     (doseq [r (take n (random-response))]
       (publish ch "pipeline-entry" "" (generate-string r))))))
