(ns platform.http.middleware
  (:require [platform.auth.authz :as authz]
            [platform.config :refer [config]]
            [buddy.auth :refer [authenticated?]]
            [reitit.ring.middleware.exception :as exception]
            [taoensso.timbre :as log]))


(defn http-basic-authenticated? [username password]
  (and (= username (get-in config [:http :basic-auth :username]))
       (= password (get-in config [:http :basic-auth :password]))))

(defrecord SongparkData [])

;; hide the data when printing to stdout or anywhere else
;; this is primarily to avoid having sensitive data like credentials
;; being written to logs
(defmethod clojure.core/print-method SongparkData [data ^java.io.Writer writer]
  (.write writer "#<SongparkData>"))


(defn inject-data [handler data]
  (let [data (map->SongparkData data)]
    (fn [request]
      (handler (assoc request :data data)))))


(derive ::error ::exception)
(derive ::failure ::exception)

(defn exception-handler [message exception request]
  (let [body {:type message
              :message (ex-message exception)
              :exception (str (.getClass exception))
              :uri (:uri request)}
        stacktrace (apply str (interpose "\n" (.getStackTrace exception)))]
    (log/error "Exception: " (assoc body
                                    :identity (:identity request)
                                    :stacktrace stacktrace
                                    :data (ex-data exception)))
    (log/debug stacktrace)
    {:status 500
     :body body}))

(defn authz-exception-handler [exception request]
  (let [body {:message (ex-message exception)
              :exception (str (.getClass exception))
              :data (ex-data exception)
              :uri (:uri request)
              :identity (:identity request)}]
    (log/warn "Permission denied" body)
    {:status 403
     :body "Permission denied"}))

(defn authn-exception-handler [exception request]
  {:status 401
   :body "Access denied"})

(def wrap-exceptions
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {;; ex-data with :type ::error
     ::error (partial exception-handler "error")

     ;; ex-data with ::exception or ::failure
     ::exception (partial exception-handler "exception")

     :auth/not-authorized authz-exception-handler

     :auth/not-authenticated authn-exception-handler

     ;; override the default handler
     ::exception/default (partial exception-handler "default")

     ;; ;; print stack-traces for all exceptions
     ;; ::exception/wrap (fn [handler e request]
     ;;                    (exception-handler e request))
     })))

(defn wrap-allow-credentials [handler value]
  (fn [request]
    (let [resp (handler request)]
      (-> resp
          (assoc-in [:headers "Access-Control-Allow-Credentials"] (str value))))))


(defn wrap-authz [handler roles]
  (fn [{:keys [identity] :as request}]
    (authz/allow? identity roles)
    (handler request)))

(defn wrap-authn [handler]
  (fn [request]
    (when-not (authenticated? request)
      (throw (ex-info "Access denied" {:type :auth/not-authenticated})))
    (handler request)))

(defn unauthorized-handler [request metadata]
  (throw (ex-info (:message metadata) (dissoc metadata :message))))

(defn wrap-debug-inject [handler data]
  (fn [request]
    (handler (merge request data))))
