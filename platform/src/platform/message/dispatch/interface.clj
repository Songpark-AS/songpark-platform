(ns platform.message.dispatch.interface
  (:require [taoensso.timbre :as log]))

(defmulti dispatch :message/type)

(defmethod dispatch :debug [{:message/keys [body]}]
  (log/debug :dispatch.debug body))

(defmethod dispatch :default [{:message/keys [type] :as message}]
  (log/warn
   (str "No message dispatch defined for message type " type) message))
