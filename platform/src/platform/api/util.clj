(ns platform.api.util
  (:require [platform.auth :as auth]))

(defn process-body
  "Takes the body in a request, and merges the credential-ks into the body if the user is a student/teacher"
  [request credential-ks]
  (let [body (get-in request [:parameters :body])]
    (if (auth/auth-user? request)
      body
      (auth/merge-credentials request
                              body
                              credential-ks))))

(defn process-query
  "Takes the query params in a request, and merges the credential-ks into the params if the user is a student/teacher"
  [request credential-ks]
  (let [params (get-in request [:parameters :query])]
    (if (auth/auth-user? request)
      params
      (auth/merge-credentials request
                              params
                              credential-ks))))
