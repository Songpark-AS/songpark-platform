(defproject fakephone "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "1.0.206"]
                 [http-kit "2.3.0"]
                 [org.clojure/data.json "2.4.0"]
                 [songpark/common "0.1.0"]]
  :repl-options {:init-ns fakephone.core}
  :main fakephone.core
  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[lein-binplus "0.6.6"]]}})
