# clj-jollyday

A Clojure wrapper around the [Jollyday](https://github.com/svendiedrichsen/jollyday "Jollyday") library
plus some convenience functions.

## Rationale
The main goal is to work with edn rather than xml data. Manipulating, adding, removing
holydays in a Clojure flavor makes it easier the task at hand.

## Installation
Add the following dependency to your project.clj file:

```clojure
[clj-hollyday "0.1.0"]
```


## Usage

To start using this library first require it:

```clojure
(require '[clj-hollyday :as hol])
```

Next set an holyday manager to query for holydays in a particular country:

``` clojure
(def man (hol/manager :italy))

```

Now that an holiday manager has been created ask it if a date is an holiday:

``` clojure
(hol/holiday? man (java.time.LocalDate/of 2019 12 25))
=> true

(hol/holiday? man (java.time.LocalDate/of 2019 12 23))
=> false
```

To get an holidays collection in a given period of time:

``` clojure
;; all the holidays in the year 2019
(hol/holidays man 2019)
=> ({:description "St.Stephen's Day",
	:properties-key :stephens,
	:date #object[java.time.LocalDate 0x62eb7a44 "2019-12-26"],
	:official-holiday? true}
   {:description "Easter",
	:properties-key :christian.easter,
	:date #object[java.time.LocalDate 0x4b1fcc41 "2019-04-21"],
	:official-holiday? true}
  ...) ;; continue...

;; all the holidays in a date range
(import '[java.time LocalDate])
=> nil

(hol/holidays man (LocalDate/of 2019 12 20) (LocalDate/of 2019 12 28))
=> ({:description "St.Stephen's Day",
	 :properties-key :stephens,
	 :date #object[java.time.LocalDate 0x4fc3a2aa "2019-12-26"],
	 :official-holiday? true}
	{:description "Christmas",
	 :properties-key :christmas,
	 :date #object[java.time.LocalDate 0x5f939f17 "2019-12-25"],
	 :official-holiday? true})
```

Not all the holidays have fixed recurring. Some holiday's date might depend upon the year:
Query an holyday by year and key:

```clojure
(hol/holiday man 2020 :christian.easter)
=> {:description "Easter",
	:properties-key :christian.easter,
	:date #object[java.time.LocalDate 0x1b4feb59 "2020-04-12"],
	:official-holiday? true}
```

Parse a xml holyday definition, getting back edn data:

``` clojure
(def xml-str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<tns:Configuration xmlns:tns=\"http://www.example.org/Holiday\" hierarchy=\"it\" description=\"Italy\">
   <tns:Holidays>
	 <tns:Fixed descriptionPropertiesKey=\"ALL_SAINTS\" day=\"1\" month=\"NOVEMBER\"/>
	 <tns:Fixed descriptionPropertiesKey=\"IMMACULATE CONCEPTION\" day=\"8\" month=\"DECEMBER\"/>   </tns:Holidays>
</tns:Configuration>")

(hol/parse-xml xml-str)
=> {:hierarchy "it",
	:description "Italy",
	:holidays
	([:tns:Fixed
	 {:month "NOVEMBER",
	  :day "1",
	  :descriptionPropertiesKey "ALL_SAINTS"}]
	 [:tns:Fixed
	  {:month "DECEMBER",
	   :day "8",
	   :descriptionPropertiesKey "IMMACULATE CONCEPTION"}])}
```

Data can be turned back to xml structure, ready to be manipulated with Clojure xml tools:

```clojure
(hol/parse-edn *1)
=> {:tag :tns:Configuration,
	:attrs
	{:hierarchy "it",
	 :description "Italy",
	 :xmlns:tns "http://www.example.org/Holiday",
	 :xmlns:xsi "http://www.w3.org/2001/XMLSchema-instance",
	 :xsi:schemaLocation "http://www.example.org/Holiday /Holiday.xsd"},
	:content
	({:tag :tns:Holidays,
	  :attrs {},
	  :content
	  ({:tag :tns:Fixed,
		:attrs
		{:month "NOVEMBER",
		 :day "1",
		 :descriptionPropertiesKey "ALL_SAINTS"},
		:content ()}
	   {:tag :tns:Fixed,
		:attrs
		{:month "DECEMBER",
		 :day "8",
		 :descriptionPropertiesKey "IMMACULATE CONCEPTION"},
		:content ()})})}
```

Countries definition files can be parsed as well:

```clojure
(hol/parse (slurp (io/resource "holidays/Holidays_it.xml")))
=> {:hierarchy "it",
	:description "Italy",
	:holidays
	([:tns:Fixed
	  {:descriptionPropertiesKey "NEW_YEAR",
	   :day "1",
	   :month "JANUARY"}]
	 [:tns:Fixed
	  {:descriptionPropertiesKey "EPIPHANY",
	   :day "6",
	   :month "JANUARY"}]
	 [:tns:Fixed
	  {:descriptionPropertiesKey "LIBERATION",
	   :day "25",
	   :month "APRIL"}]
	 [:tns:Fixed
	  {:descriptionPropertiesKey "LABOUR_DAY", :day "1", :month "MAY"}]
	 [:tns:Fixed
	  {:descriptionPropertiesKey "REPUBLIC_DAY",
	   :day "2",
	   :month "JUNE"}]
	 [:tns:Fixed
	  {:descriptionPropertiesKey "ASSUMPTION_DAY",
	   :day "15",
	   :month "AUGUST"}]
	 [:tns:Fixed
	  {:descriptionPropertiesKey "ALL_SAINTS",
	   :day "1",
	   :month "NOVEMBER"}]
	 [:tns:Fixed
	  {:descriptionPropertiesKey "IMMACULATE_CONCEPTION",
	   :day "8",
	   :month "DECEMBER"}]
	 [:tns:Fixed
	  {:descriptionPropertiesKey "CHRISTMAS",
	   :day "25",
	   :month "DECEMBER"}]
	 [:tns:Fixed
	  {:descriptionPropertiesKey "STEPHENS",
	   :day "26",
	   :month "DECEMBER"}]
	 [:tns:ChristianHoliday {:type "EASTER"}]
	 [:tns:ChristianHoliday {:type "EASTER_MONDAY"}]),
	:subconfigurations
	[{:hierarchy "bz",
	  :description "Südtirol",
	  :holidays ([:tns:ChristianHoliday {:type "WHIT_MONDAY"}])}]}
```



## License

Copyright © 2019 Paolo Piersanti

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
