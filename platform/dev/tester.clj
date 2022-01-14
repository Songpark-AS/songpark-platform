(ns tester
  (:gen-class)
  (:require [platform.init :as init]
            [platform.test.util :refer [seed-db!]]
            [ez-database.core :as db]
            [taoensso.timbre :as log]))



(defn -main [& args]
  (log/set-level! :info)
  (init/init)
  (log/info "Seeding the database")
  (let [db (get-in @init/system [:database])]
    (seed-db! db)
    (log/info "\n\n--Seeding finished--\n\nServer is ready for tests"))
  )

(comment

  ;; stop and start songpark
  (do
    ;; set the log level to info or jetty will spam your REPL console,
    ;; significantly slowing down responses
    (log/set-level! :info)
    (init/stop)
    (init/init))

  ;; seed database
  (let [db (get-in @init/system [:database])]
    (seed-db! db))
  
  )

