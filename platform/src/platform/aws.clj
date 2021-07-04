(ns platform.aws
  (:require [amazonica.aws.s3 :as s3]
            [buddy.core.codecs.base64 :as base64]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]))

(defn send-to-aws-s3!
  "Sends an audio file to aws s3 within the specified bucket"
  [{:keys [credentials] :as _aws} bucket-name key b64-audio-file]
  (let [audio-bytes (base64/decode b64-audio-file)
        content-length (count audio-bytes)]
    (s3/put-object credentials {:bucket-name bucket-name
                                :key key
                                :input-stream (io/input-stream audio-bytes)
                                :metadata {:content-length content-length}})))

(defn- get-credentials
  [aws-access-key-id aws-secret-access-key region]
  {:access-key aws-access-key-id
   :secret-key aws-secret-access-key
   :endpoint region})

(defrecord AWSClient [access-key-id secret-access-key region started? credentials]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (let [credentials (get-credentials access-key-id secret-access-key region)]
        (log/info "Starting AWS client")
        (assoc this :credentials credentials
                    :started? true))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/info "Stopping AWS client")
        (assoc this :credentials nil
                    :started? false)))))

(defn aws-client
  [settings]
  (map->AWSClient settings))
