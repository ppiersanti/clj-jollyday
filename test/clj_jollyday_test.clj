(ns clj-jollyday-test
  (:require [clojure.test :refer :all]
            [clj-jollyday :refer :all])
  (:import [java.time LocalDate]))

(deftest holiday?-test
  (let [hm (manager :italy)]
    (testing "holiday is recognized"
      (is (true? (holiday? hm (LocalDate/of 2018 12 25)))))
    (testing "false is returned"
      (is (false? (holiday? hm (LocalDate/of 2018 12 28)))))))

(deftest holiday-test
  (let [hm (manager :italy)]
    (testing "easter is recognized"
      (is (= {:description    "Easter",
              :properties-key :christian.easter,
              :date           (LocalDate/parse "2018-04-01")}
             (holiday hm 2018 :christian.easter))))))
