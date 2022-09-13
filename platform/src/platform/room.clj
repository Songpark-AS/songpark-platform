(ns platform.room
  (:require [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [platform.model.profile :as model.profile]
            [platform.model.room :as model.room]
            [platform.util :refer [id->uuid]]
            [songpark.jam.platform :as jam.platform]
            [songpark.jam.platform.protocol :as proto]
            [songpark.mqtt :as mqtt]
            [taoensso.timbre :as log]
            [tick.core :as t]))


;; db should look like this
;; {:room-id {:owner <user-id>
;;            :jammer <user-id>
;;            :session-id <session-id>
;;            :waiting #{<user-id-1> <user-id-2> <user-id-3>}
;;            :status #{:waiting :full}}}

;; room-id corresponds to the id of the room_room row in the SQL database
;; this implementation is mainly meant to be used in conjunction with a REST API
;; for an in-memory representation of what's going on
;; it's hidden behind a protocol, so it's possible to implement it with a
;; database that can save the data permantently, if the need were to arise

(defprotocol IRoom
  "Interface Database Key Value"
  (db-host [database room-id owner-id] "Host a room")
  (db-knock [database room-id jammer-id] "Knock on a room")
  (db-accept [database room-id jammer-id] "Accepted into a room")
  (db-decline [database room-id jammer-id] "Decline someone knocking on a room")
  (db-leave [database room-id jammer-id] "Leave a room")
  (db-remove [database room-id jammer-id] "Remove a jammer from a room")
  (db-close [database room-id owner-id] "Close a room")
  (db-get-room-by-host [database owner-id] "Get a specific room based on the owner's id")
  (db-get-room-by-user-id [database user-id] "Get a specific room based on either the owner, a jammer or a knocker")
  (db-get-room-by-id [database room-id])
  (db-get-rooms [database] "Get all the rooms"))

(defn- get-jammer [database jammer-id status]
  (-> (model.profile/get-profile database jammer-id)
      (dissoc :profile/id)
      (assoc :auth.user/id jammer-id
             :jammer/status status)))

(defn- can-host? [database room-id owner-id]
  (->> {:select [:rr.id]
        :from [[:room_room :rr]]
        :join [[:room_user :ru] [:= :ru.room_id :rr.id]]
        :where [:= :ru.user_id owner-id]}
       (db/query database)
       first
       :id
       some?))

(defn- already-hosting? [roomdb owner-id]
  (reduce (fn [_ {:keys [owner] :as room}]
            (if (= owner owner-id)
              (reduced true)
              false))
          false (vals @roomdb)))

(defn- get-teleporter-ids [ezdb user-id-1 user-id-2]
  (->> {:select [:teleporter_id]
        :from [:teleporter_pairing]
        :where [:in :user_id [user-id-1 user-id-2]]}
       (db/query ezdb)
       (map :teleporter_id)
       (into #{})))

(defn get-jam-id [jam-manager tp-id-1 tp-id-2]
  (let [{:keys [db]} jam-manager
        jams (vals (proto/read-db db [:jam]))
        check-for-members #{tp-id-1 tp-id-2}]
    (reduce (fn [_ {:jam/keys [members id]}]
              (if (= (set members) check-for-members)
                (reduced id)
                nil))
            nil jams)))

(defn- db-host* [{:keys [data database] :as this} room-id owner-id]
  (let [room (get @data room-id nil)]
    (cond (not (can-host? database room-id owner-id))
          {:error/key :room/cannot-host
           :error/message "The owner is not the owner of this room"}

          (= (:owner room) owner-id)
          {:error/key :room/already-hosted
           :error/message "Owner is already hosting this room"}

          (nil? owner-id)
          {:error/key :room/no-owner
           :error/message "Owner does not exist"}

          (already-hosting? data owner-id)
          {:error/key :room/already-hosting
           :error/message "The owner is already hosting another room"}

          :else
          (do
            ;; close all other rooms hosted by the same host
            (doseq [[room-id-alt {owner-id-alt :owner}] @data]
              (when (= owner-id-alt owner-id)
                (db-close this room-id-alt owner-id)))
            (swap! data assoc room-id {:owner owner-id
                                       :waiting #{}
                                       :status :waiting})
            true))))

(defn- db-accept* [{:keys [data database mqtt-client jam-manager] :as this} room-id jammer-id]
  (let [room (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (= (:jammer room) jammer-id)
          {:error/key :room/already-joined
           :error/message "User have already joined this room"}

          (= (:status room) :full)
          {:error/key :room/full
           :error/message "The room is full"}

          (= (:owner room) jammer-id)
          {:error/key :room/same-user
           :error/message "User is the owner"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? jammer-id)
          {:error/key :room/no-jammer
           :error/message "The jammer doesn't exist"}

          (not ((:waiting room) jammer-id))
          {:error/key :room/user-has-not-knocked
           :error/message "The jammer has not knocked on the room from before"}

          :else
          (do
            ;; handle an edge case where a jammer leaves the room,
            ;; but not the host. In this case the session is
            ;; still active from before
            (if (nil? (:session-id room))
              (db/with-transaction [database :default]
                ;; insert into the room_session
                (let [session-id (->> {:insert-into :room_session
                                       :values [{:room_id room-id}]}
                                      (db/query<! database)
                                      first
                                      :id)
                      owner-id (:owner room)]
                  ;; insert the users in the session
                  (db/query! database {:insert-into :room_session_users
                                       :values [{:room_session_id session-id
                                                 :user_id jammer-id}
                                                {:room_session_id session-id
                                                 :user_id owner-id}]})
                  ;; update our in-memory database
                  (swap! data update room-id merge {:jammer jammer-id
                                                    :waiting #{}
                                                    :session-id session-id
                                                    :status :full})))
              (db/with-transaction [database :default]
                ;; insert into the room_session
                (let [{:keys [session-id] owner-id :owner} room]
                  ;; insert the users in the session
                  (db/query! database {:insert-into :room_session_users
                                       :values [{:room_session_id session-id
                                                 :user_id jammer-id}]})
                  ;; update our in-memory database
                  (swap! data update room-id merge {:jammer jammer-id
                                                    :waiting #{}
                                                    :status :full}))))
            ;; send out an update to our jammer that the they have
            ;; been accepted into the jam
            (mqtt/publish mqtt-client (id->uuid jammer-id) {:message/type :room.jam/accepted
                                                            ;; for consistency
                                                            :room/id room-id
                                                            :room/jam (db-get-room-by-id this room-id)})

            (let [owner-id (:owner room)
                  [from-tp-id to-tp-id] (mapv identity (get-teleporter-ids database owner-id jammer-id))]
              (if (and from-tp-id
                       to-tp-id)
                (do
                  (let [db (:db jam-manager)]
                    (proto/write-db db [:teleporter from-tp-id :teleporter/sip]
                                    (str "sip:" from-tp-id "@voip1.songpark.com"))
                    (proto/write-db db [:teleporter to-tp-id :teleporter/sip]
                                    (str "sip:" to-tp-id "@voip1.songpark.com")))
                  (jam.platform/phone jam-manager from-tp-id to-tp-id)
                  (if-let [jam-id (get-jam-id jam-manager from-tp-id to-tp-id)]
                    (do
                      (swap! data update room-id merge {:jam-id jam-id})
                      true)
                    {:error/key :room/no-jam-id
                     :error/message "Unable to find a jam id for the room"}))
                {:error/key :room/no-paired-teleporter
                 :error/message "One or more teleporters are not paired with a user in the room"}))))))

(defn- db-leave* [{:keys [data database mqtt-client jam-manager] :as this} room-id jammer-id]
  (let [room (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (nil? jammer-id)
          {:error/key :room/no-jammer
           :error/message "The user does not exist"}

          (and (not= (:jammer room) jammer-id)
               (not ((:waiting room) jammer-id)))
          {:error/key :room/user-not-in-the-room
           :error/message "User is not in the room"}

          (= (:owner room) jammer-id)
          {:error/key :room/owner-is-trying-to-leave
           :error/message "Owner is trying to leave the room instead of closing the room"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          :else
          (let [{:keys [session-id jam-id]} room]
            (swap! data update room-id merge {:jammer nil
                                              :waiting #{}
                                              :session-id nil
                                              :jam-id nil
                                              :status :waiting})
            (when session-id
              (db/query! database {:update :room_session_users
                                   :set {:left_at (t/now)}
                                   :where [:and
                                           [:= :room_session_id session-id]
                                           [:= :user_id jammer-id]]}))
            (mqtt/publish mqtt-client (id->uuid (:owner room)) {:message/type :room.jam/left
                                                                :room/id room-id
                                                                :auth.user/id jammer-id})
            (when jam-id
              (jam.platform/stop jam-manager jam-id)
              (swap! data update room-id dissoc :jam-id))
            true))))

(defn- db-knock* [{:keys [data database mqtt-client] :as this} room-id jammer-id]
  (let [room (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (= (:owner room) jammer-id)
          {:error/key :room/owner-is-trying-to-leave
           :error/message "Owner is trying to leave the room instead of closing the room"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? jammer-id)
          {:error/key :room/no-jammer
           :error/message "The user does not exist"}

          :else
          (let [waiting (:waiting room)]
            (let [profile (model.profile/get-profile database jammer-id)]
              (mqtt/publish mqtt-client (id->uuid (:owner room)) {:message/type :room.jam/knocked
                                                                  :room/id room-id
                                                                  :room/jammer (get-jammer database jammer-id :knocking)})
              (swap! data update room-id merge {:waiting (conj waiting jammer-id)}))
            true))))

(defn- db-decline* [{:keys [data database mqtt-client] :as this} room-id jammer-id]
  (let [{:keys [waiting] :as room} (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (not (waiting jammer-id))
          {:error/key :room/user-not-waiting
           :error/message "User has not knocked on the room"}

          (= (:owner room) jammer-id)
          {:error/key :room/owner-is-trying-to-decline
           :error/message "Owner is trying to leave the room instead of declining entry to the room"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? jammer-id)
          {:error/key :room/no-jammer
           :error/message "The user does not exist"}

          :else
          (let [waiting (:waiting room)]
            ;; inform the jammer that they have been declined
            (do (mqtt/publish mqtt-client (id->uuid jammer-id) {:message/type :room.jam/declined
                                                                :room/id room-id})
                (swap! data update room-id merge {:waiting (disj waiting jammer-id)}))
            true))))

(defn- db-remove* [{:keys [data database mqtt-client jam-manager] :as this} room-id jammer-id]
  (let [room (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (not= (:jammer room) jammer-id)
          {:error/key :room/user-not-in-the-room
           :error/message "User is not in the room"}

          (= (:owner room) jammer-id)
          {:error/key :room/owner-is-trying-to-remove
           :error/message "Owner is trying to leave the room instead of closing the room"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? jammer-id)
          {:error/key :room/no-jammer
           :error/message "The user does not exist"}

          :else
          (let [{:keys [session-id jam-id]} room]
            (swap! data update room-id merge {:jammer nil
                                              :status :waiting})
            (when session-id
              (db/query! database {:update :room_session_users
                                   :set {:left_at (t/now)}
                                   :where [:and
                                           [:= :room_session_id session-id]
                                           [:= :user_id jammer-id]]}))
            (mqtt/publish mqtt-client (id->uuid jammer-id) {:message/type :room.jam/removed
                                                            :room/id room-id})
            (when jam-id
              (jam.platform/stop jam-manager jam-id)
              (swap! data update room-id dissoc :jam-id))
            true))))

(defn- db-close* [{:keys [data database mqtt-client jam-manager]} room-id owner-id]
  (let [room (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (not= (:owner room) owner-id)
          {:error/key :room/not-the-owner
           :error/message "User is not the owner"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? owner-id)
          {:error/key :room/no-jammer
           :error/message "Owner does not exist"}

          :else
          (let [{:keys [session-id jam-id]} room]
            (when session-id
              (db/with-transaction [database :default]
                (db/query! database {:update :room_session
                                     :set {:closed_at (t/now)}
                                     :where [:= :id session-id]})
                (db/query! database {:update :room_session_users
                                     :set {:left_at (t/now)}
                                     :where [:and
                                             [:= :room_session_id session-id]
                                             [:is :left_at nil]]})))
            (swap! data dissoc room-id)
            (when-let [jammer (:jammer room)]
              (mqtt/publish mqtt-client (id->uuid jammer) {:message/type :room.jam/closed
                                                           :room/id room-id}))
            (when jam-id
              (jam.platform/stop jam-manager jam-id))
            true))))

(defn- db-get-room-by-id* [{:keys [data database] :as _this} room-id]
  (let [room (get @data room-id nil)]
    (if room
      (let [{:keys [waiting] owner-id :owner jammer-id :jammer} room
            room (model.room/get-room database room-id)
            owner (get-jammer database owner-id :jamming)
            jammer (if jammer-id
                     (get-jammer database jammer-id :jamming))
            knockers (map #(get-jammer database % :waiting) waiting)]
        (assoc room :room/jammers (->> (into knockers [owner jammer])
                                       (remove nil?)
                                       (vec)
                                       (map (juxt :auth.user/id identity))
                                       (into {}))))
      nil)))

(defn- db-get-room-by-user-id* [{:keys [data database] :as this} user-id]
  (let [room-id (reduce (fn [_ [room-id {:keys [owner jammer waiting] :as _room}]]
                          (let [result (cond (= owner user-id)
                                             true
                                             (= jammer user-id)
                                             true
                                             (waiting user-id)
                                             true
                                             :else
                                             nil)]
                            (if (true? result)
                              (reduced room-id)
                              nil)))
                        nil @data)]
    (db-get-room-by-id this room-id)))

(defn- db-get-room-by-host* [{:keys [data] :as _this} owner-id]
  (reduce (fn [_ [room-id {:keys [owner] :as room}]]
            (if (= owner owner-id)
              (reduced (assoc room :id room-id))
              nil))
          nil @data))

;; we want to use SQL for storing information about when a session started
(defrecord RoomDB [started? data database mqtt-client jam-manager]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting RoomDB")
          (assoc this
                 :data (atom {})
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping RoomDB")
          (assoc this
                 :data nil
                 :started? false))))
  IRoom
  (db-host [this room-id owner-id]
    (db-host* this room-id owner-id))
  (db-accept [this room-id jammer-id]
    (db-accept* this room-id jammer-id))
  (db-leave [this room-id jammer-id]
    (db-leave* this room-id jammer-id))
  (db-knock [this room-id jammer-id]
    (db-knock* this room-id jammer-id))
  (db-decline [this room-id jammer-id]
    (db-decline* this room-id jammer-id))
  (db-remove [this room-id jammer-id]
    (db-remove* this room-id jammer-id))
  (db-close [this room-id owner-id]
    (db-close* this room-id owner-id))
  (db-get-room-by-host [this owner-id]
    (db-get-room-by-host* this owner-id))
  (db-get-room-by-user-id [this user-id]
    (db-get-room-by-user-id* this user-id))
  (db-get-room-by-id [this room-id]
    (db-get-room-by-id* this room-id))
  (db-get-rooms [this]
    @data))


(defn room-db [settings]
  (map->RoomDB settings))


(comment

  (def rooms (map->RoomDB {:data (atom {})
                           :database (:database @platform.init/system)
                           :jam-manager (:jam-manager @platform.init/system)
                           :mqtt-client (:mqtt-client @platform.init/system)}))


  (def room-id 2)
  (def owner-id 1)
  (def jammer-id 3)
  (db-host rooms room-id owner-id)
  (db-knock rooms room-id jammer-id)
  (db-knock rooms room-id 2)
  (db-decline rooms room-id jammer-id)
  (db-accept rooms room-id jammer-id)
  (db-remove rooms room-id jammer-id)
  (db-leave rooms room-id jammer-id)
  (db-close rooms room-id owner-id)

  (db-get-room-by-id rooms room-id)
  (db-get-room-by-user-id rooms owner-id)
  (db-get-room-by-user-id rooms jammer-id)
  (db-get-room-by-user-id rooms 2)

  ;; room id 2
  (db-host rooms 2 1)
  (db-knock rooms 2 2)
  (db-accept rooms 2 2)
  (db-close rooms 2 1)

  ;; room id 3
  (db-host rooms 3 1)
  (db-join rooms 3 2)
  (db-join rooms 3 3)

  (db-leave rooms 2 2)
  (db-leave rooms 2 3)

  (db-close rooms 2 1)
  (db-close rooms 2 2)

  (db-get-room-by-host rooms 1)
  (db-get-rooms rooms)

  (let [database (:database @platform.init/system)]
    ;;(can-host? database 2 2)
    (model.profile/get-profile database 2))

  (let [roomdb (:roomdb @platform.init/system)]
    ;;(db-knock roomdb room-id jammer-id)
    ;;(db-leave roomdb room-id jammer-id)
    (db-close roomdb room-id owner-id)
    ;;(db-host roomdb room-id owner-id)
    ;;(db-accept roomdb room-id jammer-id)
    )
  (def jammed (atom nil))
  (let [jam (:jam-manager @platform.init/system)
        tp1 #uuid "39d04c2c-7214-5e2c-a9ae-32ff15405b7f"
        tp2 #uuid "77756ff0-bb05-5e6a-b7d9-28086f3a07fd"
        db (:db jam)]
    (proto/read-db db [:jam])
    ;; (proto/write-db db [:teleporter tp1 :teleporter/sip]
    ;;                 (str tp1 "@voip1.songpark.com"))
    ;; (proto/write-db db [:teleporter tp2 :teleporter/sip]
    ;;                 (str tp2 "@voip1.songpark.com"))
    ;; (jam.platform/phone jam tp1 tp2)
    ;;(jam.platform/stop jam )
    ;;(proto/write-db db [:jam] nil)
    )

  )
