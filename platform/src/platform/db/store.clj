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
  (wr [:teleporter "e4:95:6e:43:fe:9b"] {:teleporter/mac "e4:95:6e:43:fe:9b"
                                         :teleporter/uuid #uuid "844ac6ff-e9a2-57ac-b91a-41cf4e6d74c8"
                                         :teleporter/nickname "zedboard-2"
                                         :teleporter/sip "sip:9115@voip1.inonit.no"})
  (wr [:teleporter "74:da:88:c2:14:b4" :teleporter/sip] "sip:9114@voip1.inonit.no")

  (reset! state {:teleporter {"74:da:88:c2:14:b4" #:teleporter{:uuid #uuid "f7a21b06-014d-5444-88d7-0374a661d2de",
                                                               :mac "74:da:88:c2:14:b4",
                                                               :nickname "zedboard-01"
                                                               :sip "sip:9114@voip1.inonit.no"}
                              "e4:95:6e:43:fe:9b" {:teleporter/mac "e4:95:6e:43:fe:9b"
                                                   :teleporter/uuid #uuid "844ac6ff-e9a2-57ac-b91a-41cf4e6d74c8"
                                                   :teleporter/nickname "zedboard-2"
                                                   :teleporter/sip "sip:9115@voip1.inonit.no"
                                                   }},
                 
                 :jam {}})

  (println @state)

  (swap! state update-in [:teleporter] dissoc "mac"))
