(defproject songpark/platform "0.1.0-SNAPSHOT"

  :description "Platform for songpark"

  :url "https://github.com/Songpark-AS/songpark-platform"

  :license {:name ""
            :url ""}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.match "1.0.0"]
                 [org.clojure/core.async "1.3.618"]

                 ;; memoization
                 [org.clojure/core.memoize "1.0.236"]

                 ;; songpark
                 [songpark/common "0.3.0-SNAPSHOT"]
                 [songpark/jam "1.0.0"]
                 [songpark/taxonomy "0.3.0-SNAPSHOT"]
                 [songpark/mqtt "1.0.1"]

                 ;; routing
                 [metosin/reitit "0.4.2"]

                 ;; digest library (md5, sha, etc)
                 [digest "1.4.9"]

                 ;; http
                 [ring "1.9.4"]
                 [ring/ring-jetty-adapter "1.9.4"]
                 [ring-cors "0.1.13"]

                 ;; structure
                 [com.stuartsierra/component "1.0.0"]

                 ;; redis support
                 [com.taoensso/carmine "2.19.1"]

                 ;; logging
                 [com.taoensso/timbre "5.1.2"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]
                 [raven-clj "1.6.0"]

                 ;; configuration
                 [cprop "0.1.11"]

                 ;; encryption
                 [buddy/buddy-core "1.5.0" :exclusions [org.clojure/clojure]]
                 [buddy/buddy-hashers "1.3.0" :exclusions [org.clojure/clojure]]
                 [buddy/buddy-sign "3.0.0" :exclusions [org.clojure/clojure]]
                 [buddy/buddy-auth "2.2.0" :exclusions [org.clojure/clojure]]

                 ;; filesystem
                 [me.raynes/fs "1.4.6"]

                 ;; database
                 [ez-database "0.8.2"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [org.postgresql/postgresql "42.2.8"]
                 [hikari-cp "2.9.0"]
                 [com.github.seancorfield/honeysql "2.0.783"]
                 [yesql "0.5.3"]

                 ;; database migrations
                 [migratus "1.2.6"]

                 ;; http
                 [http-kit "2.5.3"]

                 ;; html
                 [hiccup "1.0.5"]

                 ;; utils
                 [danlentz/clj-uuid "0.1.9"]
                 [com.cemerick/url "0.1.1"]
                 [jarohen/chime "0.3.3"]]

  :uberjar-name "platform.jar"

  :main platform.core

  :repl-options {:init-ns platform.core}

  :test-paths ["test"]

  :plugins [[lein-plantuml "0.1.22"]]

  :plantuml [["../docs/UML/*.puml" :png "../docs/modules/ROOT/assets/images/UML/"]]

  :profiles {:dev {:source-paths ["src" "dev"]
                   :resource-paths ["dev-resources" "resources"]
                   :dependencies [[midje "1.9.9"]
                                  [ring/ring-mock "0.4.0"]
                                  [clj-commons/spyscope "0.1.48"]]
                   :injections [(require 'spyscope.core)]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-plantuml "0.1.22"]]}

             :uberjar {:aot [platform.core]}

             :test {:source-paths ["src"]
                    :resource-paths ["dev-resources" "resources"]
                    :dependencies [[midje "1.9.4"]
                                   [ring/ring-mock "0.4.0"]]
                    :plugins [[lein-midje "3.1.3"]]}

             :tester {:source-paths ["src" "dev" "test"]
                      :main tester
                      :aot [tester]
                      :resource-paths ["dev-resources" "resources"]}})
