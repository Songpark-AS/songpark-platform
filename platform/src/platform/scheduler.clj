(ns platform.scheduler
  (:require [chime.core :as chime]
            [com.stuartsierra.component :as component]
            [songpark.jam.platform :as jam.platform]
            [taoensso.timbre :as log])
  (:import [java.time Instant Duration]))


(defn start-jobs [jobs interval-ms]
  (chime/chime-at
   (chime/periodic-seq (Instant/now)
                       (Duration/ofMillis interval-ms))
   (fn [time]
     (doseq [job jobs]
       (job time)))))


(defrecord Scheduler [started? jam-manager chimed interval-ms]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting Scheduler")
          (assoc this
                 :chimed (start-jobs [(fn [_]
                                        (jam.platform/check-for-timeouts jam-manager))]
                                     (or interval-ms (* 1 1000)))
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping Scheduler")
          (.close chimed)
          (assoc this
                 :started? false)))))


(defn scheduler [settings]
  (map->Scheduler settings))
