(ns kompetansia.common.config)

(def config (atom {}))

(def debug? goog.DEBUG)

(defn is? [path value]
  (= value (get-in @config path ::not-found)))
