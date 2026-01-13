(ns ascolais.manse.schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [ascolais.manse.schema :as schema]
            [malli.core :as m]))

(deftest sql-params-schema-test
  (testing "valid sql-params"
    (is (m/validate schema/sql-params ["SELECT * FROM users"]))
    (is (m/validate schema/sql-params ["SELECT * FROM users WHERE id = ?" 1]))
    (is (m/validate schema/sql-params ["INSERT INTO users(name, email) VALUES(?, ?)" "alice" "alice@example.com"]))
    (is (m/validate schema/sql-params ["SELECT 1"])))

  (testing "invalid sql-params"
    (is (not (m/validate schema/sql-params [])))
    (is (not (m/validate schema/sql-params [1 2 3])))
    (is (not (m/validate schema/sql-params "SELECT 1")))))

(deftest execute-opts-schema-test
  (testing "valid execute-opts"
    (is (m/validate schema/execute-opts {}))
    (is (m/validate schema/execute-opts {:builder-fn identity}))
    (is (m/validate schema/execute-opts {:return-keys true}))
    (is (m/validate schema/execute-opts {:return-keys [:id :name]}))
    (is (m/validate schema/execute-opts {:connectable :some-datasource}))
    (is (m/validate schema/execute-opts {:builder-fn identity
                                         :return-keys true
                                         :column-fn identity})))

  (testing "invalid execute-opts"
    (is (not (m/validate schema/execute-opts "not a map")))
    (is (not (m/validate schema/execute-opts [:not :a :map])))))

(deftest transaction-opts-schema-test
  (testing "valid transaction-opts"
    (is (m/validate schema/transaction-opts {}))
    (is (m/validate schema/transaction-opts {:isolation :read-committed}))
    (is (m/validate schema/transaction-opts {:isolation :read-uncommitted}))
    (is (m/validate schema/transaction-opts {:isolation :repeatable-read}))
    (is (m/validate schema/transaction-opts {:isolation :serializable}))
    (is (m/validate schema/transaction-opts {:read-only true}))
    (is (m/validate schema/transaction-opts {:read-only false}))
    (is (m/validate schema/transaction-opts {:rollback-only true}))
    (is (m/validate schema/transaction-opts {:isolation :serializable
                                             :read-only true
                                             :rollback-only false})))

  (testing "invalid transaction-opts"
    (is (not (m/validate schema/transaction-opts "not a map")))
    (is (not (m/validate schema/transaction-opts {:isolation :invalid-level})))
    (is (not (m/validate schema/transaction-opts {:read-only "true"})))))
