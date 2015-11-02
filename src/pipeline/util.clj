(ns pipeline.util
  (:require [pipeline.factory :refer [random-response]]))


(defn publish-some-responses
  ([] (publish-some-responses 10))
  ([n]))
