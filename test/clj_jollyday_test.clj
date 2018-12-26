(ns clj-jollyday-test
  (:require [clojure.test :refer :all]
            [clj-jollyday :refer :all]
            [clojure.java.io :as io]
            [clojure.zip :as cz]
            [clojure.data.xml :as cdx]
            [clojure.xml :as cx])
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
      (is (= {:description       "Easter",
              :properties-key    :christian.easter,
              :date              (LocalDate/parse "2018-04-01")
              :official-holiday? true}
             (holiday hm 2018 :christian.easter))))))

(deftest manager-from-xml-stream
  (testing "manipulated xml stream"

    (let [xml           (slurp (io/resource "test.xml"))
          iss           (java.io.ByteArrayInputStream. (.getBytes xml))
          xml           (cz/xml-zip (cx/parse iss))
          hloc          (-> xml (cz/down))
          holidays_node (-> hloc
                            (cz/append-child {:tag     :tns:FixedWeekday
                                              :attrs   {:weekday                  "SUNDAY"
                                                        :which                    "FIRST"
                                                        :month                    "NOVEMBER"
                                                        :descriptionPropertiesKey "MY_CUSTOM"}
                                              :content nil}))

          m (manager (cdx/emit-str
                      (cz/root holidays_node)))]
      (is (true? (holiday? m (java.time.LocalDate/of 2018 11 4)))))))

(deftest manager-from-configuration
  (testing "set configuration"
    (let [xml           (slurp (io/resource "test.xml"))
          iss           (java.io.ByteArrayInputStream. (.getBytes xml))
          _             (de.jollyday.HolidayManager/setManagerCachingEnabled false)
          util          (de.jollyday.util.XMLUtil.)
          configuration (.unmarshallConfiguration util iss)
          holidays1     (.getHolidays configuration)
          cds           (proxy [de.jollyday.datasource.ConfigurationDataSource] []
                          (getConfiguration [param]
                            configuration))
          man           (doto (manager)
                          (.setConfigurationDataSource cds)
                          (.doInit))]
      (is (= 2 (count (holidays man 2018)))))))
