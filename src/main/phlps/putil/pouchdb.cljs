(ns phlps.putil.pouchdb
  (:refer-clojure :exclude [replicate get remove])
  (:require [clojure.spec :as spec]
            [clojure.spec.test :as stest]
            [phlps.putil.util :as u]
            [clojure.core.async.impl.channels :as channels]
            [cljs.core.async :as async :refer [>! <! chan put! close! timeout]]
            [reagent.interop :refer-macros [$ $!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn- -callback [out]
  (fn [err result]
    #_(prn "+++++============== raw err " err " result " (js->clj result))
    (go (if err
          (>! out (if (instance? js/Error err) err (js/Error. err)))
          (>! out (js->clj result)))
        )))

(defn db? [arg]
  (instance? js/PouchDB arg))
(defn channel? [arg]
  (instance? channels/ManyToManyChannel arg))
(defn jsname? [arg]
  (some? (re-matches #"[a-zA-Z][a-zA-Z0-9\*\+!\-_\?]*" arg)))

(defn open
  "Returns a new or existing database."
  ([dbname]
   (js/PouchDB. dbname))
  ([dbname opts]
   (js/PouchDB. dbname (clj->js opts))))

(defn close
  [db out]
  ($ db close (-callback out)))

(defn destroy
  ([db out]
    ($ db destroy (-callback out)))
  ([db opts out]
    ($ db destroy (clj->js opts) (-callback out))
   ))

(defn info
  [db out]
   ($ db info (-callback out)))

(defn get
  ([db doc-id out]
    ($ db get doc-id (-callback out)))
  ([db doc-id opts out]
    ($ db get doc-id (clj->js opts) (-callback out))))

(defn put
  ([db doc out]
    ($ db put (clj->js doc) (-callback out)))
  ([db doc opts out]
    ($ db put (clj->js doc) (clj->js opts) (-callback out))))

(defn post
  ([db doc out]
    ($ db post (clj->js doc) (-callback out))
    )
  ([db doc opts out]
    ($ db post (clj->js doc) (clj->js opts) (-callback out))))

(defn delete
  ([db doc out]
   ($ db put (clj->js (assoc doc "_deleted" true)) (-callback out)))
  ([db doc opts out]
   ($ db put (clj->js (assoc doc "_deleted" true))
             (clj->js opts) (-callback out))))

(defn remove
  ([db doc out]
   (delete db doc out))
  ([db doc opts out]
   (delete db doc opts out)))

(defn changes
  [db opts out]
  (let [changes-feed ($ db changes (clj->js opts))
        publish #({:event %1 :info (js->clj %2)})]
    (do
      ($ changes-feed on "change" #(go (>! out (publish "change" %))))
      ($ changes-feed on "complete"
             #(go (>! out (publish "changes-complete" %))))
      ($ changes-feed on "error"
             #(go (>! out (publish "changes-error" %)))))
    changes-feed))  ; the "changes-feed" object has a cancel() method

(defn replicate
  ([source target out]
   (replicate source target {} out))
  ([source target opts out]
   (let [tgt (if (db? target) target (open target))
         replicator ($ js/PouchDB replicate source target (clj->js opts))
         publish #({:event %1 :info (js->clj %2)})]
     (do
       ($ replicator on "change"
                     #(go (>! out (publish "replication-change" %))))
       ($ replicator on "paused"
                     #(go (>! out (publish "replication-paused" %))))
       ($ replicator on "active"
                     #(go (>! out (publish "replication-active" ""))))
       ($ replicator on "denied"
                     #(go (>! out (publish "replication-denied" %))))
       ($ replicator on "complete"
                     #(go (>! out (publish "replication-complete" %)) ))
       ($ replicator on "error"
                     #(go (>! out (publish "replication-error" %)) ))
       )
     replicator)))  ; the "replicator" object has a cancel() method

(defn create-index [db index out]
  ($ db createIndex
            (clj->js index) (-callback out)))

(defn get-indexes [db out]
  ($ db getIndexes (-callback out)))

(defn delete-index [db index out]
  ($ db deleteIndex (clj->js index) (-callback out)))

