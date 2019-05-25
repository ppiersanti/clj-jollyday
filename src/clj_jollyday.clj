(ns clj-jollyday
  (:require [clj-jollyday.util :as util]
            [clojure
             [xml :as cx]
             [zip :as cz]]
            [clojure.data.xml :as cdx]
            [clojure.java.io :as io]
            [clojure.data.zip.xml :as cdz]
            [clojure.string :as str])
  (:import [de.jollyday HolidayCalendar HolidayManager]
           [java.time LocalDate]))

(def predefined-calendars
  (->> (util/get-static-fields-of-type HolidayCalendar HolidayCalendar)
       (util/map-kv
        (fn [^String n fmt]
          [(str/lower-case (.replace n \_ \-)) fmt]))))

(defn parse-edn
  "Parse an edn map given as input and returns a xml
  structure.

  The edn is expected to contains a configuration key and
  an holidays key mapped to an array of holidays.

  ;> (parse-edn
      {:configuration {:hierarchy \"IT\"}
       :holidays      [[:FixedWeekday {:weekday                  \"SATURDAY\"
                                       :which                    \"FIRST\"
                                       :month                    \"NOVEMBER\"
                                       :descriptionPropertiesKey \"MY_CUSTOM_1\"}]
                       [:FixedWeekday {:weekday                  \"SUNDAY\"
                                       :which                    \"FIRST\"
                                       :month                    \"NOVEMBER\"
                                       :descriptionPropertiesKey \"MY_CUSTOM_2\"}]]})
  => {:tag :configuration,
  :attrs
  {:hierarchy \"IT\", :xmlns:tns \"http://www.example.org/Holiday\"},
   :xmlns:xsi \"http://www.w3.org/2001/XMLSchema-instance\",
   :xsi:schemaLocation \"http://www.example.org/Holiday /Holiday.xsd\"
  :content
  ({:tag   :tns:Holidays,
    :attrs {},
    :content
    ({:tag     :FixedWeekday,
      :attrs
      {:weekday                  \"SATURDAY\",
       :which                    \"FIRST\",
       :month                    \"NOVEMBER\",
       :descriptionPropertiesKey \"MY_CUSTOM_1\"},
      :content ()}
     {:tag     :FixedWeekday,
      :attrs
      {:weekday                  \"SUNDAY\",
       :which                    \"FIRST\",
       :month                    \"NOVEMBER\",
       :descriptionPropertiesKey \"MY_CUSTOM_2\"},
      :content ()})})}"
  [m]
  (let [prefs         {:xmlns:tns          "http://www.example.org/Holiday"
                       :xmlns:xsi          "http://www.w3.org/2001/XMLSchema-instance"
                       :xsi:schemaLocation "http://www.example.org/Holiday /Holiday.xsd"}
        ensure-prefix (fn [h] (update h 0 (fn [t] (let [n (str t)]
                                                    (if (str/starts-with? n ":tns:")
                                                      t
                                                      (keyword (str "tns" n)))))))]
    (cdx/sexp-as-element
     [:tns:Configuration (merge (select-keys m [:hierarchy :description]) prefs)
      (reduce #(conj %1 (ensure-prefix %2)) [:tns:Holidays] (:holidays m))
      (when (seq (:subconfigurations m))
        (map
         #(vector
           :tns:Subconfigurations
           (select-keys %1 [:hierarchy :description])
           (reduce (fn [xs x] (conj xs (ensure-prefix x)))
                   [:tns:Holidays] (:holidays %1))) (:subconfigurations m)))])))

(defn parse-xml
  "Parse an xml string given as input and returns an edn
  structure that can be manipulated as appropriate and
  eventually turned back to xml format again."
  [xml]
  (let [parse*     #(vector (:tag %) (:attrs %))
        extract-fn (fn [n]
                     (zipmap [:hierarchy :description :holidays]
                             ((juxt #(cdz/attr % :hierarchy)
                                    #(cdz/attr % :description)
                                    #(map
                                      parse*
                                      (->
                                       (cdz/xml1-> % :tns:Holidays)
                                       first
                                       :content))) n)))
        z          (-> xml
                       (.getBytes)
                       (java.io.ByteArrayInputStream.)
                       (cx/parse)
                       (cz/xml-zip))
        c          (extract-fn
                    (cdz/xml1-> z :tns:Configuration))
        subc       (map
                    extract-fn
                    (cdz/xml-> z :tns:Configuration :tns:SubConfigurations))]

    (if (seq subc)
      (assoc c :subconfigurations (reduce conj [] subc)) c)))

(defprotocol CreateManager
  (create-manager [input]))

(defn manager
  "Given a country calendar (e.g. :italy) as an argument returns an HolidayManager
  that can be utilized to compute holidays for that country."
  ([]
   (HolidayManager/getInstance))
  ([input]
   (create-manager input)))

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
      (HolidayManager/getInstance calendar')))

  clojure.data.xml.node.Element
  (create-manager [n]
    (create-manager (cdx/emit-str n)))

  clojure.lang.IPersistentMap
  (create-manager [m]
    (let [xml (slurp
               (io/resource
                (str "holidays/Holidays_" (name (-> m :configuration :hierarchy)) ".xml")))
          cal (parse-xml xml)
          cal (update-in cal [:configuration :holidays] #(reduce (fn [hs h] (conj hs h)) % (-> m :holidays)))]

      (create-manager (parse-edn cal)))))

(defn- holiday->map
  ""
  [h]
  (hash-map
   :date            (.getDate h)
   :properties-key  (keyword (str/lower-case (.getPropertiesKey h)))
   :description     (.getDescription h)
   :official-holiday? (boolean (.getType h) )))

(defn holidays
  ([hm year]
   (map holiday->map (.getHolidays hm
                                   year
                                   (make-array String 0))))
  ([hm date-from date-to]
   (map holiday->map (.getHolidays hm
                                   date-from
                                   date-to
                                   (make-array String 0)))))

(defn holiday
  ""
  [^HolidayManager hm year properties-key-or-descr]
  (condp instance? properties-key-or-descr
    clojure.lang.Keyword (some #(and (= properties-key-or-descr (:properties-key %)) %) (holidays hm year))
    String               (some #(and (= properties-key-or-descr (:description %)) %) (holidays hm year))))

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
;; *** TODO ***
;; - make it works custom holiday's description
;; - function parameter's hints
;; - test calendars other than :it
;; - convert holidays' output to edn
;; - test subconfiguration
;; - extend holiday by description string
;; - create-manager PersistentArraymap -> generic map interface
