(ns clj-jollyday.util
  (:import [java.lang.reflect Field]))


;; from clojure.java-time dm3

(defn editable? [coll]
  (instance? clojure.lang.IEditableCollection coll))

(defn reduce-map [f coll]
  (if (editable? coll)
    (persistent! (reduce-kv (f assoc!) (transient (empty coll)) coll))
    (reduce-kv (f assoc) (empty coll) coll)))

(defn get-static-fields-of-type [^Class klass, ^Class of-type]
  (->> (seq (.getFields klass))
       (map (fn [^Field f]
              (when (.isAssignableFrom of-type (.getType f))
                [(.getName f) (.get f nil)])) )
       (keep identity)
       (into {})))

(defn map-kv
  "Maps a function over the key/value pairs of an associate collection. Expects
  a function that takes two arguments, the key and value, and returns the new
  key and value as a collection of two elements."
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (let [[k v] (f k v)] (xf m k v)))) coll))
