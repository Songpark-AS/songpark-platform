(ns platform.test.api.dashboard
  (:require [midje.sweet :refer :all]
            [platform.model.dashboard :as dashboard]
            [platform.test.util :as util]
            [clojure.spec.alpha :as spec]))



(fact "scoring"
      (let [db (util/start-db)
            aws (util/start-aws)]
        (try
          (util/seed-db! db)
          (let [result (dashboard/score-students db aws 1 [4 5] nil)]
            (and (some? result)
                 (spec/valid? :dashboard.score/students result)))
          => true
          (finally
            (util/stop-db db)
            (util/stop-aws aws)))))
