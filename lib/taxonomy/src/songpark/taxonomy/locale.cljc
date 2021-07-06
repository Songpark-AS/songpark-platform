(ns songpark.taxonomy.locale
  (:require [clojure.spec.alpha :as spec]))

;; no-NB is an example of an id
(spec/def :locale/id string?)
(spec/def :locale/ids (spec/coll-of :locale/id))
(spec/def :locale/name string?)

(spec/def :locale/locale (spec/keys :req [:locale/id :locale/name]))
(spec/def :locale/locales (spec/coll-of :locale/locale))
