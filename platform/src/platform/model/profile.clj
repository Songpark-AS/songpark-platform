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
               [:position :profile/position]
               [:image_url ->image-url <-image-url :profile/image-url])

(defn name-exists?
  "Check if the name exists for a given profile name"
  ([db name]
   (->> {:select [:id]
         :from [:profile_profile]
         :where [:= :name name]}
        (db/query db)
        first
        some?))
  ([db exclude-user-id name]
   (->> {:select [:id]
         :from [:profile_profile]
         :where [:and
                 [:= :name name]
                 [:<> :id exclude-user-id]]}
        (db/query db)
        first
        some?)))

(defn get-profile [db user-id]
  (->> {:select [:p.*]
        :from [[:profile_profile :p]]
        :where [:= :p.user_id user-id]}
       (db/query db
                 ^:opts {[:transformation :post]
                         [:profile :profile/profile]}
                 )
       first))

(defn save-profile [db user-id data]
  (try
    (let [{old-image :image_url
           id        :id       } (->> {:select [:p.image_url :p.id]
                                       :from [[:profile_profile :p]]
                                       :join [[:auth_user :u] [:= :u.id :p.user_id]]
                                       :where [:= :u.id user-id]}
                                      (db/query db)
                                      first)
          img-base64 (:profile.image/base64 data)
          img-path (if img-base64
                     (str (java.util.UUID/randomUUID)
                          "."
                          (:profile.image/type data))
                     old-image)
          to-save (-> (transform/transform {:nil false}
                                           :profile/profile :profile data)
                      (assoc :image_url img-path))]
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

(comment

  (defn slurp-bytes
    "Slurp the bytes from a slurpable thing"
    [x]
    (with-open [out (java.io.ByteArrayOutputStream.)]
      (clojure.java.io/copy (clojure.java.io/input-stream x) out)
      (.toByteArray out)))

  (let [db (:database @platform.init/system)]
    (save-profile db 1 {:profile/name "Emilius"
                        :profile/position "Guitarr"
                        :profile.image/type "png"
                        :profile.image/base64 (-> (slurp-bytes "assets/logo-black.png")
                                                  (base64/encode true))}))

  (let [db (:database @platform.init/system)]
    (get-profile db 1))

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
