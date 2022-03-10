(ns dev
  (:require [platform.init :as init]
            [songpark.jam.platform.protocol :as proto]
            [taoensso.timbre :as log]))

(defn restart
  "stop and start songpark"
  []
  ;; set the log level to info or jetty will spam your REPL console,
  ;; significantly slowing down responses
  (log/merge-config! {:min-level :debug
                      :ns-filter {:deny #{"org.eclipse.jetty.*"
                                          "io.grpc.netty.shaded.io.netty.*"}
                                  :allow #{"*"}}})
  (init/stop)
  (init/init))

(comment
  ;; stop and start songpark
  (init/stop)
  (restart)

  (-> @init/system
      :mqtt-client
      :topics
      deref)

  (let [db (get-in @init/system [:http-server :db])]
    (proto/read-db db [:teleporter #uuid "7fdf0551-b5fc-557d-bddc-2ca5b1cdfaa6" :volume/global-volume]))
  (let [db (get-in @init/system [:http-server :db])]
    (proto/write-db db [:waiting] {}))

  (let [db (get-in @init/system [:http-server :db])]
    (proto/read-db db [:waiting]))

  (let [db (get-in @init/system [:http-server :db])]
    (proto/read-db db [:jam]))

  (let [db (get-in @init/system [:http-server :db])]
    (proto/write-db db [:teleporter-fw-version] "0.0.10"))

  (let [db (get-in @init/system [:http-server :db])]
    (songpark.jam.platform.protocol/delete-db db [:teleporters])))
