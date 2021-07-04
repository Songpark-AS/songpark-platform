(ns platform.google
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [taoensso.timbre :as log])
  (:import (com.google.cloud.speech.v1 RecognitionAudio
                                       RecognitionConfig
                                       RecognitionConfig$AudioEncoding
                                       SpeechClient SpeechSettings)
           (com.google.protobuf ByteString)
           (com.google.api.gax.core FixedCredentialsProvider)
           (com.google.auth.oauth2 ServiceAccountCredentials)))

(defn- make-settings
  "Initializes settings for the Google Speech to text service client"
  [credentials-file]
  (try
    (let [credentials-provider (-> (io/resource credentials-file)
                                   (io/input-stream)
                                   (ServiceAccountCredentials/fromStream)
                                   (FixedCredentialsProvider/create))]
      (-> (SpeechSettings/newBuilder)
          (.setCredentialsProvider credentials-provider)
          (.build)))
    (catch Exception e
      (ex-message e))))

(defn- make-google-speech-to-text-client
  "Creates an instance of Google Speech to text client"
  [credentials-file]
  (try
    (-> (SpeechClient/create (make-settings credentials-file)))
    (catch Exception e
      (ex-message e))))

(defn- make-recognition-config
  "Builds the configuration for Google Speech to text recognition"
  [locale sample-rate-hertz]
  (try
    (-> (RecognitionConfig/newBuilder)
        (.setEncoding RecognitionConfig$AudioEncoding/LINEAR16)
        (.setSampleRateHertz (or sample-rate-hertz 44100))
        (.setAudioChannelCount 1)
        (.setLanguageCode locale)
        (.build))
    (catch Exception e
      (ex-message e))))

(defn- make-recognition-audio
  "Prepares the audio file for the Google Speech to text service api"
  [#^bytes base64-audio-file]
  (try
    (-> (RecognitionAudio/newBuilder)
        (.setContent (ByteString/copyFrom base64-audio-file))
        (.build))
    (catch Exception e
      (ex-message e))))

(defn- recognize
  "Sends the audio file for recognition to Google cloud api"
  [speech-client config audio-file]
  (.recognize speech-client config audio-file))

(defn- get-words
  "Given a google.cloud.speech.v1.SpeechRecognitionAlternative,
  retrieves the list of each recognized words and their respective
  attributes"
  [alternative]
  (if (pos? (.getWordsCount alternative))
    (-> alternative
        (.getWordsList)
        (seq)
        (map (fn [word-info]
               {:word  (.getWord word-info)
                :start (.getStartTime word-info)
                :stop  (.getEndTime word-info)})))
    nil))

(defn- get-transcripts
  "Extracts the `transcript` and `confidence` info from the Google Speech
  to Text response"
  [recognize-response]
  (->> recognize-response
       (.getResultsList)
       (seq)                                                ; (seq %) Needed for java/clojure interop here
       (map #(seq (.getAlternativesList %)))                ; (seq %) Needed for java/clojure interop here
       flatten
       (map (fn [alternative]
              {:transcript (.getTranscript alternative)
               :confidence (.getConfidence alternative)
               :word-infos (get-words alternative)}))))

; Defines the contract for any external component to call
; for Google Speech to Text recognition
(defprotocol IGoogleSpeechToText
  (get-speech-transcripts [this base64-audio-file locale]))

(defrecord GoogleClient [credentials-file started? sample-rate-hertz]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (let [speech-to-text-client (make-google-speech-to-text-client credentials-file)]
        (log/info "Starting Google Speech to text client")
        (assoc this :google-speech-to-text-client speech-to-text-client
                    :started? true))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/info "Stopping Google Speech to text client")
        (assoc this :google-speech-to-text-client nil
                    :started? false))))

  IGoogleSpeechToText
  (get-speech-transcripts [this base64-audio-file locale]
    (try
      (let [; init client, configuration and input file
            speech-client (:google-speech-to-text-client this)
            config (make-recognition-config locale sample-rate-hertz)
            audio (make-recognition-audio base64-audio-file)

            ; make api call to Google Speech to text service and
            ; retrieve transcripts from response
            transcripts (-> (recognize speech-client config audio)
                            (get-transcripts))]
        (first transcripts))
      (catch Exception e
        (ex-message e)))))

(defn google-client
  [settings]
  (map->GoogleClient settings))
