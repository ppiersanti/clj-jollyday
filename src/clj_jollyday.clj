(ns clj-jollyday
  (:require [clojure.string :as string]
            [clj-jollyday.util :as util]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [de.jollyday HolidayManager HolidayCalendar]
           [java.time LocalDate]))


(def predefined-calendars
  (->> (util/get-static-fields-of-type HolidayCalendar HolidayCalendar)
       (util/map-kv
        (fn [^String n fmt]
          [(string/lower-case (.replace n \_ \-)) fmt]))))

(defn manager
  "Given a country calendar (e.g. :italy) as an argument returns an HolidayManager
  that can be utilized to compute holidays for that country."
  ([]
   (HolidayManager/getInstance))
  ([calendar]
   (let [calendar' (cond
                     (keyword? calendar)                  (get
                                                           predefined-calendars
                                                           (name calendar))
                     (instance? HolidayCalendar calendar) calendar)]
     (when-not (instance? HolidayCalendar calendar')
       (throw (IllegalArgumentException. (str  calendar " was not a valid calendar"))))

     (HolidayManager/getInstance calendar'))))

(defn holiday->map
  ""
  [h]
  (hash-map
   :date            (.getDate h)
   :properties-key  (keyword (str/lower-case (.getPropertiesKey h)))
   :description     (.getDescription h)))

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
