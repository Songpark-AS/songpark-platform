(ns platform.tpstatus
  (:require [platform.config :refer [config]]
            [platform.db.store :as db]
            [taoensso.timbre :as log]
            [tick.alpha.api :as t]))

(defn- get-tp-mac-from-uuid [uuid]
  (:teleporter/mac (last (first (filter (fn[[_ v]] (= (str uuid) (str (:teleporter/uuid v)))) (db/rd [:teleporter]))))))

(defn handle-teleporter-heartbeat [tp-id]
  (let [tp-mac (get-tp-mac-from-uuid tp-id)
        now (t/now)]
    ;; store timestamp
    (db/wr [:teleporter tp-mac :teleporter/heartbeat-timestamp] now)))

(defn teleporter-online? [tp-id]
  (let [tp-mac (get-tp-mac-from-uuid tp-id)
        teleporter (db/rd [:teleporter tp-mac])
        now (t/now)]
    (if-let [heartbeat-timestamp (:teleporter/heartbeat-timestamp teleporter)]
      (t/< now (t/>> heartbeat-timestamp (t/new-duration (get-in config [:teleporter :offline-timeout]) :seconds)))
      false)))
