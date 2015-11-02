(defproject pipeline "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.3.0"]
                 [com.novemberain/langohr "3.4.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [slf4j-logger "0.1.0-SNAPSHOT"]
                 [clojurewerkz/elastisch "2.2.0-beta4"]
                 [clj-factory "0.2.2-SNAPSHOT"]
                 [clojure.joda-time "0.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [com.taoensso/faraday "1.8.0"]]
  :plugins [[lein-autoreload "0.1.0"]]
  :main ^:skip-aot pipeline.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
