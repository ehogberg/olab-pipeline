(ns pipeline.factory
    (:require [clj-factory.core :refer [deffactory factory]]))


(deffactory :response
  {:company (rand-nth ["Acme" "Ace" "Kwality" "Meh"])
   :rating (rand-nth (range 1 6))
   :comment "Some useless text"})

(defn random-response []
  (lazy-seq (cons (factory :response) (random-response))))
