(ns platform.http.route
  (:require [platform.api.app :as api.app]
            [platform.api.auth :as api.auth]
            [platform.api.jam :as api.jam]
            [platform.api.profile :as api.profile]
            [platform.api.teleporter :as api.teleporter]
            [platform.api.version :as api.version]
            [platform.http.html :refer [home]]
            [platform.http.middleware :as middleware :refer [wrap-authn
                                                             wrap-authz]]
            [buddy.auth.middleware :refer [wrap-authentication
                                           wrap-authorization]]
            [clojure.spec.alpha :as spec]
            [cognitect.transit :as transit]
            [muuntaja.core :as m]
            [songpark.taxonomy.error]
            [songpark.taxonomy.http]
            [songpark.taxonomy.jam]
            [songpark.taxonomy.teleporter]
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
            [taoensso.timbre :as log]))


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
      {:get {:no-doc true
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

     ["/swagger.json"
      {:get {:no-doc true
             :swagger {:info {:title "Songpark API"}}
             :handler (swagger/create-swagger-handler)}}]

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
               :handler #'api.auth/reset-password}}]]

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
      {:middleware [[wrap-authn]]}
      ["/teleporter"
       {:swagger {:tags ["teleporter"]}}
       [""
        {:put {:responses {200 {:body (spec/keys :req [:teleporter/id])}
                           400 {:body :error/error}}
               :parameters {:body any?}
               :handler #'api.teleporter/init}
         :post {:responses {200 {:body (spec/keys :req [:teleporter/id])}
                            400 {:body :error/error}}
                :parameters {:body any?}
                :handler #'api.teleporter/update}}]]
      ["/profile"
       {:swagger {:tags ["profile"]}}
       [""
        {:get {:responses {200 {:body :profile/profile}}
               :handler #'api.profile/get-profile}
         :post {:responses {200 {:body :http/ok}
                            400 {:body :error/error}
                            500 {:body :error/error}}
                :parameters {:body :profile/profile}
                :handler #'api.profile/save-profile}}]
       ["/pronouns"
        {:get {:responses {200 {:body :profile/pronouns}}
               :handler #'api.profile/get-pronouns}}]]
      ["/app"
       {:swagger {:tags ["app"]}}
       [""
        {:get {:responses {200 {:body any?}}
               :handler #'api.app/connect}}]]
      ["/jam"
       {:swagger {:tags ["jam"]}}
       [""
        {:delete {:responses {200 {:body :jam/stopped}
                              400 {:body :error/error}}
                  :parameters {:body :jam/stop}
                  :handler #'api.jam/stop}}]
       ["/ask"
        {:put {:responses {200 {:body :jam/asked}
                           400 {:body :error/error}}
               :parameters {:body :jam/ask}
               :handler #'api.jam/ask}
         :delete {:responses {200 {:body :jam/obviated}
                              400 {:body :error/error}}
                  :parameters {:body :jam/obviate}
                  :handler #'api.jam/obviate}}]]]]

    {:exception pretty/exception
     ;;:compile r.coercion/compile-request-coercers
     :data {:coercion reitit.coercion.spec/coercion
            :muuntaja muuntaja-instance
            :middleware [[middleware/wrap-allow-credentials true]
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
