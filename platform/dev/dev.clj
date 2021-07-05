(ns dev
  (:require [platform.init :as init]
            [platform.test.util :refer [seed-db!]]
            [ez-database.core :as db]
            [taoensso.timbre :as log]))

(defn restart
  "stop and start songpark"
  []
  ;; set the log level to info or jetty will spam your REPL console,
  ;; significantly slowing down responses
  (log/merge-config! {:level        :debug
                      :ns-blacklist ["org.eclipse.jetty.*"
                                     "io.grpc.netty.shaded.io.netty.*"
                                     "org.opensaml.*"]})
  (init/stop)
  (init/init))

(defn reseed
  ";; seed database"
  []
  (let [db (get-in @init/system [:database])]
    (seed-db! db)))

(comment
  ;; stop and start songpark
  (restart)

  ;; seed database
  (reseed)

  ;; how to quickly test something in the database
  (let [db (get-in @init/system [:database])]
    (db/query db {:select [:*] :from [:assignment_assignment]}))
  )
