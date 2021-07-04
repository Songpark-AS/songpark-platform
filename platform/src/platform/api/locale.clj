(ns platform.api.locale
  (:require [platform.model.locale :as model.locale]
            [ez-database.core :as db]
            [taoensso.timbre :as log]
            [clojure.spec.alpha :as spec]))


(defn locales [{{db :database} :data}]
  {:status 200
   :body   (model.locale/locales db)})

(defn by-id
  [{:keys [data parameters]}]
  (let [db (:database data)
        locale-id (get-in parameters [:path :locale-id])
        result (model.locale/by-id db locale-id)]
    (if result
      {:status 200
       :body   result}
      {:status 404
       :body ""})))

(defn create-locale
  [{:keys [data parameters]}]
  (let [db (:database data)
        locale (get-in parameters [:body])
        result (when (nil? (model.locale/by-id db (:locale/id locale)))
                 (-> (model.locale/add-locale db locale)
                     first))]
    (if result
      {:status 201
       :body   result}
      {:status 400
       :body {:error/message "Locale already exists"}})))

(defn update-locale
  [{:keys [data parameters]}]
  (let [db (:database data)
        locale (get-in parameters [:body])
        result (-> (model.locale/update-locale db locale)
                   first)]
    (if (= 1 result)
      {:status 204
       :body ""}
      {:status 404
       :body ""})))
