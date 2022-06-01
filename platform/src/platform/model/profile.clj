(ns platform.model.profile
  (:require [buddy.core.codecs :refer [bytes->str
                                       str->bytes]]
            [buddy.core.codecs.base64 :as base64]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ez-database.core :as db]
            [ez-database.transform :as transform]
            [me.raynes.fs :as fs]
            [platform.config :refer [config]]
            [taoensso.timbre :as log])
  (:import [java.nio ByteBuffer]))

(defn ->image-url [old new]
  (let [image-url (:image_url old)]
    (if-not (str/blank? image-url)
      (assoc new :profile/image-url
             (str (get-in config [:storage :url])
                  image-url))
      (dissoc new :profile/image-url))))

(defn <-image-url [old new]
  new)

(transform/add :profile :profile/profile
               [:id :profile/id]
               [:name :profile/name]
               [:bio :profile/bio]
               [:location :profile/location]
               [:pronoun_id :profile.pronoun/id]
               [:pronoun_name :profile.pronoun/name]
               [:image_url ->image-url <-image-url :profile/image-url])

(transform/add :pronoun :profile/pronoun
               [:id :profile.pronoun/id]
               [:name :profile.pronoun/name])

(defn name-exists? [db name]
  (->> {:select [:id]
        :from [:profile_profile]
        :where [:= :name name]}
       (db/query db)
       first
       some?))

(defn get-profile [db user-id]
  (->> {:select [:p.*, [:pr.name :pronoun_name]]
        :from [[:profile_profile :p]]
        :join [[:profile_pronoun :pr] [:= :p.pronoun_id :pr.id]]
        :where [:= :p.user_id user-id]}
       (db/query db
                 ^:opts {[:transformation :post]
                         [:profile :profile/profile]}
                 )
       first))

(defn save-profile [db user-id {:profile/keys [id] :as data}]
  (try
    (let [old-image (->> {:select [:image_url]
                          :from [:profile_profile]
                          :where [:= :id id]}
                         (db/query db)
                         first
                         :image_url)
          img-base64 (:profile.image/base64 data)
          img-path (if img-base64
                     (str (java.util.UUID/randomUUID)
                          "."
                          (:profile.image/type data))
                     old-image)
          to-save (-> (transform/transform {:nil false
                                            [:remove-ks :pre] #{:profile.pronoun/name}}
                                           :profile/profile :profile data)
                      (assoc :image_url img-path)
                      (dissoc :id))]

      ;; if base64 image is provided, we clean up any old image and
      ;; save the new image to the hd
      (when img-base64
        (when-not (str/blank? old-image)
          (fs/delete (str (get-in config [:storage :directory])
                          old-image)))
        (with-open [out (io/output-stream (str (get-in config [:storage :directory])
                                               img-path))]
          (.write out (base64/decode img-base64))))

      (db/query! db {:update :profile_profile
                     :set to-save
                     :where [:= :id id]})
      true)
    (catch Exception e
      (log/warn ::save-profile {:exception e
                                :message (ex-message e)
                                :data (ex-data e)})
      false)))

(defn get-pronouns [db]
  (->> {:select [:*]
        :from [:profile_pronoun]}
       (db/query db
                 ^:opts {[:transformation :post]
                         [:pronoun :profile/pronoun]})))


(comment

  (defn slurp-bytes
    "Slurp the bytes from a slurpable thing"
    [x]
    (with-open [out (java.io.ByteArrayOutputStream.)]
      (clojure.java.io/copy (clojure.java.io/input-stream x) out)
      (.toByteArray out)))

  (let [db (:database @platform.init/system)]
    (save-profile db 1 {:profile/id 1
                        :profile/name "Emilius"
                        :profile/bio "My bio"
                        :profile/location "Ski, Norway"
                        :profile.image/type "png"
                        :profile.image/base64 (-> (slurp-bytes "assets/logo-black.png")
                                                  (base64/encode true))
                        :profile.pronoun/id -1}))

  (let [db (:database @platform.init/system)]
    (get-profile db 1))

  (let [db (:database @platform.init/system)]
    (get-pronouns db))

  (let [data (slurp-bytes "assets/logo-black.png")]
    (with-open [out (io/output-stream "tmp.png")]
      (.write out data)))

  (let [data (-> (slurp-bytes "assets/logo-black.png")
                 (base64/encode true))]
    (with-open [out (io/output-stream "tmp.png")]
        (.write out (base64/decode data)))
    #_data)
  (with-open [in (io/input-stream (io/file "assets/logo-black.png"))]
    )


  )
