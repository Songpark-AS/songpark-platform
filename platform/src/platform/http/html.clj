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
    [:title "Songpark - Backoffice"]
    [:style {:type "text/css"}
     "body {
display: flex;
flex-direction: column;
align-items: center;
gap: 20px;
padding-top: 150px;
}"]]
   [:body
    [:div "Reset all rooms"]
    [:div
     [:button {:onclick "resetRooms();"}
      "Reset!"]]
    [:script {:type "text/javascript"}
     "var resetRooms = function () {
fetch('/admin/reset-rooms',
 {method: 'POST'})
.then((resp) => resp.json()
.then((data) => {
  if (data.result == 'success') {
    alert('The rooms have been reset. Give it a bit of time to let the Teleproters close any ongoing jams.');
  } else {
    alert('Something went wrong. Contact the administrator');
  }
}))
};"]
    ]))

(defn auth-page []
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:title ""]]
   [:body
    [:main
     [:div.loading-message "Logging into Songpark"]]
    [:script "webkit.messageHandlers.cordova_iab.postMessage(JSON.stringify({'logged-in?': true}));"]]))
