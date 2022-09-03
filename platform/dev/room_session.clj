(ns room-session
  (:require [platform.api.room-session :as rs]
            [platform.init :as init]
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
                              :room.session/participant 2}))
  (rs/accept (get-request 1 {:room/id 2
                             :room.session/participant 2}))
  (rs/close (get-request 1 {:room/id 2}))
  (rs/leave (get-request 2 {:room/id 2}))
  (rs/remove (get-request 2 {:room/id 2
                             :room.session/participant 2}))

  )
