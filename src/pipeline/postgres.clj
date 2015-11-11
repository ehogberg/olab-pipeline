(ns pipeline.postgres
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log ]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all ]))

(defn normalize-response [response]
  (-> response
      (select-keys [:response-id :added-at :comment :company-id])
      vals))


(defn create-response [response db]
  (try
    (jdbc/execute! db (-> (insert-into :responses)
                          (columns :id :created_at :comment :company_id)
                          (values [(normalize-response response)])
                          (sql/format)))
    (catch Exception e
      (do
        (log/warn e)
        (log/warn (.getNextException e))))))

(defn all-responses [db]
  (jdbc/query db (-> (select :*)
                     (from :responses)
                     (order-by :created_at)
                     (sql/format))))

(defn get-response [db response-id]
  (first (jdbc/query
          db
          (-> (select :*)
              (from :responses)
              (where [:= :id response-id])
              (sql/format)))))

(defn drop-tables [db]
  (jdbc/db-do-commands db (jdbc/drop-table-ddl :responses)))

(defn create-tables [db]
  (jdbc/db-do-commands db (jdbc/create-table-ddl :responses
                                                 [:id  "UUID" :primary :key]
                                                 [:company_id "integer"]
                                                 [:created_at "timestamp"]
                                                 [:comment    "text"])))

(defn purge-tables [db]
  (drop-tables db)
  (create-tables db))
