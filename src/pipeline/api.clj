(ns pipeline.api
  (:require [clojure.core.async :refer [>!!] ]
            [compojure.api.sweet :refer :all]
            [pipeline.auditor :refer [purge-audit-table audit-trail]]
            [pipeline.dydb :as dydb]
            [pipeline.postgres :as postgres]
            [pipeline.handler :as handler]
            [ring.util.http-response :refer :all]
            [schema.core :refer [defschema] :as s ]))


(defschema Response {:id s/Uuid
                     :created_at java.util.Date
                     :company_id Number
                     :comment String})
(defschema NewResponse (dissoc Response :id :created_at :company_id))
(defschema Checkpoint {:id s/Uuid
                       :response {:response-id s/Uuid
                                  :resp-tx-id s/Uuid
                                  :comment String
                                  :added-at java.util.Date}})

(defroutes* audit-routes
  (context* "/audit" []
            :tags ["Audit"]
            :components [es]
            (GET* "/transaction/:transact-id" []
                  :path-params [transact-id :- String]
                  (ok (audit-trail es transact-id)))
            (POST* "/purge" []
                   :summary "Empties the audit table"
                   (ok (purge-audit-table es)))))


(defroutes* checkpoint-routes
  (context* "/checkpoint" []
            :tags ["Checkpoint"]
            :components [dynamodb]
            (GET* "/pending" []
                  :return [Checkpoint]
                  :summary "Lists all responses awaiting pipeline processing."
                  (ok (dydb/get-checkpoints dynamodb)))
            (POST* "/purge" []
                   :summary "Purges the checkpoint table."
                   (ok (select-keys
                        (do (dydb/delete-tables dynamodb)
                            (dydb/create-tables dynamodb))
                        [:item-count :status])))))

(defroutes* response-routes
  (context* "/response" []
            :tags ["Responses"]
            (GET* "/all" []
                  :components [postgres]
                  (ok (postgres/all-responses postgres)))
            (GET* "/:resp-id" []
                  :path-params [resp-id :- s/Uuid]
                  :components [postgres]
                  :return Response
                  :summary "Returns details about a response."
                  (ok (postgres/get-response postgres resp-id)))
            (POST* "/purge" []
                   :components [postgres]
                   (ok (postgres/purge-tables postgres)))
            (POST* "/process" []
                   :body [resp (describe NewResponse "New response")]
                   :components [dynamodb intake-channel]
                   :summary "Submits a response for pipeline processing"
                   (let [new-resp (handler/add-init-properties resp)]
                     (dydb/write-to-checkpoint new-resp dynamodb)
                     (>!! intake-channel new-resp)
                     (ok new-resp)))))

(defapi api-routes
 (swagger-ui)
 (swagger-docs
  {:info {:title "Pipeline POC"}})
 (context* "/api" []
           audit-routes
           checkpoint-routes
           response-routes))
