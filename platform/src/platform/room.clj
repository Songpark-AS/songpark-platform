(ns platform.room)


;; db should look like this
;; {:room-id {:owner :user-id
;;            :participant :user-id
;;            :status #{:waiting :full}}}

(defprotocol IRoom
  "Interface Database Key Value"
  (db-host [database room-id owner-id] "Host a room")
  (db-join [database room-id user-id] "Join a room")
  (db-leave [database room-id user-id] "Leave a room")
  (db-close [database room-id owner-id] "Close a room")
  (db-get-room-by-host [database owner-id] "Get a specific room based on the owner's id")
  (db-get-rooms [database] "Get all the rooms"))


(defrecord RoomDB [data]
  IRoom
  (db-host [this room-id owner-id]
    (let [room (get @data room-id nil)]
      (cond (= (:owner room) owner-id)
            {:result :error
             :reason/id :room/already-hosted
             :reason/message "Onwer is already hosting this room"}

            (nil? owner-id)
            {:result :error
             :resason/id :room/no-owner
             :reason/message "Owner does not exist"}

            :else
            (do (swap! data assoc room-id {:owner owner-id
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
            (do (swap! data update room-id merge {:participant user-id
                                                  :status :full})
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
            (do (swap! data dissoc room-id)
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

  (def rooms (RoomDB. (atom {})))

  (db-host rooms 1 1)
  (db-join rooms 1 2)
  (db-join rooms 1 3)
  (db-leave rooms 1 2)
  (db-leave rooms 1 3)
  (db-close rooms 1 2)
  (db-close rooms 1 1)
  (db-get-room-by-host rooms 1)
  (db-get-rooms rooms)
  )
