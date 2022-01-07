(ns platform.tpstatus
  (:require [platform.config :refer [config]]))

(defonce teleporter-statuses (atom {}))

(defn handle-teleporter-heartbeat [tp-id]
  (let [offline-timeout (get-in config [:teleporter :offline-timeout])]

    ;; cancel previous offline-timeout if exist
    (when-let [offline-timer (get-in @teleporter-statuses [tp-id :offline-timer])]
      (future-cancel offline-timer))

    ;; set online-status to online for this teleporter
    (swap! teleporter-statuses assoc-in [tp-id :online?] true)

    ;; create new offline-timeout that changes status to offline
    (swap! teleporter-statuses assoc-in [tp-id :offline-timer] (future
                                                                 (Thread/sleep offline-timeout)
                                                                 (swap! teleporter-statuses assoc-in [tp-id :online?] false)))))

(defn teleporter-online? [tp-id]
  (get-in @teleporter-statuses [(str tp-id) :online?]))
