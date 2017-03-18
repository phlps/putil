(ns phlps.putil.constraint
  (:require [clojure.string :as s]
            [goog.string :as gs]
            [phlps.putil.util :as u]
            [phlps.putil.parse :as parse]))

(defn s-notblank? [ss] (not (s/blank? ss)))

(defn s-blank? [ss] (s/blank? ss))

(defn s->blank [ss]
  (if (s-blank? ss)
    nil
    ss))

(defn s-number? [ss] (let [n (gs/toNumber ss)] (= n n)))

(defn s->number [ss]
  (let [n (gs/toNumber ss)]
    (if (= n n)
      n
      nil)))

(defn s-int? [ss] (gs/isNumeric (s/trim ss)))

(defn s->int
  [ss]
  (if (s-int? ss)
    (gs/toNumber ss)
    nil))

(defn s-date? [ss pattern] (not (u/NaN? (parse/date ss pattern))))

(defn s->date
  [ss pattern]
  (let [dt (parse/date ss pattern)]
    (if (not (u/NaN? dt))
      dt
      nil)))

(defn s-maxlen? [len ss] (<= (count (s/trim ss)) len))

(defn s-minlen? [len ss] (>= (count (s/trim ss)) len))

(defn- tonum [ss]
  (if (s-int? ss)
    (s->int ss)
    (if (s-number? ss)
      (s->number ss)
      nil)))

(defn s-maxval? [max ss]
  (let [m (tonum max)
        s (tonum ss)
        both-numeric (and (not (nil? m)) (not (nil? s)))]
    (if both-numeric
      (<= s m)
      nil)))

(defn s-minval? [min ss]
  (let [m (tonum min)
        s (tonum ss)
        both-numeric (and (not (nil? m)) (not (nil? s)))]
    (if both-numeric
      (>= s m)
      nil)))

(def analysis
  {:blank [0 0]
   :numeric [0 0]
   :int [0 0]
   :date [0 0]})
