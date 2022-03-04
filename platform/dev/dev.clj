(ns dev
  (:require [platform.init :as init]
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
    (-> db :kv-map deref))

  (let [db (get-in @init/system [:http-server :db])]
    (songpark.jam.platform.protocol/delete-db db [:teleporters])))
