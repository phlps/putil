(ns phlps.putil.pouchdb
  (:refer-clojure :exclude [replicate get remove])
  (:require [clojure.spec :as spec]
            [clojure.spec.test :as stest]
            [phlps.putil.util :as u]
            [phlps.putil.chan :as ch]
            [clojure.core.async.impl.channels :as channels]
            #_[oops.core :refer [oget oset! ocall oapply ocall! oapply!
                                 oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]])) ;

(defn- ->js [m] (clj->js m))
(defn- -process [channel] (->> channel
                               (ch/mapch js->clj)
                               (ch/mapch ch/log)))
(defn db? [arg]
  (instance? js/PouchDB arg))
(defn channel? [ch]
  (instance? channels/ManyToManyChannel ch))
(defn jsname? [s]
  (some? (re-matches #"[a-zA-Z][a-zA-Z0-9\*\+!\-_\?]*" s)))

(defn open
  "Returns a new or existing database."
  ([dbname]
   (js/PouchDB. dbname))
  ([dbname opts]
   (js/PouchDB. dbname (->js opts))))

(defn close
  [db]
  (-> #(.close %1 %2)
      (ch/as-chan db)
      -process))

(defn destroy
  ([db]
   (-> #(.destroy %1 %2)
       (ch/as-chan db)
       -process))
  ([db opts]
   (-> #(.destroy %1 %2 %3)
       (ch/as-chan db (->js opts))
       -process)))

(defn info
  [db]
  (-> #(.info %1 %2)
      (ch/as-chan db)
      -process))

(defn get
  ([db doc-id]
   (-> #(.get %1 %2 %3)
       (ch/as-chan db doc-id)
       -process))
  ([db doc-id opts]
   (-> #(.get %1 %2 %3 %4)
       (ch/as-chan db doc-id (->js opts))
       -process)))

(defn put
  ([db doc]
   (-> #(.put %1 %2 %3)
       (ch/as-chan db (->js doc))
       -process))
  ([db doc opts]
   (-> #(.put %1 %2 %3 %4)
       (ch/as-chan db (->js doc) (->js opts))
       -process)))

(defn post
  ([db doc]
   (-> #(.post %1 %2 %3)
       (ch/as-chan db (->js doc))
       -process))
  ([db doc opts]
   (-> #(.post %1 %2 %3 %4)
       (ch/as-chan db (->js doc) (->js opts))
       -process)))

(defn delete
  ([db doc]
   (-> #(.put %1 %2 %3)
       (ch/as-chan db (->js (assoc doc "_deleted" true)))
       -process))
  ([db doc opts]
   (-> #(.put %1 %2 %3 %4)
       (ch/as-chan db (->js (assoc doc "_deleted" true)) (->js opts))
       -process)))

(defn remove
  ([db doc]
   (delete db doc))
  ([db doc opts]
   (delete db doc opts)))

(defn changes
  [db]
  (ch/as-chan #(.changes %1 %2) db))  ; return a channel

(defn replicate
  ([db to]
   (replicate db to {}))    ;  TODO channel with various types of events
  ([db to opts]
   12))  ;  TODO channel with various types of events

(defn sync
  ([db with]
   (sync db with {}))    ;  TODO channel with various types of events
  ([db with opts]
   12))   ;  TODO channel with various types of events

(defn create-index [db fields]
  (-> #(.createIndex %1 %2)
      (ch/as-chan db #js {"index" {"fields" fields}})
      (-process)))

(defn get-indexes [db]
  (-> #(.getIndexes %1 %2) (ch/as-chan db) (-process)))

(defn delete-index [db index]
  (-> #(.deleteIndex %1 (->js %2) %3) (ch/as-chan db index) (-process)))

