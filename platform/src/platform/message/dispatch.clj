(ns platform.message.dispatch
  (:require [platform.message.dispatch.platform]
            [platform.message.dispatch.teleporter]
            [platform.message.dispatch.jam]
            [platform.message.dispatch.interface :as interface]))


(defn handler [msg]
  (interface/dispatch msg))
