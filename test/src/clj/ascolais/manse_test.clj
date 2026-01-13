(ns ascolais.manse-test
  (:require [clojure.test :refer [deftest is testing]]
            [ascolais.manse :as manse]))

(deftest greet-test
  (testing "greet returns a greeting message"
    (is (= "Hello, World!" (manse/greet "World")))
    (is (= "Hello, Clojure!" (manse/greet "Clojure")))))
