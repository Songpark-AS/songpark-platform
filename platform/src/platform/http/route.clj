(ns platform.http.route
  (:require [platform.api.auth :as api.auth]
            [platform.api.locale :as api.locale]

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
            [tick.alpha.api :as t]
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
    [["/"
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

     ;; auth
     ["/auth"
      {:swagger {:tags ["auth"]}}
      [""
       {:get {:responses {200 {:body :auth/user}
                          400 {:body :error/error}}
              :handler #'api.auth/user}}]
      ["/login"
       {:post {:responses {200 {:body :auth/user}
                           401 {:body :error/error}}
               :parameters {:body :auth/auth}
               :handler #'api.auth/login}}]
      ["/logout"
       {:post {:responses {200 {:body :http/empty?}}
               :handler #'api.auth/logout}}]]

     ["/api"
      ;; everything under /api needs to be authenticated
      {:middleware [[wrap-authn]]}
      ["/echo"
       {:swagger {:tags ["testing"]}
        :post {:responses {200 {:body any?}}
               :handler (fn [req]
                          {:status 200
                           :body (or (:body-params req) {:failed true})})}}]

      ;; locale
      ["/locale"
       {:middleware [[wrap-authz #{:admin}]]
        :swagger {:tags ["locale"]}}
       [""
        {:get  {:responses {200 {:body :locale/locales}}
                :handler   #'api.locale/locales}
         :post {:responses  {204 {:body :http/empty?}
                             404 {:body :http/empty?}}
                :parameters {:body :locale/locale}
                :handler    #'api.locale/update-locale}
         :put {:responses {201 {:body :locale/locale}
                           400 {:body :error/error}}
               :parameters {:body :locale/locale}
               :handler #'api.locale/create-locale}}]

       ["/:locale-id"
        {:get {:responses  {200 {:body :locale/locale}
                            404 {:body :http/empty?}}
               :parameters {:path {:locale-id string?}}
               :handler    #'api.locale/by-id}}]]
      ]
     ]

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

                         #_[middleware/wrap-debug-inject {:session {:identity {:teacher/id 1
                                                                               :school/id 1
                                                                               :authz/credentials #{:teacher}}}}]

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
                         [wrap-authentication (:authz.platform/session settings)]
                         [wrap-authorization (:authz.platform/session settings)]

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
