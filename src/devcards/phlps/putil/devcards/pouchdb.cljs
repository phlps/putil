
(ns phlps.putil.devcards.pouchdb
  (:require
    [devcards.core] ;:as dc :refer [defcard defcard-doc deftest]]
    [cljs.test :as t :refer [is testing async report] #_:include-macros #_true]
    [phlps.putil.util :as u]
    [phlps.putil.parse :as parse]
    [phlps.putil.pouchdb :as pouch]
    [phlps.putil.chan :as ch]
    #_[sablono.core :as sab :include-macros true]
    #_[clojure.spec :as spec]
    #_[clojure.spec.test :as stest]
    [cljs.core.async :refer [>! <! chan put! close! timeout]]
    )
  (:require-macros
    [phlps.putil.macros :refer [<?]]
    [devcards.core :as dc :refer [defcard defcard-doc deftest]]
    #_[cljs.test :as t :refer [is testing async]]
    [cljs.core.async.macros :refer [go go-loop alt!]]))

#_(set! devcards.core/test-timeout 999)
#_(comment
  (require '[phsutil.pouchdb :as pouch])
  (go (let [db (pouch/open "test-silly-db")
            r (pouch/destroy db)
            r (<! r)
            r (prn r)]
        123)))
(deftest test-pouch-simple
 (testing "testing pouchdb: open put post info get destry"
   (async done
     (go
     (let
         [db (pouch/open "test-silly-db")
          a1 {:Xid "a1" :long "111"}
          a2 {:wide 222}
          b1 (<! (pouch/post db a1))
          b2 (<! (pouch/post db a2))
          c1 (get b1 "id")
          c2 (get b2 "id")
          d1 (get b1 "rev")
          d2 (get b2 "rev")
          inf (<! (pouch/info db))
          g1 (<! (pouch/get db c1))
          g2 (<! (pouch/get db c2))
          ]
         (do
           (is (= c1 (get g1 "_id")) (str "'_id' of first document " c1))
           (is (= c2 (get g2 "_id")) (str "'_id' of second document " c2))
           (is (= d1 (get g1 "_rev")) (str "'_rev' of first document " d1))
           (is (= d2 (get g2 "_rev")) (str "'_rev' of second document " d2))
           (is (= 222 (get g2 "wide")) "'wide' of second document 222")
           (is (= (get inf "doc_count") 2) "doc count of database")
           (is (= (get inf "db_name") "test-silly-db") " name of database")
           )
         (pouch/destroy db)
       (done))))))

(comment
  (require '[phlps.putil.pouchdb :as pouch]
           '[cljs.core.async :refer [>! <! chan put! close! timeout]]
           )
  (require '[cljs.core.async.macros :as mac :refer [go go-loop alt!]])
  (def a2 {:wide 222})
  (def db (pouch/open "test-silly-db"))
  (cljs.core.async.macros/go (prn "b2" (cljs.core.async/<! (pouch/post db a2))))
  )


