(ns phlps.putil.chan
  (:require
    [goog.events :as events]
    [goog.events.EventType]
    [goog.net.Jsonp]
    [goog.Uri]
    [clojure.string :as str]
    [cljs.core.async :as async :refer [>! <! chan put! close! timeout]]
    [phlps.putil.util :refer [index-of now]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn atom? [x]
  (instance? Atom x))

(defn log [value]
  (let [_ (prn "============ log: " value)]
    value))

(defn xform
  "transform the values on the in channel using the transform xf"
  [in xf]
  (let [out (chan 1 xf)
        _ (async/pipe in out)]
    out)
  ) ; return a channel

(defn ->clj-log [channel] (-> channel
                              (xform (map js->clj))
                              (xform (map log))))

(defn eat
  ([in]
   (eat in 0))
  ([in limit]
   (go-loop [c 1]
            (let [item (<! in)]
              (if (and item (or (<= c limit) (= limit 0)))
                (recur (inc c))
                (prn "ate limit " c))))))

(defn limit
  ([in]
   (limit in 0))
  ([in limit]
   (let [out (chan)]
     (go-loop [c 1]
              (let [item (<! in)]
                (if (and item (or (= limit 0) (<= c limit)))
                  (do (>! out item)
                      (recur (inc c)))
                  (close! out))))
     out)))

(defn as-chan
  "Given an asynchronous function 'f' and some args
   returns a channel 'out'. Any result or error will be on the channel."
  [f & args]
  (let [out (chan)
        callback (fn [err result]
             #_(prn "+++++============== raw err " err " result " (js->clj result))
             (go (if err
                   (>! out (if (instance? js/Error err) err (js/Error err)))
                   (>! out result))
                 (close! out)))]
    (apply f (cljs.core/concat args [callback]))
    out))
