(ns dev
  (:require [platform.init :as init]
            [platform.test.util :refer [seed-db!]]
            [ez-database.core :as db]
            [taoensso.timbre :as log]
            [platform.model.examination :as model.examination]
            [platform.api.examination :as api.examination]))

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

  ;; examinations
  (let [db (get-in @init/system [:database])]
    (db/query db {:select [:e.id, :e.name, :e.pass_percentage]
                  :from [[:examination_examination :e]]
                  :order-by [[:e.id :asc]]}))

  (let [db (get-in @init/system [:database])]
    (model.examination/examinations db))

  (let [db (get-in @init/system [:database])
        d {:data {:database db}}]
    (api.examination/examinations d))


  )
