(ns clj-jollyday
  (:require [clj-jollyday.util :as util]
            [clojure
             [xml :as cx]
             [zip :as cz]]
            [clojure.data.xml :as cdx]
            [clojure.java.io :as io])
            [clojure.string :as str])
  (:import [de.jollyday HolidayCalendar HolidayManager]
           java.time.LocalDate))


(def predefined-calendars
  (->> (util/get-static-fields-of-type HolidayCalendar HolidayCalendar)
       (util/map-kv
        (fn [^String n fmt]
          [(str/lower-case (.replace n \_ \-)) fmt]))))

(defprotocol CreateManager
  (create-manager [input]))

(extend-protocol CreateManager
  String
  (create-manager [xml]
    (let [is            (java.io.ByteArrayInputStream. (.getBytes xml))
          _             (de.jollyday.HolidayManager/setManagerCachingEnabled false)
          util          (de.jollyday.util.XMLUtil.)
          configuration (.unmarshallConfiguration util is)
          cds           (proxy [de.jollyday.datasource.ConfigurationDataSource] []
                          (getConfiguration [param]
                            configuration))
          man           (doto (HolidayManager/getInstance)
                          (.setConfigurationDataSource cds)
                          (.doInit))]
      man))

  clojure.lang.Keyword
  (create-manager [calendar]
    (let [calendar' (cond
                      (keyword? calendar)                  (get
                                                            predefined-calendars
                                                            (name calendar))
                      (instance? HolidayCalendar calendar) calendar)]
      (when-not (instance? HolidayCalendar calendar')
        (throw (IllegalArgumentException. (str  calendar " was not a valid calendar"))))
      (HolidayManager/getInstance calendar'))))

(defn manager
  "Given a country calendar (e.g. :italy) as an argument returns an HolidayManager
  that can be utilized to compute holidays for that country."

  [input]
  (create-manager input))

(defn holiday->map
  ""
  [h]
  (hash-map
   :date            (.getDate h)
   :description     (.getDescription h)))
   :properties-key  (keyword (str/lower-case (.getPropertiesKey h)))

(defn holidays
  ([hm year]
   (.getHolidays hm
                 year
                 (make-array String 0)))
  ([hm date-from date-to]
   (.getHolidays hm
                 date-from
                 date-to
                 (make-array String 0))))

(defn holiday
  ""
  [^HolidayManager hm year properties-key]
  (some #(and (= properties-key (:properties-key %)) %) (map holiday->map (holidays hm year))))

(defn holiday?
  "Takes an HolidayManager and a date object and returns true
  if date is an holiday, false otherwise."

  [^HolidayManager hm ^LocalDate d]
  (.isHoliday hm d (into-array [""])))





(comment
  ;; poc to create an holiday manager from a customized configuration stream
  (let [xml           (slurp (io/resource "test.xml"))
        is            (java.io.ByteArrayInputStream. (.getBytes xml))
        xml           (cz/xml-zip (cx/parse is))
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

    (holiday? m (java.time.LocalDate/of 2018 11 4))))

(comment
  ;; poc to initialize an holiday manager from a holiday configuration stream
  (let [xml           (slurp (io/resource "test.xml"))
        is            (java.io.ByteArrayInputStream. (.getBytes xml))
        _             (de.jollyday.HolidayManager/setManagerCachingEnabled false)
        util          (de.jollyday.util.XMLUtil.)
        configuration (.unmarshallConfiguration util is)
        holidays1     (.getHolidays configuration)
        cds           (proxy [de.jollyday.datasource.ConfigurationDataSource] []
                        (getConfiguration [param]
                          (println "invoked getConfiguration")
                          configuration))
        man           (doto (manager)
                        (.setConfigurationDataSource cds)
                        (.doInit))]
    (holidays man 2018)))
