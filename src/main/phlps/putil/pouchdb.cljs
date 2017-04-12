(ns phlps.putil.pouchdb
  (:refer-clojure :exclude [replicate get remove])
  (:require [clojure.spec :as spec]
            [clojure.spec.test :as stest]
            [phlps.putil.util :as u]
            [phlps.putil.chan :as ch]
            [clojure.core.async.impl.channels :as channels]
            [cljs.core.async :as async :refer [>! <! chan put! close! timeout]]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                                 oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn- -process2 [channel] (->> channel
                                (ch/mapch js->clj)
                                #_(ch/mapch #(do (prn "log ....... " %) %))))
(defn- -process
  "returns a channel 'out'. Any result or error will be on the channel."
  [db op & args]
  (let [out (chan)
        callback (fn [err result]
                   #_(prn "+++++============== raw err " err " result " (js->clj result))
                   (go (if err
                         (>! out (if (instance? js/Error err) err (js/Error. err)))
                         (>! out result))
                       (close! out)))]
    (oapply+ db op (concat args [callback]))
    (-process2 out)))

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
   (js/PouchDB. dbname (clj->js opts))))

(defn close
  [db]
  (-process db "close"))

(defn destroy
  ([db]
   (-process db "destroy"))
  ([db opts]
   (-process db "destroy (cljs->js opts")))

(defn info
  [db]
  (-process db "info"))

(defn get
  ([db doc-id]
   (-process db "get" doc-id))
  ([db doc-id opts]
   (-process db "get" doc-id (clj->js opts))))

(defn put
  ([db doc]
   (-process db "put" (clj->js doc)))
  ([db doc opts]
   (-process db "put" (clj->js doc) (clj->js opts))))

(defn post
  ([db doc]
   (-process db "post" (clj->js doc))
    )
  ([db doc opts]
   (-process db "post" (clj->js doc) (clj->js opts))))

(defn delete
  ([db doc]
   (-process db "put" (clj->js (assoc doc "_deleted" true))))
  ([db doc opts]
   (-process db "put" (clj->js (assoc doc "_deleted" true)) (clj->js opts))))

(defn remove
  ([db doc]
   (delete db doc))
  ([db doc opts]
   (delete db doc opts)))

(defn changes
  [db opts]
  (let [out (chan)
        changes-feed (ocall db "changes" (clj->js opts))
        publish #({:event %1 :info (js->clj %2)})]
    (do
      (ocall changes-feed "on" "change" #(>! out (publish "change" %)))
      (ocall changes-feed "on" "complete"
             #(do (>! out (publish "complete" %)) (close! out)))
      (ocall changes-feed "on" "error"
             #(do (>! out (publish "error" %)) (close! out)))
      (-process2 out))))  ; return a channel

(defn replicate
  ([db direction target]
   (replicate db direction target {}))
  ([db direction target opts]
   (let [tgt (if (db? target) target (open target))
         replicator (ocall+ (oget db "replicate") direction
                            tgt (clj->js opts))
         out (chan)
         publish #({:event %1 :info (js->clj %2)})]
     (do
       (ocall replicator "on" "change" #(>! out (publish "change" %)))
       (ocall replicator "on" "paused" #(>! out (publish "paused" %)))
       (ocall replicator "on" "active" #(>! out {:event "active"}))
       (ocall replicator "on" "denied" #(>! out (publish "denied" %)))
       (ocall replicator "on" "complete"
              (do #(>! out (publish "complete" %)) (close! out)))
       (ocall replicator "on" "error"
              (do #(>! out (publish "error" %)) (close! out)))
       )
     (-process2 out))))  ;  return a channel

(defn create-index [db ixname fields]
  (-process db "createIndex"
            (clj->js {:index {:fields fields} :name ixname})))

(defn get-indexes [db]
  (-process db "getIndexes"))

(defn delete-index [db index]
  (-process db "deleteIndex" (clj->js index)))

