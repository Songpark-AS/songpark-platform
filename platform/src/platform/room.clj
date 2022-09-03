(ns platform.room
  (:require [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [platform.model.profile :as model.profile]
            [platform.util :refer [id->uuid]]
            [songpark.jam.platform :as jam.platform]
            [songpark.mqtt :as mqtt]
            [taoensso.timbre :as log]
            [tick.core :as t]))


;; db should look like this
;; {:room-id {:owner <user-id>
;;            :participant <user-id>
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
  (db-knock [database room-id user-id] "Knock on a room")
  (db-accept [database room-id user-id] "Accepted into a room")
  (db-decline [database room-id user-id] "Decline someone knocking on a room")
  (db-leave [database room-id user-id] "Leave a room")
  (db-remove [database room-id user-id] "Remove a participant from a room")
  (db-close [database room-id owner-id] "Close a room")
  (db-get-room-by-host [database owner-id] "Get a specific room based on the owner's id")
  (db-get-room-by-id [database room-id])
  (db-get-rooms [database] "Get all the rooms"))

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

(defn- db-accept* [{:keys [data database mqtt-client jam-manager] :as this} room-id user-id]
  (let [room (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (= (:participant room) user-id)
          {:error/key :room/already-joined
           :error/message "User have already joined this room"}

          (= (:status room) :full)
          {:error/key :room/full
           :error/message "The room is full"}

          (= (:owner room) user-id)
          {:error/key :room/same-user
           :error/message "User is the owner"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? user-id)
          {:error/key :room/no-participant
           :error/message "The participant doesn't exist"}

          (not ((:waiting room) user-id))
          {:error/key :room/user-has-not-knocked
           :error/message "The participant has not knocked on the room from before"}

          :else
          (do
            ;; handle an edge case where a participant leaves the room,
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
                                                 :user_id user-id}
                                                {:room_session_id session-id
                                                 :user_id owner-id}]})
                  ;; update our in-memory database
                  (swap! data update room-id merge {:participant user-id
                                                    :waiting #{}
                                                    :session-id session-id
                                                    :status :full})))
              (db/with-transaction [database :default]
                ;; insert into the room_session
                (let [{:keys [session-id] owner-id :owner} room]
                  ;; insert the users in the session
                  (db/query! database {:insert-into :room_session_users
                                       :values [{:room_session_id session-id
                                                 :user_id user-id}]})
                  ;; update our in-memory database
                  (swap! data update room-id merge {:participant user-id
                                                    :waiting #{}
                                                    :status :full}))))
            ;; send out an update to our participant that the they have
            ;; been accepted into the jam
            (mqtt/publish mqtt-client (id->uuid user-id) {:message/type :room.session/accepted
                                                          :room/id room-id})

            ;; TODO: add jam phoning
            ;; (jam.platform/phone )

            true))))

(defn- db-leave* [{:keys [data database mqtt-client] :as this} room-id user-id]
  (let [room (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (= (:status room) :waiting)
          {:error/key :room/user-not-in-the-room
           :error/message "User is not in the room"}

          (not= (:participant room) user-id)
          {:error/key :room/user-not-in-the-room
           :error/message "User is not in the room"}

          (= (:owner room) user-id)
          {:error/key :room/owner-is-trying-to-leave
           :error/message "Owner is trying to leave the room instead of closing the room"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? user-id)
          {:error/key :room/no-participant
           :error/message "The user does not exist"}

          :else
          (do (swap! data update room-id merge {:participant nil
                                                :waiting #{}
                                                :status :waiting})
              (when-let [session-id (:session-id room)]
                (db/query! database {:update :room_session_users
                                     :set {:left_at (t/now)}
                                     :where [:and
                                             [:= :room_session_id session-id]
                                             [:= :user_id user-id]]}))
              (mqtt/publish mqtt-client (id->uuid (:owner room)) {:message/type :room.session/left
                                                                  :room.session/participant user-id})
              ;; TODO: add jam leaving
              true))))

(defn- db-knock* [{:keys [data database mqtt-client] :as this} room-id user-id]
  (let [room (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (= (:owner room) user-id)
          {:error/key :room/owner-is-trying-to-leave
           :error/message "Owner is trying to leave the room instead of closing the room"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? user-id)
          {:error/key :room/no-participant
           :error/message "The user does not exist"}

          :else
          (let [waiting (:waiting room)]
            (let [profile (model.profile/get-profile database user-id)]
              (mqtt/publish mqtt-client (id->uuid (:owner room)) {:message/type :room.session/knocked
                                                                  :room.session/participant user-id
                                                                  :room.session.participant/profile profile})
              (swap! data update room-id merge {:waiting (conj waiting user-id)}))
            true))))

(defn- db-decline* [{:keys [data database mqtt-client] :as this} room-id user-id]
  (let [{:keys [waiting] :as room} (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (not (waiting user-id))
          {:error/key :room/user-not-waiting
           :error/message "User has not knocked on the room"}

          (= (:owner room) user-id)
          {:error/key :room/owner-is-trying-to-decline
           :error/message "Owner is trying to leave the room instead of declining entry to the room"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? user-id)
          {:error/key :room/no-participant
           :error/message "The user does not exist"}

          :else
          (let [waiting (:waiting room)]
            ;; inform the participant that they have been declined
            (do (mqtt/publish mqtt-client (id->uuid user-id) {:message/type :room.session/declined
                                                              :room/id room-id})
                (swap! data update room-id merge {:waiting (disj waiting user-id)}))
            true))))

(defn- db-remove* [{:keys [data database mqtt-client] :as this} room-id user-id]
  (let [room (get @data room-id nil)]
    (cond (nil? room)
          {:error/key :room/does-not-exist
           :error/message "The room does not exist"}

          (not= (:participant room) user-id)
          {:error/key :room/user-not-in-the-room
           :error/message "User is not in the room"}

          (= (:owner room) user-id)
          {:error/key :room/owner-is-trying-to-remove
           :error/message "Owner is trying to leave the room instead of closing the room"}

          (nil? (:owner room))
          {:error/key :room/no-owner
           :error/message "No owner is present for this room"}

          (nil? user-id)
          {:error/key :room/no-participant
           :error/message "The user does not exist"}

          :else
          (do (swap! data update room-id merge {:participant nil
                                                :status :waiting})
              (when-let [session-id (:session-id room)]
                (db/query! database {:update :room_session_users
                                     :set {:left_at (t/now)}
                                     :where [:and
                                             [:= :room_session_id session-id]
                                             [:= :user_id user-id]]}))
              (mqtt/publish mqtt-client (id->uuid user-id) {:message/type :room.session/removed
                                                            :room/id room-id})
              ;; TODO: add jam ending
              true))))

(defn- db-close* [{:keys [data database mqtt-client]} room-id owner-id]
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
          {:error/key :room/no-participant
           :error/message "Owner does not exist"}

          :else
          (do (when-let [session-id (:session-id room)]
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
              (when-let [participant (:participant room)]
                (mqtt/publish mqtt-client (id->uuid participant) {:message/type :room.session/closed
                                                                  :room/id room-id}))
              ;; TODO: add jam stopping
              true))))

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
  (db-accept [this room-id user-id]
    (db-accept* this room-id user-id))
  (db-leave [this room-id user-id]
    (db-leave* this room-id user-id))
  (db-knock [this room-id user-id]
    (db-knock* this room-id user-id))
  (db-decline [this room-id user-id]
    (db-decline* this room-id user-id))
  (db-remove [this room-id user-id]
    (db-remove* this room-id user-id))
  (db-close [this room-id owner-id]
    (db-close* this room-id owner-id))
  (db-get-room-by-host [this owner-id]
    (reduce (fn [_ [room-id {:keys [owner] :as room}]]
              (if (= owner owner-id)
                (reduced (assoc room :id room-id))
                nil))
            nil @data))
  (db-get-room-by-id [this room-id]
    (get @data room-id nil))
  (db-get-rooms [this]
    @data))


(defn room-db [settings]
  (map->RoomDB settings))


(comment

  (def rooms (map->RoomDB {:data (atom {})
                           :database (:database @platform.init/system)}))


  (def room-id 2)
  (def owner-id 1)
  (def participant-id 3)
  (db-host rooms room-id owner-id)
  (db-knock rooms room-id participant-id)
  (db-decline rooms room-id participant-id)
  (db-accept rooms room-id participant-id)
  (db-remove rooms room-id participant-id)
  (db-leave rooms room-id participant-id)
  (db-close rooms room-id owner-id)

  ;; room id 2
  (db-host rooms 2 1)
  (db-join rooms 2 2)
  (db-join rooms 2 3)

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
  )
