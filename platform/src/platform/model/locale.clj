(ns platform.model.locale
  (:require [ez-database.model :as model :refer [defmodel]]
            #_ [songpark.taxonomy.locale]))

(defmodel LocaleModel {:model {:table :locale_locale
                               :constraints #{[:= :id :locale/id]}}
                       :transformation [:locale :locale/locale
                                        [:id :locale/id]
                                        [:name :locale/name]]})


(defn locales [db]
  (model/select LocaleModel db {:order-by [[:name :asc]]}))

(defn by-id
  [db locale-id]
  (first (model/select LocaleModel db {:locale/id locale-id})))

(defn add-locale
  [db locale]
  (model/insert LocaleModel db [locale]))

(defn update-locale
  [db locale #_{:keys [:locale/id :locale/name]}]
  (model/update LocaleModel db (select-keys locale [:locale/id]) (dissoc locale :locale/id)))
