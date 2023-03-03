(ns platform.http.route
  (:require [platform.api.admin :as api.admin]
            [platform.api.app :as api.app]
            [platform.api.auth :as api.auth]
            [platform.api.fx :as api.fx]
            [platform.api.pairing :as api.pairing]
            [platform.api.profile :as api.profile]
            [platform.api.room :as api.room]
            [platform.api.room-jam :as api.room-jam]
            [platform.api.teleporter :as api.teleporter]
            [platform.api.version :as api.version]
            [platform.config :refer [config]]
            [platform.http.html :refer [home]]
            [platform.http.middleware :as middleware :refer [http-basic-authenticated?
                                                             wrap-authn
                                                             wrap-authz
                                                             wrap-exceptions]]
            [buddy.auth.middleware :refer [wrap-authentication
                                           wrap-authorization]]
            [clojure.spec.alpha :as spec]
            [cognitect.transit :as transit]
            [muuntaja.core :as m]
            [songpark.taxonomy.error]
            [songpark.taxonomy.http]
            [songpark.taxonomy.jam]
            [songpark.taxonomy.teleporter]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :as ring.response]
            [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.coercion :as coercion]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.spec]
            [tick.core :as t]
            [taoensso.timbre :as log]
            [platform.api.admin :as admin]))


(defn time-fn [o]
  (str o))

(def transit-encoder-handlers {java.time.Instant (transit/write-handler "time/instant" time-fn)
                               java.time.Month (transit/write-handler "time/month" time-fn)
                               java.time.DayOfWeek (transit/write-handler "time/day-of-week" time-fn)
                               java.time.Year (transit/write-handler "time/year" time-fn)})

(def transit-decoder-handlers {"time/instant" (transit/read-handler (fn [x] (t/instant x)))
                               "time/month" (transit/read-handler (fn [x] (t/month x)))
                               "time/day-of-week" (transit/read-handler (fn [x] (t/day-of-week x)))
                               "time/year" (transit/read-handler (fn [x] (t/year x)))})



(def muuntaja-instance
  (-> m/default-options
      (update-in [:formats] dissoc "application/transit+msgpack" "application/edn")
      (assoc :charsets #{"utf-8"} )
      (update-in [:formats "application/transit+json"] assoc
                 :encoder-opts {:handlers transit-encoder-handlers}
                 :decoder-opts {:handlers transit-decoder-handlers})
      (m/create)))

(defn get-routes [settings]
  (ring/ring-handler
   (ring/router
    [
     ["/"
      {:middleware [[wrap-basic-authentication http-basic-authenticated?]]
       :get {:no-doc true
             :handler (fn [_]
                        {:status 200
                         :headers {"Content-Type" "text/html; charset=utf-8"}
                         :body (home (:frontend settings))})}}]

     ["/static/*"
      {:middleware [[middleware/wrap-allow-credentials true]
                    [wrap-cors
                     :access-control-allow-origin [#".*"]
                     :access-control-allow-methods [:get]]
                    [wrap-not-modified]
                    [wrap-content-type]]
       :get {:handler (fn [request]
                        (let [path (first (vals (:path-params request)))]
                          (ring.response/resource-response path {:root "public"})))}}]
     ["/media/*"
      {:middleware [[middleware/wrap-allow-credentials true]
                    [wrap-cors
                     :access-control-allow-origin [#".*"]
                     :access-control-allow-methods [:get]]
                    [wrap-not-modified]
                    [wrap-content-type]]
       :get {:handler (fn [request]
                        (let [path (first (vals (:path-params request)))]
                          (ring.response/file-response path {:root (get-in config [:storage :directory])
                                                             :index-files? false})))}}]

     ["/swagger.json"
      {:get {:no-doc true
             :swagger {:info {:title "Songpark API"}}
             :handler (swagger/create-swagger-handler)}}]

     ["/admin/reset-rooms"
      {:middleware [[wrap-basic-authentication http-basic-authenticated?]]
       :post {:handler #'api.admin/reset-rooms}}]

     ;; health check
     ["/health"
      {:swagger {:tags ["health"]}}
      [""
       {:get {:responses {200 {:body :http/empty?}}
              :handler (fn [_]
                         {:status 200
                          :body ""})}}]]

     ["/version"
      {:swagger {:tags ["version"]}}
      [""
       {:get {:responses {200 {:body map?}}
              :handler #'api.version/get-version}}]]

     ["/latest-available-version"
      {:swagger {:tags ["version"]}}
      [""
       {:get {:responses {200 {:body any?}}
              :handler #'api.version/get-latest-available-version}}]]

     ["/auth"
      [""
       {:swagger {:tags ["auth"]}
        :get {:responses {200 {:body :auth/whoami}
                          500 {:body :error/error}}
              :handler #'api.auth/whoami}}]
      ["/signup"
       {:swagger {:tags ["auth"]}
        :post {:responses {200 {:body :auth/user}
                           400 {:body :error/error}
                           500 {:body :error/error}}
               :parameters {:body :auth/signup}
               :handler #'api.auth/signup}}]
      ["/login"
       {:swagger {:tags ["auth"]}
        :post {:responses {200 {:body :auth/user}
                           400 {:body :error/error}
                           500 {:body :error/error}}
               :parameters {:body :auth/login}
               :handler #'api.auth/login}}]
      ["/logout"
       {:swagger {:tags ["auth"]}
        :post {:responses {200 {:body :http/ok}}
               :handler #'api.auth/logout}}]
      ["/verify-email"
       {:swagger {:tags ["auth"]}
        :post {:responses {200 {:body :http/ok}
                           400 {:body :error/error}}
               :parameters {:body :auth/verify-email}
               :handler #'api.auth/verify-email}}]
      ["/send-verify-email"
       {:swagger {:tags ["auth"]}
        :post {:responses {200 {:body :http/ok}
                           400 {:body :error/error}}
               :parameters {:body :auth/send-verify-email}
               :handler #'api.auth/send-verify-email}}]
      ["/forgotten-password"
       {:swagger {:tags ["auth"]}
        :post {:responses {200 {:body :http/ok}}
               :parameters {:body :auth/forgotten-password}
               :handler #'api.auth/forgotten-password}}]
      ["/reset-password"
       {:swagger {:tags ["auth"]}
        :post {:responses {200 {:body :http/ok}}
               :parameters {:body :auth/reset-password}
               :handler #'api.auth/reset-password}}]
      ["/change-password"
       {:middleware [[wrap-authn]]
        :swagger {:tags ["auth"]}
        :post {:responses {200 {:body :http/ok}}
               :parameters {:body :auth/change-password}
               :handler #'api.auth/change-password}}]]

     ["/echo"
      {:swagger {:tags ["testing"]}
       :post {:responses {200 {:body any?}}
              :handler (fn [{:keys [session] :as req}]
                         ;; (log/debug session)
                         ;; (log/debug (:identity req))
                         {:status 200
                          :body (or (:body-params req) {:failed true})})}}]
     ;; auth
     ["/api"
      ;; everything under /api needs to be authenticated
      ;;{:middleware [[wrap-authn]]}
      ["/teleporter"
       {:swagger {:tags ["teleporter"]}}
       [""
        {:put {:responses {200 {:body :teleporter.init/response}
                           400 {:body :error/error}}
               :parameters {:body any?}
               :handler #'api.teleporter/init}
         :post {:responses {200 {:body (spec/keys :req [:teleporter/id])}
                            400 {:body :error/error}}
                :parameters {:body any?}
                :handler #'api.teleporter/update}}]
       ["/pair"
        {:get {:responses {200 {:body :pairing/pairs}}
               :handler #'api.pairing/get-pairs}
         :post {:responses {200 {:body :http/ok}}
                :parameters {:body :pairing.teleporter/paired}
                :handler #'api.pairing/paired}
         :put {:responses {200 {:body :http/ok}
                           400 {:body :error/error}}
               :parameters {:body :pairing/pair}
               :handler #'api.pairing/pair}
         :delete {:responses {200 {:body :pairing/unpair}
                              400 {:body :error/error}}
                  :parameters {:body :pairing/unpair}
                  :handler #'api.pairing/unpair}}]
       ["/settings"
        {:swagger {:tags ["teleporter" "settings"]}}
        [""
         {:post {:responses {200 {:body :teleporter/settings}
                             400 {:body :error/error}}
                 :parameters {:body :teleporter/settings}
                 :handler #'api.teleporter/settings}}]]]
      ["/fx"
       {:swagger {:tags ["fx"]}}
       [""
        {:get {:responses {200 {:body :fx/presets}
                           400 {:body :error/error}}
               :handler #'api.fx/get-presets}
         :put {:responses {200 {:body :fx/preset}
                           400 {:body :error/error}}
               :parameters {:body :fx.preset/save}
               :handler #'api.fx/save-preset}
         :post {:responses {200 {:body :fx/preset}
                            400 {:body :error/error}}
                :parameters {:body :fx.preset/update}
                :handler #'api.fx/update-preset}
         :delete {:responses {200 {:body :fx.preset/deleted}
                              400 {:body :error/error}}
                  :parameters {:body :fx.preset/delete}
                  :handler #'api.fx/delete-preset}}]]
      ["/room"
       {:swagger {:tags ["room"]}}
       [""
        {:get {:responses {200 {:body :room/rooms}}
               :handler #'api.room/get-rooms}
         :post {:responses {200 {:body :room/room}
                            400 {:body :error/error}}
                :parameters {:body :room/update}
                :handler #'api.room/update-room}
         :put {:responses {200 {:body :room/room}
                           400 {:body :error/error}}
               :parameters {:body :room/save}
               :handler #'api.room/save-room}}]
       ["/jam"
        [""
         {:get {:respones {200 {:body (spec/or :jam :room/jam
                                               :http-ok :http/ok)}
                           400 {:body :error/error}}
                :handler #'api.room-jam/current}}]
        ["/history"
         {:get {:responses {200 {:body :room.jam/history}
                            400 {:body :error/error}}
                :handler #'api.room-jam/jam-history}}]
        ["/host"
         {:post {:responses {200 {:body :room.jam/hosted}
                             400 {:body :error/error}}
                 :parameters {:body :room.jam/host}
                 :handler #'api.room-jam/host}}]
        ["/knock"
         {:post {:responses {200 {:body :room.jam/knocked}
                             400 {:body :error/error}}
                 :parameters {:body :room.jam/knock}
                 :handler #'api.room-jam/knock}}]
        ["/accept"
         {:post {:responses {200 {:body :room.jam/accepted}
                             400 {:body :error/error}}
                 :parameters {:body :room.jam/accept}
                 :handler #'api.room-jam/accept}}]
        ["/decline"
         {:post {:responses {200 {:body :room.jam/declined}
                             400 {:body :error/error}}
                 :parameters {:body :room.jam/decline}
                 :handler #'api.room-jam/decline}}]
        ["/leave"
         {:post {:responses {200 {:body :http/ok}
                             400 {:body :error/error}}
                 :parameters {:body :room.jam/leave}
                 :handler #'api.room-jam/leave}}]
        ["/remove"
         {:post {:responses {200 {:body :room.jam/removed}
                             400 {:body :error/error}}
                 :parameters {:body :room.jam/remove}
                 :handler #'api.room-jam/remove}}]
        ["/close"
         {:post {:responses {200 {:body :http/ok}
                             400 {:body :error/error}}
                 :parameters {:body :room.jam/close}
                 :handler #'api.room-jam/close}}]]]
      ["/profile"
       {:swagger {:tags ["profile"]}}
       [""
        {:get {:responses {200 {:body :profile/profile}}
               :handler #'api.profile/get-profile}
         :post {:responses {200 {:body :profile/profile}
                            400 {:body :error/error}
                            500 {:body :error/error}}
                :parameters {:body :profile/save}
                :handler #'api.profile/save-profile}}]]
      ["/app"
       {:swagger {:tags ["app"]}}
       [""
        {:get {:responses {200 {:body any?}}
               :handler #'api.app/connect}}]]]]

    {:exception pretty/exception
     ;;:compile r.coercion/compile-request-coercers
     :data {:coercion reitit.coercion.spec/coercion
            :muuntaja muuntaja-instance
            :middleware [wrap-exceptions
                         [middleware/wrap-allow-credentials true]
                         [wrap-cors
                          :access-control-allow-origin [#".*"]
                          :access-control-allow-methods [:get :post :options :put :delete]]

                         [middleware/inject-data (:songpark/data settings)]
                         [wrap-session {:store (:store settings)
                                        :cookie-attrs (:http/cookies settings)}]

                         #_[middleware/wrap-debug-inject {:session {:identity {:teleporter/id 1
                                                                               :authz/credentials #{:teleporter}}}}]

                         ;; swagger feature
                         swagger/swagger-feature
                         ;; query-params & form-params
                         parameters/parameters-middleware
                         ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                         ;; encoding response body
                         muuntaja/format-response-middleware
                         ;; exception handling
                         ;;exception/exception-middleware
                         middleware/wrap-exceptions
                         [wrap-authentication (:authz.backend/session settings)]
                         [wrap-authorization (:authz.backend/session settings)]

                         ;; decoding request body
                         muuntaja/format-request-middleware
                         ;; coercing response bodys
                         coercion/coerce-response-middleware
                         ;; coercing request parameters
                         coercion/coerce-request-middleware
                         ;; multipart
                         multipart/multipart-middleware]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/docs"
      :config {:validatorUrl nil
               :operationsSorter "alpha"}})
    (ring/create-default-handler))))
