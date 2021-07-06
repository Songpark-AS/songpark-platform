(ns kompetansia.dev
  (:require [ajax.core :refer [GET POST]]
            [ajax.interceptors :as ajax-interceptors]
            [ajax.transit :as ajax-transit]
            [kompetansia.common.communication :as communication]
            [kompetansia.common.i18n :refer [t t-with-locale]]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [cognitect.transit :as transit]
            [tick.alpha.api :as t]))

(reset! communication/base-url "http://127.0.0.1:3000")
(reset! communication/credentials {})


(rf/reg-event-db :data/display (fn [db [_ data]]
                                 (assoc db :data/display data)))
(rf/reg-sub :data/display (fn [db _]
                            (:data/display db)))


(defn dev-setup []
  (println "Running in dev mode. println will print cljs structures to the console"))


(defn init-i18n []
  (rf/dispatch-sync [:locale/locale :no-NB])
  (rf/dispatch-sync [:i18n/initialize {:no-NB {:hi "Hei"
                                               :locale/name "Bokmål (Norsk)"
                                               :button/switch "Endre"}
                                       :no-NN {:hi "Hei"
                                               :locale/name "Nynorsk (Norsk)"
                                               :button/switch "Endre"}
                                       :en {:hi "Hi"
                                            :locale/name "English (International)"
                                            :button/switch "Switch"}
                                       :sv-SE {:hi "Hej"
                                               :locale/name "Svenska"
                                               :button/switch "Byt"}
                                       :la {:hi "Salve"
                                            :locale/name "Latine"
                                            :button/switch "Mutare"}
                                       :tongue/fallback :en}]))

(defn login []
  (let [data (r/atom nil)]
    (fn []
      [:form
       [:div [:input {:type "text" :on-change #(swap! data assoc :email (-> % .-target .-value))}]]
       [:div [:input {:type "text" :on-change #(swap! data assoc :password (-> % .-target .-value))}]]
       [:div (pr-str {:auth.user/email (:email @data)
                      :auth.user/password (:password @data)})]
       [:div {:on-click #(rf/dispatch [:http/post
                                       "/api/auth/login"
                                       {:auth.user/email (:email @data)
                                        :auth.user/password (:password @data)}])} "Login"]])))

(defn show-i18n []
  [:div
   [:h2 "Current locale"]
   [:p "» " (t :locale/name)]
   [:table>tbody
    (doall
     (for [locale  [:en :no-NB :no-NN :sv-SE :la]]
       ^{:key locale}
       [:tr
        [:td (t-with-locale locale :locale/name)]
        [:td [:button {:on-click #(rf/dispatch [:locale/locale locale])} (t :button/switch)]]]))]])



(defn display-data []
  (let [data (rf/subscribe [:data/display])]
    [:div {:style {:background-color "#d0d0d0"
                   :padding "10px"
                   :margin "10px 0"}}
     [:h2 "Current data from the server"]
     [:p (pr-str @data)]]))

(defn main []
  [:div {:style {:margin "0 auto" :width "800px" :margin-top "10rem"}}
   [:div "We are now running reagent"]
   [display-data]
   [:button
    {:on-click #(rf/dispatch [:http/post "/api/echo" {:timestamp (t/now)} :data/display])}
    "Click me for some data"]
   [:hr]
   [show-i18n]
   [login]])

(defn mount-root []
  (rf/clear-subscription-cache!)
  (init-i18n)
  (dom/render [main]
              (.getElementById js/document "app"))
  )

(defn ^:export init []
  (dev-setup)
  (mount-root))

