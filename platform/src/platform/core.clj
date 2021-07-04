(ns platform.core
  (:gen-class)
  (:require [platform.init :as init]
            [taoensso.timbre :as log]))

(defn -main [& args]
  (log/set-level! :info)
  (init/init))
