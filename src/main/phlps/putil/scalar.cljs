(ns phlps.putil.scalar
  (:require [goog.string :as gstring]
            [cljs-time.core :as t]
            [cljs-time.format :as tfmt]
            [cljs-uuid-utils.core :as uuid]
            [goog.i18n.DateTimeFormat]))


(defn showme []
  (tfmt/show-formatters))

(defn thrush [& args]
  "similar to ->  but better? :: see
   [http://blog.fogus.me/2010/09/28/thrush-in-clojure-redux/]"
  (reduce #(%2 %1) args))

(defn NaN?
  "isNaN(v) not as reliable as (v!=v) acccording to Mozilla."
  [v]
  (or (nil? v) (js/isNaN v)))

;(comment ?? would this be better ??
         #_(defn NaN? [node]
           (and (= (.call js/toString node) (str "[object Number]"))
                (js/eval (str node " != +" node))))  ;)

(defn rand-uuid []
  (uuid/make-random-uuid))

(defn rand-squuid []
  (uuid/make-random-squuid))


(declare datestr)

(defn date? [v]
  "a completely valid instance of Date or DateTime"
  (goog.isDateLike v))

(defn isodate
  [d]
  (if (date? d)
    (datestr d "yyyy-MM-dd")
    "Not a Date"))

(defonce date-time-formatters
         (atom {}))

(defn datestr
  "Format a date using a formatting string like \"dd MMMM yyyy\""
  ([date]
   (datestr date "yyyy-MM-dd"))
  ([date pattern]
   (let [cache-it (fn []
                   (let [frmattr (goog.i18n.DateTimeFormat. pattern)]
                     (swap! date-time-formatters assoc pattern frmattr)
                     frmattr))
         formatter (or (get @date-time-formatters pattern) (cache-it))]
    (.format formatter date))))

(def date-formats
  [
   "yyyy-MM-dd"
   "M/d/yy"
   "d/M/yy"
   "MMM dd, yyyy"
   "MMMM dd, yyyy"
   "EEEE, MMMM dd, yyyy"
   "h:mm a"
   "hh:mm:ss a"
   "h:mm:ss a z"
   "hh:mm:ss a z"
   "yyyy-MM-dd hh:mm:ss a z"
   "M/d/yy h:mm a"
   "d/M/yy h:mm a"
   "MMM dd, yyyy hh:mm:ss a"
   "MMMM dd, yyyy h:mm:ss a z"
   "EEEE, MMMM dd, yyyy hh:mm:ss a z"])

(defn gmt []
  (t/now))

(defn now []
  (t/to-default-time-zone (t/now)))

(defn gmt-offset
  "At our location, what gmt-offset does a new Date enshrine? In Australia
  or Japan it might be 1000, mid-Pacific it might be -1000, in Palestine 200,
  in Brazil perhaps -400."
  []
  (gstring/parseInt (subs (str (js/Date.)) 28 33)))
