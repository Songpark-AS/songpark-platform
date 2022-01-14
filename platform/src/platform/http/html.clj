(ns platform.http.html
  (:require [cheshire.core :as json]
            [clojure.core.memoize :as memo]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [digest :refer [md5]]
            [hiccup.page :refer [html5]]))

(defn- media* [path]
  (try
    (str path "?" (md5 (slurp (io/resource (str/replace path #"/static/" "public/")))))
    (catch Exception e
      nil)))


(def media
  ;; clear cache after 24 hours
  (memo/ttl media* :ttl/threshold (* 24 60 60 1000)))


(defn home [options]
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet" :href (media "/static/css/vendor/antd.css")}]
    [:link {:rel "stylesheet" :href (media "/static/css/app.css")}]
    [:link {:rel "icon" :type "image/png" :href "/static/img/logo.png"}]
    [:title "Songpark - Backoffice"]]
   [:body
    [:div {:id "app"}]
    [:script {:src (media "/static/js/main.js") :type "text/javascript"}]
    [:script {:type "text/javascript"}
     (str "backoffice.core.init("
          (json/generate-string options)
          ");")]]))

(defn auth-page []
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:title ""]]
   [:body
    [:main
     [:div.loading-message "Logging into Songpark"]]
    [:script "webkit.messageHandlers.cordova_iab.postMessage(JSON.stringify({'logged-in?': true}));"]]))
