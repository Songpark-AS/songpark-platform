(ns room-jam
  (:require [platform.api.room-session :as rs]
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

  (rs/host (get-request 3 {:room/id 3}))

  (rs/host (get-request 1 {:room/id 2}))
  (rs/knock (get-request 2 {:room/name "foobar"}))
  (rs/knock (get-request 2 {:room/name "My fantastic room 2"}))
  (rs/decline (get-request 2 {:room/id 2
                              :room.jammer/id 2}))
  (rs/accept (get-request 1 {:room/id 2
                             :room.jammer/id 2}))
  (rs/close (get-request 1 {:room/id 2}))
  (rs/leave (get-request 2 {:room/id 2}))
  (rs/remove (get-request 2 {:room/id 2
                             :room.jammer/id 2}))

  (let [jam-manager (-> @platform.init/system
                        :http-server
                        :middleware-data
                        :songpark/data
                        :jam-manager)
        ezdb (:database @platform.init/system)]
    ;;(proto/read-db (:db jam-manager) [:jam])
    (platform.room/get-jam-id jam-manager
                              #uuid "39d04c2c-7214-5e2c-a9ae-32ff15405b7f"
                              #uuid "77756ff0-bb05-5e6a-b7d9-28086f3a07fd"))

  )
