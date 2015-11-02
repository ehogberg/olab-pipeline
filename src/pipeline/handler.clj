(ns pipeline.handler
  (:require [clojure.tools.logging :as log ]
            [clojure.core.async :refer [chan go go-loop <! >! >!!]]
            [joda-time.instant :refer [date-time]]))


(defmacro processing-step [[in out] body]
  `(let [in#  ~in
         out# ~out]
     (go-loop []
      (when-let [val# (<! in#)]
        (let [resp# (-> val#
                        ~body)])
        (if out#
          (>! out# resp#))
        (recur)))))


(defn add-init-properties [resp]
  (assoc resp
         :id       (.toString (java.util.UUID/randomUUID))
         :added-at (.toString (date-time))))

(defn add-global-filter-properties [resp]
  (assoc resp :global-filter-applied true))
