(ns platform.model.room
  (:require [clojure.string :as str]
            [ez-database.core :as db]
            [ez-database.transform :as transform]
            [platform.util :as util]
            [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]]))

(defqueries "queries/room.sql")

(transform/add :room :room/room
               [:id :room/id]
               [:name :room/name]
               [:name_normalized :room/name-normalized]
               [:user_id :room/owner]
               [:last_jammed :room/last-jammed]
               [:jammers :room/last-jammers])

(defn name-exists?
  ([db name]
   (let [name-normalized (util/normalize-string name)]
     (->> ["SELECT id FROM room_room WHERE LOWER(REPLACE(name, ' ', '')) = ?" name-normalized]
          (db/query db)
          first
          some?)))
  ([db exclude-room-id name]
   (let [name-normalized (util/normalize-string name)]
     (->> ["SELECT id FROM room_room WHERE LOWER(REPLACE(name, ' ', '')) = ? AND id <> ?" name-normalized exclude-room-id]
          (db/query db)
          first
          some?))))

(defn id-exists? [db room-id]
  (->> {:select [:id]
        :from [:room_room]
        :where [:= :id room-id]}
       (db/query db)
       first
       some?))

(defn is-owner? [db room-id user-id]
  (->> {:select [:user_id]
        :from [:room_user]
        :where [:and
                [:= :room_id room-id]
                [:= :user_id user-id]]}
       (db/query db)
       first
       some?))

(defn get-rooms [db user-id]
  (->> {:user_id user-id}
       (db/query db ^:opts {[:transformation :post]
                            [:room :room/room {:nil false}]} sql-get-rooms)))

(defn get-jammed [db user-id]
  (->> {:user_id user-id}
       (db/query db ^:opts {[:transformation :post]
                            [:room :room/room {:nil false}]} sql-get-jammed)))

(defn get-room [db room-id]
  (->> {:select [:r.* :ru.user_id]
        :from [[:room_room :r]]
        :join [[:room_user :ru] [:= :r.id :ru.room_id]]
        :where [:= :ru.room_id room-id]}
       (db/query db ^:opts {[:transformation :post]
                            [:room :room/room {:nil false}]})
       first))

(defn get-room-by-name [db room-name]
  (->> {:select [:r.* :ru.user_id]
        :from [[:room_room :r]]
        :join [[:room_user :ru] [:= :r.id :ru.room_id]]
        :where [:= :r.name_normalized (util/normalize-string room-name)]}
       (db/query db ^:opts {[:transformation :post]
                            [:room :room/room]})
       first))


(defn save-room [db user-id {:keys [room/name]}]
  (db/with-transaction [db :default]
    (let [room-id (->> {:insert-into :room_room
                        :values [{:name (util/trim-string name)
                                  :name_normalized (util/normalize-string name)}]}
                       (db/query<! db ^:opts {[:transformation :pre]
                                              [:room/room :room]})
                       first
                       :id)]
      (db/query! db {:insert-into :room_user
                     :values [{:user_id user-id
                               :room_id room-id}]})
      (get-room db room-id))))

(defn update-room [db {:room/keys [id name]}]
  (db/with-transaction [db :default]
    (->> {:update :room_room
          :set {:name (util/trim-string name)
                :name_normalized (util/normalize-string name)}
          :where [:= :id id]}
         (db/query! db))
    (get-room db id)))

(comment
  (let [db (:database @platform.init/system)]
    ;;(is-owner? db 19 1)
    #_(get-room-by-name db "asdf")
    #_(save-room db 1 {:room/name "My awesome meh "})
    #_(update-room db {:room/id 19
                     :room/name "foobar"})
    #_(-rooms db 1)
    #_(get-room db 17)
    #_(name-exists? db "My AWESOME foobar ! ")
    ;;(get-room db 2)
    (get-jammed db 3))
  )
