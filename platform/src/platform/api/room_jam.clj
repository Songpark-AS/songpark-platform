(ns platform.api.room-jam
  (:refer-clojure :exclude [remove])
  (:require [platform.model.room :as model.room]
            [platform.room :as room]
            [songpark.taxonomy.room]
            [taoensso.timbre :as log]))

(def http-ok {:status 200
              :body {:result :success}})

(defn current [{{roomdb :roomdb} :data
                {user-id :auth.user/id} :identity
                :as request}]
  (if user-id
    (if-let [result (room/db-get-room-by-user-id roomdb user-id)]
      {:status 200
       :body result}
      {:status 200
       :body http-ok})
    {:status 200
     :body http-ok}))

(defn host [{{roomdb :roomdb} :data
             {data :body} :parameters
             {user-id :auth.user/id} :identity
             :as request}]
  (let [{room-id :room/id} data
        result (room/db-host roomdb room-id user-id)]
    (if (true? result)
      {:status 200
       :body (room/db-get-room-by-id roomdb room-id)}
      {:status 400
       :body result})))

(defn knock [{{roomdb :roomdb
               db :database} :data
              {data :body} :parameters
              {user-id :auth.user/id} :identity
              :as request}]
  (let [{room-name :room/name} data
        {room-id :room/id room-name :room/name} (model.room/get-room-by-name db room-name)
        result (room/db-knock roomdb room-id user-id)]
    (if (true? result)
      {:status 200
       :body {:room/id room-id
              :room/name room-name}}
      {:status 400
       :body result})))

(defn accept [{{roomdb :roomdb} :data
               {data :body} :parameters
               :as request}]
  (let [{user-id :auth.user/id
         room-id :room/id} data
        result (room/db-accept roomdb room-id user-id)]
    (if (true? result)
      {:status 200
       :body {:room/id room-id
              :auth.user/id user-id}}
      {:status 400
       :body result})))

(defn decline [{{roomdb :roomdb} :data
                {data :body} :parameters
                :as request}]
  (let [{user-id :auth.user/id
         room-id :room/id} data
        result (room/db-decline roomdb room-id user-id)]
    (if (true? result)
      {:status 200
       :body {:room/id room-id
              :auth.user/id user-id}}
      {:status 400
       :body result})))

(defn leave [{{roomdb :roomdb} :data
              {user-id :auth.user/id} :identity
              {data :body} :parameters
              :as request}]
  (let [{room-id :room/id} data
        result (room/db-leave roomdb room-id user-id)]
    (if (true? result)
      http-ok
      {:status 400
       :body result})))

(defn remove [{{roomdb :roomdb} :data
               {user-id :auth.user/id} :identity
               {data :body} :parameters
               :as request}]
  (let [{user-id :auth.user/id
         room-id :room/id} data
        result (room/db-remove roomdb room-id user-id)]
    (if (true? result)
      {:status 200
       :body {:room/id room-id
              :auth.user/id user-id}}
      {:status 400
       :body result})))

(defn close [{{roomdb :roomdb} :data
              {user-id :auth.user/id} :identity
              {data :body} :parameters
              :as request}]
  (let [{room-id :room/id} data
        {:keys [participant]} (room/db-get-room-by-id roomdb room-id)
        result (room/db-close roomdb room-id user-id)]
    (if (true? result)
      http-ok
      {:status 400
       :body result})))
