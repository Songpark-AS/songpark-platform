(ns fakephone.core
  (:require [clojure.tools.cli :refer [parse-opts] :as cli]
            [org.httpkit.client :as http]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [common.connect.client :as connect.client])
  (:gen-class))

(def cli-options
  [["-c" "--connect-to-endpoint ENDPOINT" "The address of the endpoint resource you are trying to access"
    :id :connect-to-endpoint
    :default "http://localhost:3000/connect/client/init"]
   ["-n" "--nickname NICKNAME" "The nickname of the teleporter you are trying to connect to"
    :id :nickname]
   ["-h" "--help" "Displays this screen"
    :id :help]])

(defn -main
  [& cli-args]
  (let [input (cli/parse-opts cli-args cli-options)
        endpoint (get-in input [:options :connect-to-endpoint])
        nickname (get-in input [:options :nickname])
        help (get-in input [:options :help])]
    (if help
      (println (:summary input))
      (let [respond-params (connect.client/init {:nickname nickname})
            status (:status respond-params)
            status-desc (:status-desc respond-params)
            username (:mqtt-username respond-params)
            password (:mqtt-password respond-params)]
        (println "Connecting to endpoint:" endpoint)
        (case status
          nil (println "ERROR: No status retrieved. Possibly bad endpoint")
          "success" (do
                      (println "SUCCESS: Tp is available, with the following MQTT credentials:")
                      (println "MQTT-username: " username)
                      (println "MQTT-password: " password))
          status-desc)))))