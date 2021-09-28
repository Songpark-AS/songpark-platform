(ns platform.util.mock
  (:require [clj-uuid :as uuid]))

(defn nickname []
  (let [nouns #{"Hope" "Dream" "Party" "Jam"}
        persons ["Mathias" "Achim" "Magnus" "Thor_Atle" "Christian" "Jan_William" "Sindre" "Ronny" "Emil" "Kenneth" "Thanks" "Alf-Gunnar" "Daniel"]
        person (rand-nth persons)
        person-last-letter (clojure.string/join (take-last 1 person))
        noun (rand-nth nouns)]
    (str person (when (not (= person-last-letter "s")) "s") "." noun)))

(defn nicknames [n]  
  (for [i (range n)]
    (nickname)))

(defn teleporter-factory [nickname]
  {:teleporter/uuid (uuid/v4)
   :teleporter/nickname nickname})

(defn random-teleporters [n]
  (let [nicks (->> (distinct (nicknames (* n 10))) (take n))]
    (mapv #(teleporter-factory %) nicks)))
