;; Copyright (c) Jonas Enlund. All rights reserved.
;; Alterations Copyright (c) Ian Phillips. All rights reserved.
;; The use and distribution terms
;; for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

;; The original version of this software was in clojure.
;; This version is in clojurescript.
;; The original version scans into a lazy seq of vectors
;;         from a csv representation in a java.io.Reader,
;;    and, given a seq of seqs, writes
;;                   a csv representation in a java.io.Writer.
;; This version scans csv input into a lazy seq of vectors
;;        from any sequence of chars (e.g. a string).
;;   Given a seq of seqs, create-csv returns a lazy sequence of strings,
;;         where each string represents a single csv-record.
;; Detailed documentation can be found in test.phs.phsutil.csv
;;    which acts as a test, with documentation.

;; :author "Jonas Enlund, modified by Ian Phillips"
(ns phlps.putil.csv
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [cljs.core.async :refer [>! <! chan put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))



;; Reading

(def ^{:private true} lf \newline)
(def ^{:private true} cr \return)
(def ^{:private true} eof -1)

(defn- read-quoted-cell
  [input sep quote]
  (loop [ch (first input)
         input (rest input)
         cell (gstring/StringBuffer.)]
    (condp == ch
      quote (let [next-ch (first input)
                  input (rest input)]
              (condp == next-ch
                quote (do (.append cell quote)
                          (recur (first input) (rest input) cell))
                sep [(str cell) :sep input]
                lf [(str cell) :eol input]
                cr (if (== (first input) lf)
                     [(str cell) :eol (rest input)]
                     (recur (first input) (rest input) cell)) ; eat cr
                eof [(str cell) :eof input]
                nil [(str cell) :eof input]
                (throw (js/Error. (str "CSV error 3 (unexpected character: " cell " immediately after quote)")))))
      eof [(str cell) :eof input]
        #_(throw (js/Error. (str "CSV error 1 (unexpected end of file) when quote still expected. " cell)))
      nil [(str cell) :eof input]
        #_(throw (js/Error. (str "CSV error 2 (unexpected end of file) when quote still expected. " cell)))
      (do (.append cell ch)
          (recur (first input) (rest input) cell)))))


(defn- read-cell
  [input sep quote]
  (let [first-ch (first input)
        input (rest input)]
    (if (== first-ch quote)
      (read-quoted-cell input sep quote)
      (loop [first-ch first-ch
             input input
             cell (gstring/StringBuffer.)]
        (condp == first-ch
          sep [(str cell) :sep input]
          lf [(str cell) :eol input]
          cr (let [next-ch (first input)]
               (if (== next-ch lf)
                 [(str cell) :eol (rest input)]
                 (recur next-ch input cell)))               ; eat the cr
          eof [(str cell) :eof input]
          nil [(str cell) :eof input]
          (do (.append cell first-ch)
              (recur (first input) (rest input) cell)))))))

(defn- read-record [input sep quote]
  (loop [record (vector)
         input input]
    (let [rslt (read-cell input sep quote)
          cell (rslt 0)
          sentinel (rslt 1)
          input (rslt 2)
          record (conj record cell)]
      (if (= sentinel :sep)
        (recur record input)
        [record sentinel input]))))

(defn read-remaining-csv [input sep quote]
  (lazy-seq
    (let [[record sentinel input] (read-record input sep quote)]
      (case sentinel
        :eol (cons record (read-remaining-csv input sep quote))
        :eof (when-not (= record [""])
               (cons record nil))))))

(defprotocol Read-CSV-From
  (read-csv-from [input sep quote]))

(extend-protocol Read-CSV-From
  string
  (read-csv-from [s sep quote]
    (read-remaining-csv (seq s) sep quote))

  IndexedSeq
  (read-csv-from [s sep quote]
    (read-remaining-csv s sep quote)))


(defn read-csv
  "Reads CSV-data from input (string or a sequence of characters)
  into a lazy sequence, a vector of vectors.

   Valid options are
     :separator (default \\,)
     :quote (default \\\")"
  [input & options]
  (let [{:keys [separator quote] :or {separator \, quote \"}} options]
    (read-csv-from input separator quote)))

(defn read-csv-chan [in]
  "Takes an in channel, returns an out channel.
  Each value on the in channel is a segment of a large
  string (perhaps very large).
  Each value on the out channel will be a vector.
  It represents one line of the original, parsed as csv."
  (let [out (chan)
        sep \,
        quote \"]
    (go-loop [leftover nil]
             (if-let [segment (<! in)]
               (let [input (concat leftover (seq segment))]
                 (recur (loop [input input]
                          (let [[record sentinel more]
                                (read-record input sep quote)]
                            (case sentinel
                              :eol (do (>! out record)
                                       (recur more))
                              :eof input)))))
               (do  (let [[record s m]
                          (read-record leftover sep quote)]
                      (if (not (or (nil? record)
                                   (empty? record)
                                   (= [""] record)))
                        (>! out record)))
                   (close! out))))
    out))


;; create csv

(defn- create-cell [val quote quote?]
  (let [string (str val)
        must-quote (quote? string)
        surround (if must-quote quote "")
        raw (str/escape string {quote (str quote quote)})]
    (str surround raw surround)))

(defn- create-record [vals sep quote quote?]
  (loop [vals vals
         record ""]
    (let [val (first vals)
          more (next vals)
          cell (if val (create-cell val quote quote?))
          record (if val (str record cell))]
      (if more
        (recur more (str record sep))
        record))))


(defn create-entire-csv [valueseqs sep quote quote?]
  (lazy-seq
    (let [vals (first valueseqs)
          more (next valueseqs)
          record (if vals (create-record vals sep quote quote?))]
      (if more
        (cons record (create-entire-csv more sep quote quote?))
        (cons record nil)))))

(defn create-csv
  "creates a lazy sequence of lines in CSV-format.
   Each line is a string.
   If you join them all up with \newline you'll have a .csv file.

   The input 'data' is a sequence of sequences of values.

   Valid options are
     :separator (Default \\,)
     :quote (Default \\\")
     :quote? (A predicate function which determines if a string should be quoted. Defaults to quoting only when necessary.)"
  [data & options]
  (let [opts (apply hash-map options)
        separator (or (:separator opts) \,)
        quote (or (:quote opts) \")
        quote? (or (:quote? opts)
                   #(some #{separator quote \return \newline} %))]
    (create-entire-csv data separator quote quote?)))
