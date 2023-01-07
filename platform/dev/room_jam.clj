(ns room-jam
  (:require [platform.api.room-jam :as rj]
            [platform.init :as init]
            [songpark.jam.platform :as jam.platform]
            [songpark.jam.platform.protocol :as proto]
            [taoensso.timbre :as log]))



(defn- get-request [user-id data]
  {:data {:database (:database @platform.init/system)
          :roomdb (-> @platform.init/system
                      :http-server
                      :middleware-data
                      :songpark/data
                      :roomdb)}
   :identity {:auth.user/id user-id}
   :parameters {:body data}})

(comment

  (rj/host (get-request 3 {:room/id 3}))

  (rj/host (get-request 1 {:room/id 2}))
  (rj/knock (get-request 2 {:room/name "foobar"}))
  (rj/knock (get-request 2 {:room/name "My fantastic room 2"}))
  (rj/decline (get-request 2 {:room/id 2
                              :room.jammer/id 2}))
  (rj/accept (get-request 1 {:room/id 2
                             :room.jammer/id 2}))
  (rj/close (get-request 1 {:room/id 2}))
  (rj/leave (get-request 2 {:room/id 2}))
  (rj/remove (get-request 2 {:room/id 2
                             :room.jammer/id 2}))

  (let [jam-manager (-> @platform.init/system
                        :http-server
                        :middleware-data
                        :songpark/data
                        :jam-manager)
        ezdb (:database @platform.init/system)
        jam-id (platform.room/get-jam-id jam-manager
                                         #uuid "39d04c2c-7214-5e2c-a9ae-32ff15405b7f")]
    ;;(proto/read-db (:db jam-manager) [:jam])
    (platform.room/get-jam-id jam-manager
                              #uuid "39d04c2c-7214-5e2c-a9ae-32ff15405b7f"
                              #uuid "77756ff0-bb05-5e6a-b7d9-28086f3a07fd")
    (platform.room/get-jam jam-manager jam-id))

  )
