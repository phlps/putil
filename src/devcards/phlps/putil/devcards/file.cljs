(ns phlps.putil.devcards.file
  (:require
    [phlps.putil.file :as f]
    [phlps.putil.util :as u]
    [phlps.putil.chan :as ch]
    [phlps.putil.csv :as csv]
    [reagent.core]
    [devcards.core]
    [cljs.core.async :as async
     :refer [>! <! chan buffer close!
             alts! timeout]])
  (:require-macros
    [devcards.core :as dc :refer [defcard-rg defcard-doc deftest]]
    [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn process-file [file]
  (->> file
       (f/file-segments)
       (csv/read-csv-chan)
       (async/take 4)
       (ch/mapch js->clj)
       (ch/mapch #(do (prn "log ....... " %) %))
       (ch/discard)))

(defn on-files [files]
  (doseq [ix (range (.-length files))]
    (process-file (aget files ix))))

(defcard-rg demo-ability-to-import-file
         "Use the button to select a csv file.."
            [f/select on-files true]
         )

(defcard-doc
  "The component is called `file/select`...
  It takes a parametes, a function to receive the file.

  To use the component, passing it that function, e.g.:
  ```
  (file/select on-files)
  ```
  The function you provide should accept one parameter, a seq of files.

  Here is the function `on-files` which I passed to the `file/select` component:
  "
  (dc/mkdn-pprint-source on-files)

  " and here is the function `process-file` which it calls:"
  (dc/mkdn-pprint-source process-file)

  "
  To help understand what this `process-file` function does, here is the source of the function `file/file-segments`
  "
  (dc/mkdn-pprint-source f/file-segments)
  "
  For completeness, here is the definition of the `file/select` component:

  "
  (dc/mkdn-pprint-source f/select)
  "")
