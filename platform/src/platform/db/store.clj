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
  (wr [:teleporter] {"74:da:88:c2:14:b4" #:teleporter{:uuid #uuid "f7a21b06-014d-5444-88d7-0374a661d2de",
                                                      :mac "74:da:88:c2:14:b4",
                                                      :sip "sip:9114@voip1.inonit.no",
                                                      :nickname "zedboard-01"},
                     "b8:08:cf:30:6e:64" #:teleporter{:uuid #uuid "41077c69-4aba-53ee-b2e1-3bfca8629255",
                                                      :mac "b8:08:cf:30:6e:64",
                                                      :sip "sip:9115@voip1.inonit.no",
                                                      :nickname "zedboard-02"}})


  (println @state)
  (reset! state {:teleporter {}
                 :jam {}})

  (swap! state update-in [:teleporter] dissoc "mac"))
