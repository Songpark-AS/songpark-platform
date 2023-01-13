(ns migrations
  (:require [clojure.string :as str]
            [platform.model.fx :refer [kw->fx-key]]
            [platform.init :as init]
            [platform.migrator :refer [get-migration-map]]
            [migratus.core :as migratus]))

(defn migratus-config []
  (let [ezdb (get-in @init/system [:database])
        ds (get-in @init/system [:database :db-specs :default :datasource])
        mmap (get-migration-map ezdb ds)]
    (assert (some? ds) "The system needs to be initialized in order to work with migrations")
    mmap))

(defn generate-fx-keys []
  (->> [;; gate
        [:fx.gate/threshold]
        [:fx.gate/attack]
        [:fx.gate/release]
        ;; reverb
        [:fx.reverb/mix]
        [:fx.reverb/damp]
        [:fx.reverb/room-size]
        ;; amplify
        [:fx.amplify/drive]
        [:fx.amplify/tone]
        ;; equalizer
        [:fx.equalizer/low]
        [:fx.equalizer/medium-low]
        [:fx.equalizer/medium-high]
        [:fx.equalizer/high]
        ;; echo
        [:fx.echo/delay-time]
        [:fx.echo/level]
        ;; compressor
        [:fx.compressor/threshold]
        [:fx.compressor/ratio]
        [:fx.compressor/attack]
        [:fx.compressor/release]]
       (flatten)
       (map kw->fx-key)))

(comment

  (migratus/create (migratus-config) "room-room-normalized-name")
  (migratus/migrate (migratus-config))
  (migratus/rollback (migratus-config))

  (let [ks (generate-fx-keys)
        delete-fx-values (str "DELETE FROM fx_value WHERE fx_key IN ('"
                              (str/join "', '" ks)
                              "');")
        delete-fx-ks (->> ks
                          (map (fn [k]
                                 (str "DELETE FROM fx_key VALUES WHERE id = '" k "';"))))]
    (->> [delete-fx-values
          delete-fx-ks]
         (flatten)
         (str/join "\n--;;\n")
         (spit "resources/migrations/20220706132105-add-preset-keys.down.sql")))

  (->> (generate-fx-keys)
       (map (fn [k]
              (str "INSERT INTO fx_key VALUES ('" k "');")))
       (str/join "\n--;;\n")
       (spit "resources/migrations/20220706132105-add-preset-keys.up.sql"))
  )
