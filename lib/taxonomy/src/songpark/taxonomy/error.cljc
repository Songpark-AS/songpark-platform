(ns songpark.taxonomy.error
  (:require [clojure.spec.alpha :as spec]))


(spec/def :error/message string?)
(spec/def :error/exception string?)
(spec/def :error/data any?)

(spec/def :error/error (spec/keys :req [:error/message]
                                  :opt [:error/exception
                                        :error/data]))
