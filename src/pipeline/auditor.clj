(ns pipeline.auditor
  (:require [clojure.tools.logging :as log ]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esrd]
            [clojurewerkz.elastisch.rest.index :as esri]
            [clojurewerkz.elastisch.rest.response :as esrr]
            [com.stuartsierra.component :as component ]))


(defn audit-trail [{:keys [connection]} transact-id]
  (map :_source
       (-> connection
           (esrd/search "pipeline-audit" "audit-entry"
                        :filter (q/term :resp-tx-id transact-id)
                        :sort {:audited-at "desc"})
           (esrr/hits-from))))

(defn- delete-audit-table [connection]
  (esri/delete connection "pipeline-audit"))

(defn- create-audit-table [connection]
  (esri/create
   connection "pipeline-audit"
   {:mappings {"audit-entry"
               {:properties {:resp-tx-id  {:type "string" :index "not_analyzed"}
                             :response-id {:type "string" :index "not_analyzed"}
                             :audited-at  {:type "date"}}}}}))

(defn purge-audit-table [{:keys [connection]}]
  (delete-audit-table connection)
  (create-audit-table connection))
