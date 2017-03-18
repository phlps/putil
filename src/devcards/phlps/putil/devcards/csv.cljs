(ns phlps.putil.devcards.csv
  (:require [phlps.putil.csv :as csv]
            [devcards.core]
            [sablono.core :as sab :include-macros true]
            [cljs.test :as t :refer [report] :include-macros true])
  (:require-macros
    [devcards.core :refer [defcard defcard-doc deftest]]
    [cljs.test :as t :refer [is testing async]]))


(defn quote?
      "The default implementation of the function that decides whether or not
      to quote the output. You can supply your own replacement function as
      the argument *quote?* when you call the top level function further down."
      [string]
      (some #{\, \" \return \newline} string))

(defcard date-time-formatting-and-parsing
         (sab/html
           [:div
           [:h2 "The parsing and creation of csv"]]))


(defcard-doc
  "phsutils/csv

  A bottom-up intro with illustrative working tests...

  The original version was in clojure, by Jonas Enlund.
  This version is in clojurescript.

  The original version scans into a lazy seq of vectors from a csv
  representation in a java.io.Reader, and, given a seq of seqs, writes
  a csv representation in a java.io.Writer.

   In this version, *read* scans into a lazy seq of vectors from any
   sequence of chars (e.g. a string).

   In this version, *create*, given a seq of seqs, returns a lazy sequence of
   strings, where each string represents a single csv-record.")




(deftest read-cell
         "csv/read-cell is the basis of csv parsing.
         It scans a sequence of chars and returns a `triple`, showing:

       - firstly, the value of the cell,
       - secondly, the separator that caused it to stop scanning,
       - thirdly, the sequence of characters as yet unscanned.
       "
         (testing "tests:"
                  (is
                    (= (csv/read-cell "uvw" \, \")
                       ["uvw" :eof '()])
                    "INPUT a sequence of chars:-  uvw")
                  (is
                    (= (csv/read-cell "uvw,ab" \, \")
                       ["uvw" :sep '("a" "b")])
                    "INPUT a sequence of chars:-  uvw,ab")
                  (is
                    (= (csv/read-cell "uvw\nab" \, \")
                       ["uvw" :eol '("a" "b")])
                    "INPUT a sequence of chars:-  uvw\\nab")
                  (is
                    (= (csv/read-cell "uv\"w,ab" \, \")
                       ["uv\"w" :sep '("a" "b")]) "INPUT a sequence of chars:-  uv\"w,ab")))

(deftest quoted-cell
         "**Only when** the very first character of a cell is a quote, then
         that cell is processed by _csv/read-quoted-cell_, and scanning
         continues until the matching end-quote is found.

       If a double quote is found, it is not regarded as the end of the cell,
       it is regarded as a signal to embed a single quote in the cell.

       Because scanning continues until the matching end quote is found, it
       is possible to include newlines in the cell. That would not have been
       possible in a cell that was not quoted because such a newline would
       have been taken to be the end of record."
         (testing
           (is
             (= (csv/read-cell "\"uvw\",ab" \, \")
                ["uvw" :sep '("a" "b")])
             "parse a string i.e. a sequence of chars:-  \"uvw\",ab")
           (is
             (= (csv/read-cell "\"uv\nw\",ab" \, \")
                ["uv\nw" :sep '("a" "b")])
             "parse a string i.e. a sequence of chars:- \"uv\\nw\",ab")
           (is
             (= (csv/read-cell "\"uv\"\"w\",ab" \, \")
                ["uv\"w" :sep '("a" "b")])
             "parse a string i.e. a sequence of chars:- \"uv\"\"w\",ab")))


(def p8 (seq "abc,def\n12,34\n56,78"))

(deftest get-fromrecord---read-remaining-csv---parse
   (is (= (csv/read-record p8 \, \")
          [["abc" "def"] :eol '("1" "2" "," "3" "4" "\n" "5" "6" "," "7" "8")]) "")
   (is (= (csv/read-remaining-csv p8 \, \")
           [["abc" "def"] ["12" "34"] ["56" "78"]]))
   (is (= (csv/read-csv p8)
          [["abc" "def"] ["12" "34"] ["56" "78"]]))
   (is (= (csv/read-csv p8)
            [["abc" "def"] ["12" "34"] ["56" "78"]])))

(deftest create-cell
         "csv/create-cell takes an object and creates a cell (a string,
           quoted if necessary).  In these examples, observe that
           the first two results are unquoted, and
            the remaining four results are quoted."
         (is
           (= (vec (csv/create-cell 54 \" quote?))
              [\5 \4]))
         (is
           (= (vec (csv/create-cell "uvw" \" quote?))
              [\u \v \w]))
         (is
           (= (vec (csv/create-cell "uv,w" \" quote?))
              [\" \u \v \, \w \"]))
         (is
           (= (vec (csv/create-cell "uv\nw" \" quote?))
              [\" \u \v \newline \w \"]))
         (is
           (= (vec (csv/create-cell "uv\rw" \" quote?))
              [\" \u \v \return \w \"]))
         (is
           (= (vec (csv/create-cell "uv\"w" \" quote?))
              [\" \u \v \" \" \w \"])))


(deftest create-record
         "csv/create-record creates a record
          (a string of comma-separated cells, quoted as necessary)."
         (is
           (= (csv/create-record [54 67] \, \" quote?)
              "54,67"))
         (is
           (= (csv/create-record [89 "uvw"] \, \" quote?)
              "89,uvw"))
         (is
           (= (csv/create-record ["uvw" "uv,w"] \, \" quote?)
              "uvw,\"uv,w\""))
         (is
           (= (csv/create-record ["uvw" "uv\nw"] \, \" quote?)
              "uvw,\"uv\nw\""))
         (is
           (= (csv/create-record ["uvw" "uv\rw"] \, \" quote?)
              "uvw,\"uv\rw\""))
         (is
           (= (csv/create-record ["uvw" "uv\"w"] \, \" quote?)
              "uvw,\"uv\"\"w\"")))



(deftest create-entire-csv
         "csv/create-entire-csv Returns a lazy sequence of records
         where each record is a string made up of comma-separated cells."
         (is
           (= (csv/create-entire-csv [[12 34] [54 67] [78 89]] \, \" quote?)
              ["12,34" "54,67" "78,89"]))
         (is
           (= (csv/create-entire-csv [["ab" 34] [54 67] [78 "uv,w"]]
                                     \, \" quote?)
              ["ab,34" "54,67" "78,\"uv,w\""]))
         (is
           (= (csv/create-entire-csv [["ab" 34] [54 67] [78 "uv\"w"]]
                                     \, \" quote?)
              ["ab,34" "54,67" "78,\"uv\"\"w\""])))



(deftest create-csv
         "csv/create-csv Returns a lazy sequence of records where each record is
          a string made up of comma-separated cells. The input data is a sequence
           of sequences of values.
           Optional additional arguments are :separator :quote :quote?"
         (is
           (= (csv/create-csv [[12 34] [54 67] [78 89]])
              ["12,34" "54,67" "78,89"]))
         (is
           (= (csv/create-csv [[12 34] [54 67] [78 89]] :separator \|)
              ["12|34" "54|67" "78|89"]))
         (is
           (= (csv/create-csv [["ab" 34] [54 67] [78 "uv,w"]])
              ["ab,34" "54,67" "78,\"uv,w\""]))
         (is
           (= (csv/create-csv [["ab" 34] [54 67] [78 "uv,w"]] :quote \')
              ["ab,34" "54,67" "78,'uv,w'"]))
         (is
           (= (csv/create-csv [["ab" 34] [54 67] [78 "uv\"w"]])
              ["ab,34" "54,67" "78,\"uv\"\"w\""]))
         (is
           (= (csv/create-csv [["ab" 34] [54 67] [78 "uv\"w"]] :quote \')
              ["ab,34" "54,67" "78,uv\"w"]))
         (is
           (= (csv/create-csv [["ab" 34] [54 67] [78 "uv'w"]] :quote \')
              ["ab,34" "54,67" "78,'uv''w'"])))
