(ns songpark.taxonomy.http
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as str]))

(spec/def :http/empty? str/blank?)

(spec/def :http.ok/result #{:success :failed})
(spec/def :http/ok (spec/or :map map?
                            :result (spec/keys :req-un [:http.ok/result])))
