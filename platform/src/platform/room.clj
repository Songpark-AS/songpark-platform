(ns platform.room
  (:require [ez-database.core :as db]
            [tick.core :as t]))


;; db should look like this
;; {:room-id {:owner <user-id>
;;            :participant <user-id>
;;            :session-id <session-id>
;;            :status #{:waiting :full}}}

;; room-id corresponds to the id of the room_room row in the SQL database
;; this implementation is mainly meant to be used in conjunction with a REST API
;; for an in-memory representation of what's going on
;; it's hidden behind a protocol, so it's possible to implement it with a
;; database that can save the data permantently, if the need were to arise

(defprotocol IRoom
  "Interface Database Key Value"
  (db-host [database room-id owner-id] "Host a room")
  (db-join [database room-id user-id] "Join a room")
  (db-leave [database room-id user-id] "Leave a room")
  (db-close [database room-id owner-id] "Close a room")
  (db-get-room-by-host [database owner-id] "Get a specific room based on the owner's id")
  (db-get-rooms [database] "Get all the rooms"))


;; we want to use SQL for storing information about when a session started
(defrecord RoomDB [data ezdb]
  IRoom
  (db-host [this room-id owner-id]
    (let [room (get @data room-id nil)]
      (cond (= (:owner room) owner-id)
            {:result :error
             :reason/id :room/already-hosted
             :reason/message "Owner is already hosting this room"}

            (nil? owner-id)
            {:result :error
             :resason/id :room/no-owner
             :reason/message "Owner does not exist"}

            :else
            (do
              ;; close all other rooms hosted by the same host
              (doseq [[room-id-alt {owner-id-alt :owner}] @data]
                (when (= owner-id-alt owner-id)
                  (db-close this room-id-alt owner-id)))
              (swap! data assoc room-id {:owner owner-id
                                         :status :waiting})
              {:result :success}))))
  (db-join [this room-id user-id]
    (let [room (get @data room-id nil)]
      (cond (nil? room)
            {:result :error
             :reason/id :room/does-not-exist
             :reason/message "The room does not exist"}

            (= (:participant room) user-id)
            {:result :error
             :reason/id :room/already-joined
             :reason/message "User have already joined this room"}

            (= (:status room) :full)
            {:result :error
             :reason/id :room/full
             :reason/message "The room is full"}

            (= (:owner room) user-id)
            {:result :error
             :reason/id :room/same-user
             :reason/message "User is the owner"}

            (nil? (:owner room))
            {:result :error
             :resason/id :room/no-owner
             :reason/message "No owner is present for this room"}

            (nil? user-id)
            {:result :error
             :resason/id :room/no-participant
             :reason/message "The participant doesn't exist"}

            :else
            (do
              ;; handle an edge case where a participant leaves the room,
              ;; but no the host. In this case the session is still on-going
              (if (nil? (:session-id room))
                (db/with-transaction [ezdb :default]
                  ;; insert into the room_session
                  (let [session-id (->> {:insert-into :room_session
                                         :values [{:room_id room-id}]}
                                        (db/query<! ezdb)
                                        first
                                        :id)
                        owner-id (:owner room)]
                    ;; insert the users in the session
                    (db/query! ezdb {:insert-into :room_session_users
                                     :values [{:room_session_id session-id
                                               :user_id user-id}
                                              {:room_session_id session-id
                                               :user_id owner-id}]})
                    ;; update our in-memory database
                    (swap! data update room-id merge {:participant user-id
                                                      :session-id session-id
                                                      :status :full})))
                (db/with-transaction [ezdb :default]
                  ;; insert into the room_session
                  (let [{:keys [session-id] owner-id :owner} room]
                    ;; insert the users in the session
                    (db/query! ezdb {:insert-into :room_session_users
                                     :values [{:room_session_id session-id
                                               :user_id user-id}]})
                    ;; update our in-memory database
                    (swap! data update room-id merge {:participant user-id
                                                      :status :full}))))
              {:result :success}))))
  (db-leave [this room-id user-id]
    (let [room (get @data room-id nil)]
      (cond (nil? room)
            {:result :error
             :reason/id :room/does-not-exist
             :reason/message "The room does not exist"}

            (= (:status room) :waiting)
            {:result :error
             :reason/id :room/user-not-in-the-room
             :reason/message "User is not in the room"}

            (not= (:participant room) user-id)
            {:result :error
             :reason/id :room/user-not-in-the-room
             :reason/message "User is not in the room"}

            (= (:owner room) user-id)
            {:result :error
             :reason/id :room/owner-is-trying-to-leave
             :reason/message "Owner is trying to leave the room instead of closing the room"}

            (nil? (:owner room))
            {:result :error
             :resason/id :room/no-owner
             :reason/message "No owner is present for this room"}

            (nil? user-id)
            {:result :error
             :resason/id :room/no-participant
             :reason/message "The user does not exist"}

            :else
            (do (swap! data update room-id merge {:participant nil
                                                  :status :waiting})
                (when-let [session-id (:session-id room)]
                 (db/query! ezdb {:update :room_session_users
                                  :set {:left_at (t/now)}
                                  :where [:and
                                          [:= :room_session_id session-id]
                                          [:= :user_id user-id]]}))
                {:result :success}))))
  (db-close [this room-id owner-id]
    (let [room (get @data room-id nil)]
      (cond (nil? room)
            {:result :error
             :reason/id :room/does-not-exist
             :reason/message "The room does not exist"}

            (not= (:owner room) owner-id)
            {:result :error
             :reason/id :room/not-the-owner
             :reason/message "User is not the owner"}

            (nil? (:owner room))
            {:result :error
             :resason/id :room/no-owner
             :reason/message "No owner is present for this room"}

            (nil? owner-id)
            {:result :error
             :resason/id :room/no-participant
             :reason/message "Owner does not exist"}

            :else
            (do (when-let [session-id (:session-id room)]
                  (db/with-transaction [ezdb :default]
                    (db/query! ezdb {:update :room_session
                                     :set {:closed_at (t/now)}
                                     :where [:= :id session-id]})
                    (db/query! ezdb {:update :room_session_users
                                     :set {:left_at (t/now)}
                                     :where [:and
                                             [:= :room_session_id session-id]
                                             [:is :left_at nil]]})))
                (swap! data dissoc room-id)
                {:result :success}))))
  (db-get-room-by-host [this owner-id]
    (reduce (fn [_ [room-id {:keys [owner] :as room}]]
              (if (= owner owner-id)
                (reduced (assoc room :id room-id))
                nil))
            nil @data))
  (db-get-rooms [this]
    @data))



(comment

  (def rooms (map->RoomDB {:data (atom {})
                           :ezdb (:database @platform.init/system)}))


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
  )
