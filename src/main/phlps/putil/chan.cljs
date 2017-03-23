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
  (prn "============ log: " value)
  value)

(defn mapch [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (>! out (f x))
                (recur))
            (close! out))))
    out))

#_(defn xform
  "transform the values on the in channel using the transform xf"
  [xf in]
  (let [out (chan 1 xf)
        _ (async/pipe in out)]
    out)
  ) ; return a channel

(defn ->clj-log [channel] (->> channel
                               (mapch js->clj)
                               (mapch log)))

(defn accept [pred in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (when (pred x) (>! out x))
                (recur))
            (close! out))))
    out))

(defn reject [pred in]
  (let [out (chan)]
    (go (loop []
          (if-let [v (<! in)]
            (do (when-not (pred v) (>! out v))
                (recur))
            (close! out))))
    out))

(defn discard
  ([in]
   (discard 0 in))
  ([limit in]
   (go-loop [c 1]
            (let [item (<! in)]
              (if (and item (or (<= c limit) (= limit 0)))
                (recur (inc c))
                (prn "discard " (dec c)))))))

(defn as-chan
  "Given an asynchronous function 'f' and some args
   returns a channel 'out'. Any result or error will be on the channel."
  [f & args]
  (let [out (chan)
        callback (fn [err result]
             #_(prn "+++++============== raw err " err " result " (js->clj result))
             (go (if err
                   (>! out (if (instance? js/Error err) err (js/Error. err)))
                   (>! out result))
                 (close! out)))]
    (apply f (cljs.core/concat args [callback]))
    out))
