(ns platform.connection.parsesql
  (:require [yesql.core :refer [defqueries]]))

(defn parsesql []
  (let [db-spec {:classname "org.postgresql.Driver"
                 :subprotocol "postgresql"
                 :subname "//localhost:5432/songpark"
                 :user "postgres"
                 :password "postgres"}]
    (defqueries "platform/connection/queries/test.sql" {:connection db-spec})))

(parsesql)