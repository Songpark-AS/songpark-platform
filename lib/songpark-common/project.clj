(defproject kompetansia/common "0.6.0-SNAPSHOT"

  :description "Common functionality for frontend"

  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript "1.10.597" :scope "provided"]
                 ;; web
                 ;; reagent was here
                 ;; we want to have dev building within this library however,
                 ;; and that is easiest done with figwheel
                 ;; we use shadow-cljs everywhere else though as it has
                 ;; superior interop with the npm ecosystem
                 ;; so do a local release with lein with-profile build install
                 ;; and use things as is for dev work

                 ;; wiring (forms)
                 [ez-wire "0.5.0-beta3"]

                 ;; matching
                 [org.clojure/core.match "1.0.0"]

                 ;; i18n
                 [tongue "0.2.9"]
                 
                 ;; structure
                 [re-frame "1.1.2"]
                 [com.stuartsierra/component "1.0.0"]

                 ;; communication
                 [cljs-ajax "0.8.0"]
                 [tick "0.4.24-alpha"]

                 ;; data format
                 [com.cognitect/transit-cljs "0.8.256"]
                 
                 ;; logging
                 [com.taoensso/timbre "4.10.0"]]

  :repl-options {:init-ns kompetansia}

  :min-lein-version "2.5.3"

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}
  
  :profiles
  {:build {:source-paths ["src"]
           :dependencies [[reagent "0.10.0" :exclusions [cljsjs/react
                                                         cljsjs/react-dom
                                                         cljsjs/react-dom-server
                                                         cljsjs/create-react-class]]]}
   :dev
   {:source-paths ["src" "dev"]
    :dependencies [[binaryage/devtools "1.0.0"]
                   [day8.re-frame/re-frame-10x "0.6.0"]
                   [day8.re-frame/tracing "0.5.3"]
                   [reagent "0.10.0"]]

    :plugins [[lein-cljsbuild "1.1.7"]
              [lein-figwheel "0.5.19"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src" "dev"]
     :figwheel     {:on-jsload "kompetansia.dev/mount-root"}
     :compiler     {:main                 kompetansia.dev
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "/js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}]})
