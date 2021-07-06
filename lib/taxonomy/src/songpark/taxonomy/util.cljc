(ns songpark.taxonomy.util)

(defn url? [url]
  #?(:clj (instance? java.net.URL url)
     :cljs (string? url)))
(defn tick? [value]
  (= (type value) java.time.Instant))
(defn period? [value]
  (= (type value) java.time.Period))

(def status #{:passed
              :failed
              :sent
              :received})

