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
                 [clojurewerkz/elastisch "2.2.0-beta4"]]
  :main ^:skip-aot pipeline.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
