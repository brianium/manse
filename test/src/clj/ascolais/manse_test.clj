(ns ascolais.manse-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [ascolais.manse :as manse]
            [ascolais.sandestin :as s]
            [next.jdbc :as jdbc])
  (:import [java.io File]))

(def ^:dynamic *datasource* nil)

(def ^:private test-db-file
  (let [f (File/createTempFile "manse-test" ".db")]
    (.deleteOnExit f)
    (.getAbsolutePath f)))

(defn with-sqlite-datasource
  "Test fixture that creates a file-based SQLite datasource for testing."
  [f]
  (let [ds (jdbc/get-datasource {:dbtype "sqlite"
                                 :dbname test-db-file})]
    (jdbc/execute! ds ["CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT)"])
    (jdbc/execute! ds ["DELETE FROM users"])
    (jdbc/execute! ds ["INSERT INTO users(name, email) VALUES(?, ?)" "alice" "alice@example.com"])
    (jdbc/execute! ds ["INSERT INTO users(name, email) VALUES(?, ?)" "bob" "bob@example.com"])
    (binding [*datasource* ds]
      (f))))

(use-fixtures :each with-sqlite-datasource)

(deftest registry-test
  (testing "registry requires datasource"
    (is (thrown? clojure.lang.ExceptionInfo
                 (manse/registry {}))))

  (testing "registry returns valid Sandestin registry"
    (let [reg (manse/registry {:datasource *datasource*})]
      (is (map? reg))
      (is (contains? reg :ascolais.sandestin/effects))
      (is (contains? reg :ascolais.sandestin/placeholders)))))

(deftest execute-effect-test
  (let [dispatch (s/create-dispatch [(manse/registry {:datasource *datasource*})])]

    (testing "execute returns vector of results"
      (let [{:keys [results errors]} (dispatch {} [[::manse/execute ["SELECT * FROM users"]]])]
        (is (empty? errors))
        (is (= 1 (count results)))
        (is (= 2 (count (-> results first :res))))))

    (testing "execute with parameters"
      (let [{:keys [results errors]} (dispatch {} [[::manse/execute ["SELECT * FROM users WHERE name = ?" "alice"]]])]
        (is (empty? errors))
        (is (= 1 (count (-> results first :res))))
        (is (= "alice" (-> results first :res first :users/name)))))

    (testing "execute with options"
      (let [{:keys [results errors]} (dispatch {} [[::manse/execute
                                                    ["SELECT * FROM users"]
                                                    {:builder-fn next.jdbc.result-set/as-unqualified-maps}]])]
        (is (empty? errors))
        (is (= "alice" (-> results first :res first :name)))))))

(deftest execute-with-continuation-test
  (let [*captured (atom nil)
        capture-effect {:ascolais.sandestin/description "Captures data for testing"
                        :ascolais.sandestin/schema [:tuple [:= ::capture] :any]
                        :ascolais.sandestin/handler (fn [_ _ data]
                                                      (reset! *captured data)
                                                      :captured)}
        dispatch (s/create-dispatch [(manse/registry {:datasource *datasource*})
                                     {:ascolais.sandestin/effects
                                      {::capture capture-effect}}])]

    (testing "continuation receives results via placeholder"
      (let [{:keys [results errors]} (dispatch {} [[::manse/execute
                                                    ["SELECT * FROM users WHERE name = ?" "alice"]
                                                    {}
                                                    [[::capture [::manse/results]]]]])]
        (is (empty? errors))
        (is (vector? @*captured))
        (is (= 1 (count @*captured)))
        (is (= "alice" (:users/name (first @*captured))))))))

(deftest execute-one-effect-test
  (let [dispatch (s/create-dispatch [(manse/registry {:datasource *datasource*})])]

    (testing "execute-one returns single result"
      (let [{:keys [results errors]} (dispatch {} [[::manse/execute-one ["SELECT * FROM users WHERE name = ?" "alice"]]])]
        (is (empty? errors))
        (is (= 1 (count results)))
        (is (map? (-> results first :res)))
        (is (= "alice" (-> results first :res :users/name)))))

    (testing "execute-one returns nil for no match"
      (let [{:keys [results errors]} (dispatch {} [[::manse/execute-one ["SELECT * FROM users WHERE name = ?" "nobody"]]])]
        (is (empty? errors))
        (is (nil? (-> results first :res)))))

    (testing "execute-one with options"
      (let [{:keys [results errors]} (dispatch {} [[::manse/execute-one
                                                    ["SELECT * FROM users WHERE name = ?" "bob"]
                                                    {:builder-fn next.jdbc.result-set/as-unqualified-maps}]])]
        (is (empty? errors))
        (is (= "bob" (-> results first :res :name)))))))

(deftest execute-one-with-continuation-test
  (let [*captured (atom nil)
        capture-effect {:ascolais.sandestin/description "Captures data for testing"
                        :ascolais.sandestin/schema [:tuple [:= ::capture] :any]
                        :ascolais.sandestin/handler (fn [_ _ data]
                                                      (reset! *captured data)
                                                      :captured)}
        dispatch (s/create-dispatch [(manse/registry {:datasource *datasource*})
                                     {:ascolais.sandestin/effects
                                      {::capture capture-effect}}])]

    (testing "continuation receives result via placeholder"
      (let [{:keys [errors]} (dispatch {} [[::manse/execute-one
                                            ["SELECT * FROM users WHERE name = ?" "alice"]
                                            {}
                                            [[::capture [::manse/result]]]]])]
        (is (empty? errors))
        (is (map? @*captured))
        (is (= "alice" (:users/name @*captured)))))

    (testing "continuation receives nil result via placeholder"
      (reset! *captured :not-set)
      (let [{:keys [errors]} (dispatch {} [[::manse/execute-one
                                            ["SELECT * FROM users WHERE name = ?" "nobody"]
                                            {}
                                            [[::capture [::manse/result]]]]])]
        (is (empty? errors))
        (is (nil? @*captured))))))

(deftest plan-effect-test
  (let [dispatch (s/create-dispatch [(manse/registry {:datasource *datasource*})])]

    (testing "plan returns a reducible"
      (let [{:keys [results errors]} (dispatch {} [[::manse/plan ["SELECT * FROM users"]]])]
        (is (empty? errors))
        (is (= 1 (count results)))
        (let [reducible (-> results first :res)]
          (is (some? reducible))
          ;; Reduce over the plan to verify it works
          (is (= 2 (reduce (fn [acc _] (inc acc)) 0 reducible))))))

    (testing "plan with transducer"
      (let [{:keys [results errors]} (dispatch {} [[::manse/plan ["SELECT * FROM users"]]])]
        (is (empty? errors))
        (let [reducible (-> results first :res)
              names (into [] (map :users/name) reducible)]
          (is (= ["alice" "bob"] names)))))))

(deftest plan-with-continuation-test
  (let [*captured (atom nil)
        capture-effect {:ascolais.sandestin/description "Captures data for testing"
                        :ascolais.sandestin/schema [:tuple [:= ::capture] :any]
                        :ascolais.sandestin/handler (fn [_ _ data]
                                                      (reset! *captured data)
                                                      :captured)}
        dispatch (s/create-dispatch [(manse/registry {:datasource *datasource*})
                                     {:ascolais.sandestin/effects
                                      {::capture capture-effect}}])]

    (testing "continuation receives reducible via placeholder"
      (let [{:keys [errors]} (dispatch {} [[::manse/plan
                                            ["SELECT * FROM users"]
                                            {}
                                            [[::capture [::manse/reducible]]]]])]
        (is (empty? errors))
        (is (some? @*captured))
        ;; Verify it's reducible
        (is (= 2 (reduce (fn [acc _] (inc acc)) 0 @*captured)))))))
