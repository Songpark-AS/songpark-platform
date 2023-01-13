(ns platform.model.fx
  (:require [clojure.string :as str]
            [ez-database.core :as db]
            [ez-database.transform :as transform]
            [taoensso.timbre :as log]))


(defn kw->fx-key [k]
  (let [ns* (namespace k)
        n* (name k)]
    (str ns* "/" n*)))

(defn fx-key->kw [k]
  (keyword k))


(defn prepare-presets [preset-id kvs]
  (->> kvs
       (filter #(= preset-id (:preset_id %)))
       (group-by (fn [v]
                   (let [parts (-> v :fx_key fx-key->kw namespace (str/split #"\."))
                         ns* (str/join "." (butlast parts))
                         n* (last parts)]
                     (keyword ns* n*))))
       (mapv (fn [[fx-type values]]
               (merge {:fx/type (keyword fx-type)}
                      (->> values
                           (map (fn [{:keys [fx_key value]}]
                                  {(fx-key->kw fx_key) value}))
                           (into {})))))))

(comment
  (kw->fx-key :fx.input.amplify/drive)
  (let [preset-id 4
        kvs [{:preset_id 4, :fx_key "fx.input1.amplify/drive", :value 1}
             {:preset_id 4, :fx_key "fx.input1.amplify/tone", :value 1}
             {:preset_id 4, :fx_key "fx.input1.echo/delay-time", :value 100}
             {:preset_id 4, :fx_key "fx.input1.echo/level", :value 50}
             {:preset_id 5, :fx_key "fx.input1.echo/delay-time", :value 123}
             {:preset_id 5, :fx_key "fx.input1.echo/level", :value 45}]]
    (prepare-presets preset-id kvs))
  )

(defn presets [db user-id]
  (let [presets (->> {:select [:id :name]
                      :from [:fx_preset]
                      :where [:= :user_id user-id]}
                     (db/query db))
        kvs (if (seq presets)
              (db/query db {:select [:preset_id :fx_key :value]
                            :from [:fx_value]
                            :where [:in :preset_id (map :id presets)]})
              nil)]
    (if kvs
      (reduce (fn [out {:keys [id name]}]
                (conj out {:fx.preset/id id
                           :fx.preset/name name
                           :fx/fxs (prepare-presets id kvs)}))
              [] presets)
      [])))

(defn preset [db user-id preset-id]
  (let [preset (->> {:select [:id :name]
                      :from [:fx_preset]
                      :where [:and
                              [:= :user_id user-id]
                              [:= :id preset-id]]}
                     (db/query db)
                     first)
        kvs (db/query db {:select [:preset_id :fx_key :value]
                          :from [:fx_value]
                          :where [:= :preset_id preset-id]})]
    {:fx.preset/id (:id preset)
     :fx.preset/name (:name preset)
     :fx/fxs (prepare-presets (:id preset) kvs)}))

(defn save-preset [db user-id {:fx.preset/keys [name]
                               :keys [fx/fxs]}]
  (db/with-transaction [db :default]
    (let [{preset-id :id :as _saved} (->> {:insert-into :fx_preset
                                           :values [{:name name
                                                     :user_id user-id}]}
                                          (db/query<! db)
                                          first)]
      (let [values (->> fxs
                        (map (fn [coll]
                               (map (fn [[k v]]
                                      {:preset_id preset-id
                                       :fx_key (kw->fx-key k)
                                       :value v})
                                    (dissoc coll :fx/type))))
                        (flatten))]
        (when-not (empty? values)
          (db/query! db {:insert-into :fx_value
                         :values values})))
      (preset db user-id preset-id))))

(defn update-preset [db user-id {:fx.preset/keys [id name]
                                 :keys [fx/fxs]}]
  (db/with-transaction [db :default]
    (when name
      (->> {:update :fx_preset
            :set {:name name}
            :where [:and
                    [:= :id id]
                    [:= :user_id user-id]]}
           (db/query! db)))
    (db/query! db {:delete-from :fx_value
                   :where [:and
                           [:= :preset_id id]]})
    (let [values (->> fxs
                      (map (fn [coll]
                             (map (fn [[k v]]
                                    {:preset_id id
                                     :fx_key (kw->fx-key k)
                                     :value v})
                                  (dissoc coll :fx/type))))
                      (flatten))]
      (when-not (empty? values)
        (db/query! db {:insert-into :fx_value
                       :values values})))
    (preset db user-id id)))

(defn delete-preset [db user-id {:fx.preset/keys [id]}]
  (try
    (db/with-transaction [db :default]
     (db/query! db {:delete-from :fx_value
                    :where [:= :preset_id id]})
     (db/query! db {:delete-from :fx_preset
                    :where [:= :id id]}))
    true
    (catch Exception e
      (log/error ::delete-preset {:exception e
                                  :message (ex-message e)
                                  :data (ex-data e)})
      false)))


(comment
  (let [db (:database @platform.init/system)]
    #_(save-preset db 1 {:fx.preset/name "test 1"
                       :fx/presets [{:fx.input1.amplify/drive 1
                                     :fx.input1.amplify/tone 1}

                                    {:fx.input1.echo/delay-time 100
                                     :fx.input1.echo/level 50}]})
    #_(update-preset db 1 {:fx.preset/name "test 2"
                         :fx.preset/id 4
                         :fx/presets [{:fx.input1.amplify/drive 1
                                       :fx.input1.amplify/tone 1}

                                      {:fx.input1.gate/threshold 1
                                       :fx.input1.gate/release 2
                                       :fx.input1.gate/attack 200}

                                      {:fx.input1.echo/delay-time 100
                                       :fx.input1.echo/level 50}]})
    #_(delete-preset db 1 {:fx.preset/id 2})
    #_(presets db 1)
    (preset db 1 13))

  (let [preset-id 1
        presets [{:fx.input1.amplify/drive 1
                  :fx.input1.amplify/tone 1}

                 {:fx.input1.echo/delay-time 100
                  :fx.input1.echo/level 50}]]
    (->> presets
         (map (fn [coll]
                (map (fn [[k v]]
                       {:preset_id preset-id
                        :fx_key (kw->fx-key k)
                        :value v})
                     coll)))
         (flatten))
    )
  )
