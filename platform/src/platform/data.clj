(ns platform.data)

(defonce ^:private jam-id* (atom nil))

(defn set-jam-id! [jam-id]
  (reset! jam-id* jam-id))

(defn clear-jam-id! []
  (reset! jam-id* nil))

(defn get-jam-teleporters []
  (str @jam-id* "/teleporters"))

(defn get-jam []
  (str @jam-id* "/jam"))
