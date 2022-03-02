(ns platform.tpstatus
  (:require [platform.config :refer [config]]
            [platform.db.store :as db]
            [taoensso.timbre :as log]
            [tick.alpha.api :as t]))

(defn handle-teleporter-heartbeat [tp-id]
  (let [now (t/now)]
    ;; store timestamp
    (db/wr [:teleporter tp-id :teleporter/heartbeat-timestamp] now)))

(defn teleporter-online? [tp-id]
  (let [teleporter (db/rd [:teleporter tp-id])
        now (t/now)
        timeout (get-in config [:teleporter :offline-timeout] (* 60 1000))]
    (if-let [heartbeat-timestamp (:teleporter/heartbeat-timestamp teleporter)]
      (t/< now (t/>> heartbeat-timestamp (t/new-duration timeout :millis)))
      false)))
