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
        [:fx.input1.gate/threshold :fx.input2.gate/threshold]
        [:fx.input1.gate/attack :fx.input2.gate/attack]
        [:fx.input1.gate/release :fx.input2.gate/release]
        ;; reverb
        [:fx.input1.reverb/mix :fx.input2.reverb/mix]
        [:fx.input1.reverb/damp :fx.input2.reverb/damp]
        [:fx.input1.reverb/room-size :fx.input2.reverb/room-size]
        ;; amplify
        [:fx.input1.amplify/drive :fx.input2.amplify/drive]
        [:fx.input1.amplify/tone :fx.input2.amplify/tone]
        ;; equalizer
        [:fx.input1.equalizer/low :fx.input2.equalizer/low]
        [:fx.input1.equalizer/medium-low :fx.input2.equalizer/medium-low]
        [:fx.input1.equalizer/medium-high :fx.input2.equalizer/medium-high]
        [:fx.input1.equalizer/high :fx.input2.equalizer/high]
        ;; echo
        [:fx.input1.echo/delay-time :fx.input2.echo/delay-time]
        [:fx.input1.echo/level :fx.input2.echo/level]
        ;; compressor
        [:fx.input1.compressor/threshold :fx.input2.compressor/threshold]
        [:fx.input1.compressor/ratio :fx.input2.compressor/ratio]
        [:fx.input1.compressor/attack :fx.input2.compressor/attack]
        [:fx.input1.compressor/release :fx.input2.compressor/release]]
       (flatten)
       (map kw->fx-key)))

(comment

  (migratus/create (migratus-config) "add-preset-keys")
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
