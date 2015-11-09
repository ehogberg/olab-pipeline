(ns pipeline.dydb
  (:require [taoensso.faraday :as far]))


(defn write-to-checkpoint [resp db]
  (far/put-item db :checkpoints resp))

(defn get-checkpoints [db]
  (far/scan db :checkpoints))

(defn create-tables [db]
  (far/create-table db :checkpoints [:checkpoint-id :s]
                    {:gsindexes [{:name "CheckpointResponseId"
                                  :projection :all
                                  :throughput {:read 1 :write 1}
                                  :hash-keydef [:response-id :s]}]}))

(defn delete-tables [db]
  (far/delete-table db :checkpoints))
