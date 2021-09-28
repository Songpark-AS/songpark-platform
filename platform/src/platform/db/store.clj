(ns platform.db.store
  (:require [taoensso.timbre :as log]))


(def ^:private state (atom {:teleporter {}
                            :jam {}}))

(defn rd [ks]
  (get-in @state ks))

(defn wr 
  ([ks v] (swap! state assoc-in ks v))
  ([ks v f] (swap! state update-in ks f v)))



(comment
  (rd [:teleporter])
  (wr [:teleporter "mac"] {:tp/id 3234})
  (wr [:teleporter] "mac" dissoc)

  (swap! state update-in [:teleporter] dissoc "mac")
  
  )
