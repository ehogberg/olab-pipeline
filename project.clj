(defproject pipeline "0.1.0-SNAPSHOT"
  :description "POC demonstrating a core.async/component-centric
                pipeline for response intake."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.3.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clojurewerkz/elastisch "2.2.0-beta4"]
                 [clj-factory "0.2.2-SNAPSHOT"]
                 [clojure.joda-time "0.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.taoensso/faraday "1.8.0"]
                 [metosin/compojure-api "0.23.1"]
                 [prismatic/schema "1.0.3"]
                 [metosin/schema-tools "0.6.2"]]
  :plugins [[lein-autoreload "0.1.0"]]
  :main ^:skip-aot pipeline.core
  :target-path "target/%s"
  :jvm-opts ["-Dlog4j.configuration=pipeline.log4j.properties" "-Dlog4j.debug"]
  :profiles {:uberjar {:aot :all}})
