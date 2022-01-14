(ns platform.test.transit
  (:require [midje.sweet :refer :all]
            [platform.http.route :refer [muuntaja-instance]]
            [platform.test.util]
            [muuntaja.core :as m]
            [tick.alpha.api :as t]))



(fact "transit"
      (let [t-type "application/transit+json"
            encode (fn [data] (m/encode muuntaja-instance t-type data))
            decode (fn [data] (m/decode muuntaja-instance t-type data))]
        (fact "encode/decode"
              (fact "time/instant"
                    (let [data {:test (t/now)}]
                      (-> data encode decode)  => data))
              (fact "time/month"
                    (let [data (t/month 1)]
                      (-> data encode decode) => data))
              (fact "time/day-of-week"
                    (let [data (t/day-of-week 1)]
                      (-> data encode decode) => data))
              (fact "time/year"
                    (let [data (t/year 2020)]
                      (-> data encode decode) => data)))))
