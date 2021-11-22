(ns platform.api.jam
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [songpark.common.taxonomy.jam]
            [platform.db.store :as db]
            [platform.api :refer [send-message!]]))


(defn- get-members [jam-id]
  (let [uuids (db/rd [:jam jam-id])]
    (->> uuids
         (mapv #(str %)))))

(defn- get-sips [jam-id]
  (let [teleporter-uuids (into #{} (db/rd [:jam jam-id]))]
    (->> (db/rd [:teleporter])
         (vals)
         (filter #(teleporter-uuids (:teleporter/uuid %)))
         (map (juxt #(-> % :teleporter/uuid str) :teleporter/sip))
         (into {}))))

(defn- get-start-order [jam-id]
  (let [members (into #{} (get-members jam-id))
        zedboard1 "f7a21b06-014d-5444-88d7-0374a661d2de"]
    ;; for debugging and development purposes during the final sprint to get
    ;; a working prototype. we always want "zedboard-01" to be first, since
    ;; we are developing on that one via the REPL
    (if (members zedboard1)
      (into [zedboard1] (disj members zedboard1))
      (into [] members))))

;; on connection, the frontend sends a request to start
;; a jam session with a given collection of teleporter uuids.
;; this will then initiate the registration of SIP accounts for
;; the given teleporters, which will be sent to the teleporters
;; via MQTT, such that they can call each other. Only when confirmation
;; from all teleporters of SIP connectivity has been received, we send back
;; a response to the frontend with jam/uuid, which will be used as a jam topic
;; on mqtt... LATERZ...

(defn start [{:keys [data parameters]}]
  (let [uuids (-> parameters :body)
        jam-id (uuid/v4)]
    (db/wr [:jam jam-id] (->> uuids (map :teleporter/uuid)))
    (if-not (empty? uuids)
      (do
        (send-message! {:message/type :jam.cmd/start
                        :message/body {:jam/status true
                                       :jam/topic (str jam-id)
                                       :jam/sip (get-sips jam-id)
                                       :jam/members (get-start-order jam-id)}
                        :message/meta {:mqtt/topic (str jam-id)
                                       :origin :platform}})
        {:status 201
         :body {:jam/uuid jam-id
                :jam/status true}})
      {:status 400
       :body {:error/message "Invalid: No members in jam"}})))

;; Does not check whether or not the jam actually
;; exists, so it will return 200 no matter what. It
;; will however delete the jam if it exists.
(defn stop [{:keys [data parameters]}]
  (let [jam-id (-> parameters :body :jam/uuid)]
    (send-message! {:message/type :jam.cmd/stop
                    :message/body {:jam/topic (str jam-id)
                                   :jam/sip (get-sips jam-id)
                                   :jam/members (get-start-order jam-id)}
                    :message/meta {:mqtt/topic (str jam-id)
                                   :origin :platform}})

    ;; delete the jam
    (let [res (->> (db/wr [:jam] jam-id dissoc) :jam keys (filter #{jam-id}))]
      (if (empty? res)
        {:status 204
         :body ""}
        {:status 400
         :body {:error/message "Could not stop jam"}}))))



(comment
  (let [jam-id (first (keys (db/rd [:jam])))]
    (keys (jam-topics jam-id)))
  
  (db/rd [:teleporter])
  (db/rd [:jam])

  (send-message! {:message/type :jam.cmd/stop
                  :message/body {:jam/members ["f7a21b06-014d-5444-88d7-0374a661d2de" "844ac6ff-e9a2-57ac-b91a-41cf4e6d74c8"]}
                  :message/meta {:mqtt/topic "77d6aeaa-3f90-4844-950b-f0c8b043a680"
                                 :origin :platform}})
  
  )


