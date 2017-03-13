(ns phlps.putil.parse
  (:refer-clojure :exclude [uuid])
  (:require [phlps.putil.util :as u]
            [goog.string :as gstring]
            [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [goog.i18n.DateTimeParse])
  (:import [goog.date DateTime]))

(defn number
  [s]
  (let [n (gstring/toNumber s)]
    (if (u/NaN? n)
      nil
      n)))

(defn integer
  [s]
  (let [n (number s)]
    (if (nil? n)
      nil
      (if (= n (int n))
        n
        nil))))

(defn uuid [s]
  (uuid/make-uuid-from s))

(defonce date-time-parsers
         (atom {}))

(defn- parser [fmt]
  (let [cache-it (fn []
                   (let [parser (goog.i18n.DateTimeParse. fmt)]
                     (swap! date-time-parsers assoc fmt parser)
                     parser))]
    (or (get @date-time-parsers fmt (cache-it)))))

(defn- parse-date
  "Parse a date-string using a formatting string like \"dd MMMM yyyy\""
  [s pattern]
  (let [result (DateTime. 0)
        parser (parser pattern)
        chars-consumed (.strictParse parser s result)]
    (if (= chars-consumed (count s)) result js/NaN)))

(defn- -attempt-date [s]
  (let [date? u/date?]
    (cond
      (date? (parse-date s "yyyy-MM-dd")) "yyyy-MM-dd"
      (date? (parse-date s "M/d/yy")) "M/d/yy"
      (date? (parse-date s "d/M/yy")) "d/M/yy"
      (date? (parse-date s "MMM dd, yyyy")) "MMM dd, yyyy"
      (date? (parse-date s "MMMM dd, yyyy")) "MMMM dd, yyyy"
      :else nil)))

(defn date
  ([s]
   (let [pattern (-attempt-date s)]
     (if pattern (parse-date s pattern))))
  ([s pattern]
   (parse-date s pattern)))

(defn parsable? [s]
  (cond
    (= "" (string/trim s)) :blank
    (integer s) :integer
    (number s) :number
    :else (let [pattern (-attempt-date s)]
             (if pattern pattern :string))))

(defn row-stats [row stats]
  (for [item (range (count row))
        :let [elem (nth row item)
              stat (nth stats item)
              result (parsable? elem)]]
    (assoc stat result (inc (get stat result)))))

(defn stats
  ([rows]
   (stats rows nil))
  ([rows n]
   (let [names (for [col (first rows)] {:name col})
         rows (cond
                (nil? n) (rest rows)
                :else (subvec (vec (rest rows)) 1 n))]
     (vec (reduce #(row-stats %2 %1) names rows)))))
