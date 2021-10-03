(ns dev
  (:require [platform.init :as init]
            #_[platform.test.util :refer [seed-db!]]
            #_[ez-database.core :as db]
            [taoensso.timbre :as log]))

(defn restart
  "stop and start songpark"
  []
  ;; set the log level to info or jetty will spam your REPL console,
  ;; significantly slowing down responses
  (log/merge-config! {:min-level :debug
                      :ns-filter {:deny #{"org.eclipse.jetty.*"
                                          "io.grpc.netty.shaded.io.netty.*"
                                          "org.opensaml.*"}
                                  :allow #{"*"}}})
  (init/stop)
  (Thread/sleep 500)
  (init/init))

#_(defn reseed
    ";; seed database"
    []
    (let [db (get-in @init/system [:database])]
      (seed-db! db)))

(comment  
  ;; stop and start songpark
  (init/stop)
  (restart)
  
  ;; seed database
  ;;(reseed)

  ;; how to quickly test something in the database
  (let [db (get-in @init/system [:database])]
    (db/query db {:select [:*] :from [:assignment_assignment]}))


  
  )
