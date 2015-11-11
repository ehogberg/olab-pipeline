(ns pipeline.dydb
  (:require [taoensso.faraday :as far]))


(defn write-to-checkpoint [resp db]
  (far/put-item db :checkpoints
                {:id (.toString (:resp-tx-id resp))
                 :response (far/freeze resp)}))

(defn get-checkpoints [db]
  (far/scan db :checkpoints))

(defn create-tables [db]
  (far/create-table db :checkpoints [:id :s] {}))

(defn delete-tables [db]
  (far/delete-table db :checkpoints))
