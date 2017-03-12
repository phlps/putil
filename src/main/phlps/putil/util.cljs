(ns phlps.putil.util
  #_(:require   ))

(defn index-of [xs x]
  (loop [i 0 xs xs]
    (if (= x (first xs))
      i
      (if (next xs)
        (recur (inc i) (next xs))
        -1))))

(defn- -error? [x]
  (instance? js/Error x))

(defn throw-err [x]
  (if (-error? x)
    (throw x)
    x))

(defn now [] (js/Date.))
